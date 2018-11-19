/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.issue.webhook;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.elasticsearch.action.search.SearchResponse;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.AnalysisPropertyDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.server.es.Facets;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.issue.IssueQuery;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.qualitygate.ShortLivingBranchQualityGate;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.settings.ProjectConfigurationLoader;
import org.sonar.server.webhook.Analysis;
import org.sonar.server.webhook.Branch;
import org.sonar.server.webhook.Project;
import org.sonar.server.webhook.ProjectAnalysis;
import org.sonar.server.webhook.QualityGate;
import org.sonar.server.webhook.WebHooks;
import org.sonar.server.webhook.WebhookPayload;
import org.sonar.server.webhook.WebhookPayloadFactory;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.Collections.singletonList;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class IssueChangeWebhookImpl implements IssueChangeWebhook {
  private static final Set<String> MEANINGFUL_TRANSITIONS = ImmutableSet.of(
    DefaultTransitions.RESOLVE, DefaultTransitions.FALSE_POSITIVE, DefaultTransitions.WONT_FIX, DefaultTransitions.REOPEN);
  private final DbClient dbClient;
  private final WebHooks webhooks;
  private final ProjectConfigurationLoader projectConfigurationLoader;
  private final WebhookPayloadFactory webhookPayloadFactory;
  private final IssueIndex issueIndex;
  private final System2 system2;

  public IssueChangeWebhookImpl(DbClient dbClient, WebHooks webhooks, ProjectConfigurationLoader projectConfigurationLoader,
    WebhookPayloadFactory webhookPayloadFactory, IssueIndex issueIndex, System2 system2) {
    this.dbClient = dbClient;
    this.webhooks = webhooks;
    this.projectConfigurationLoader = projectConfigurationLoader;
    this.webhookPayloadFactory = webhookPayloadFactory;
    this.issueIndex = issueIndex;
    this.system2 = system2;
  }

  @Override
  public void onChange(IssueChangeData issueChangeData, IssueChange issueChange, IssueChangeContext context) {
    if (isEmpty(issueChangeData) || !isUserChangeContext(context) || !isRelevant(issueChange)) {
      return;
    }

    callWebHook(issueChangeData);
  }

  private static boolean isRelevant(IssueChange issueChange) {
    return issueChange.getTransitionKey().map(IssueChangeWebhookImpl::isMeaningfulTransition).orElse(true);
  }

  private static boolean isEmpty(IssueChangeData issueChangeData) {
    return issueChangeData.getIssues().isEmpty();
  }

  private static boolean isUserChangeContext(IssueChangeContext context) {
    return context.login() != null;
  }

  private static boolean isMeaningfulTransition(String transitionKey) {
    return MEANINGFUL_TRANSITIONS.contains(transitionKey);
  }

  private void callWebHook(IssueChangeData issueChangeData) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Map<String, ComponentDto> branchesByUuid = getBranchComponents(dbSession, issueChangeData);
      if (branchesByUuid.isEmpty()) {
        return;
      }

      Set<String> branchProjectUuids = branchesByUuid.values().stream()
        .map(ComponentDto::uuid)
        .collect(toSet(branchesByUuid.size()));
      Set<BranchDto> shortBranches = dbClient.branchDao().selectByUuids(dbSession, branchProjectUuids)
        .stream()
        .filter(branchDto -> branchDto.getBranchType() == BranchType.SHORT)
        .collect(toSet(branchesByUuid.size()));
      if (shortBranches.isEmpty()) {
        return;
      }

      Map<String, Configuration> configurationByUuid = projectConfigurationLoader.loadProjectConfigurations(dbSession,
        shortBranches.stream().map(shortBranch -> branchesByUuid.get(shortBranch.getUuid())).collect(Collectors.toSet()));
      Set<BranchDto> branchesWithWebhooks = shortBranches.stream()
        .filter(shortBranch -> webhooks.isEnabled(configurationByUuid.get(shortBranch.getUuid())))
        .collect(toSet());
      if (branchesWithWebhooks.isEmpty()) {
        return;
      }

      Map<String, SnapshotDto> analysisByProjectUuid = dbClient.snapshotDao().selectLastAnalysesByRootComponentUuids(
        dbSession,
        branchesWithWebhooks.stream().map(BranchDto::getUuid).collect(toSet(shortBranches.size())))
        .stream()
        .collect(uniqueIndex(SnapshotDto::getComponentUuid));
      branchesWithWebhooks
        .forEach(shortBranch -> {
          ComponentDto branch = branchesByUuid.get(shortBranch.getUuid());
          SnapshotDto analysis = analysisByProjectUuid.get(shortBranch.getUuid());
          if (branch != null && analysis != null) {
            webhooks.sendProjectAnalysisUpdate(
              configurationByUuid.get(shortBranch.getUuid()),
              new WebHooks.Analysis(shortBranch.getUuid(), analysis.getUuid(), null),
              () -> buildWebHookPayload(dbSession, branch, shortBranch, analysis));
          }
        });
    }
  }

  private WebhookPayload buildWebHookPayload(DbSession dbSession, ComponentDto branch, BranchDto shortBranch, SnapshotDto analysis) {
    Map<String, String> analysisProperties = dbClient.analysisPropertiesDao().selectBySnapshotUuid(dbSession, analysis.getUuid())
      .stream()
      .collect(Collectors.toMap(AnalysisPropertyDto::getKey, AnalysisPropertyDto::getValue));
    ProjectAnalysis projectAnalysis = new ProjectAnalysis(
      new Project(branch.getMainBranchProjectUuid(), branch.getKey(), branch.name()),
      null,
      new Analysis(analysis.getUuid(), analysis.getCreatedAt()),
      new Branch(false, shortBranch.getKey(), Branch.Type.SHORT),
      createQualityGate(branch, issueIndex),
      null,
      analysisProperties);
    return webhookPayloadFactory.create(projectAnalysis);
  }

  private QualityGate createQualityGate(ComponentDto branch, IssueIndex issueIndex) {
    SearchResponse searchResponse = issueIndex.search(IssueQuery.builder()
      .projectUuids(singletonList(branch.getMainBranchProjectUuid()))
      .branchUuid(branch.uuid())
      .mainBranch(false)
      .resolved(false)
      .checkAuthorization(false)
      .build(),
      new SearchOptions().addFacets(RuleIndex.FACET_TYPES));
    LinkedHashMap<String, Long> typeFacet = new Facets(searchResponse, system2.getDefaultTimeZone())
      .get(RuleIndex.FACET_TYPES);

    Set<QualityGate.Condition> conditions = ShortLivingBranchQualityGate.CONDITIONS.stream()
      .map(c -> toCondition(typeFacet, c))
      .collect(toSet(ShortLivingBranchQualityGate.CONDITIONS.size()));

    return new QualityGate(valueOf(ShortLivingBranchQualityGate.ID), ShortLivingBranchQualityGate.NAME, qgStatusFrom(conditions), conditions);
  }

  private static QualityGate.Condition toCondition(LinkedHashMap<String, Long> typeFacet, ShortLivingBranchQualityGate.Condition c) {
    long measure = getMeasure(typeFacet, c);
    QualityGate.EvaluationStatus status = measure > 0 ? QualityGate.EvaluationStatus.ERROR : QualityGate.EvaluationStatus.OK;
    return new QualityGate.Condition(status, c.getMetricKey(),
      toOperator(c),
      c.getErrorThreshold(), c.getWarnThreshold(), c.isOnLeak(),
      valueOf(measure));
  }

  private static QualityGate.Operator toOperator(ShortLivingBranchQualityGate.Condition c) {
    String operator = c.getOperator();
    switch (operator) {
      case QualityGateConditionDto.OPERATOR_GREATER_THAN:
        return QualityGate.Operator.GREATER_THAN;
      case QualityGateConditionDto.OPERATOR_LESS_THAN:
        return QualityGate.Operator.LESS_THAN;
      case QualityGateConditionDto.OPERATOR_EQUALS:
        return QualityGate.Operator.EQUALS;
      case QualityGateConditionDto.OPERATOR_NOT_EQUALS:
        return QualityGate.Operator.NOT_EQUALS;
      default:
        throw new IllegalArgumentException(format("Unsupported Condition operator '%s'", operator));
    }
  }

  private static QualityGate.Status qgStatusFrom(Set<QualityGate.Condition> conditions) {
    if (conditions.stream().anyMatch(c -> c.getStatus() == QualityGate.EvaluationStatus.ERROR)) {
      return QualityGate.Status.ERROR;
    }
    return QualityGate.Status.OK;
  }

  private static long getMeasure(LinkedHashMap<String, Long> typeFacet, ShortLivingBranchQualityGate.Condition c) {
    String metricKey = c.getMetricKey();
    switch (metricKey) {
      case CoreMetrics.BUGS_KEY:
        return getValueForRuleType(typeFacet, RuleType.BUG);
      case CoreMetrics.VULNERABILITIES_KEY:
        return getValueForRuleType(typeFacet, RuleType.VULNERABILITY);
      case CoreMetrics.CODE_SMELLS_KEY:
        return getValueForRuleType(typeFacet, RuleType.CODE_SMELL);
      default:
        throw new IllegalArgumentException(format("Unsupported metric key '%s' in hardcoded quality gate", metricKey));
    }
  }

  private static long getValueForRuleType(Map<String, Long> facet, RuleType ruleType) {
    Long res = facet.get(ruleType.name());
    if (res == null) {
      return 0L;
    }
    return res;
  }

  private Map<String, ComponentDto> getBranchComponents(DbSession dbSession, IssueChangeData issueChangeData) {
    Set<String> projectUuids = issueChangeData.getIssues().stream()
      .map(DefaultIssue::projectUuid)
      .collect(toSet());
    Set<String> missingProjectUuids = ImmutableSet.copyOf(Sets.difference(
      projectUuids,
      issueChangeData.getComponents()
        .stream()
        .map(ComponentDto::uuid)
        .collect(Collectors.toSet())));
    if (missingProjectUuids.isEmpty()) {
      return issueChangeData.getComponents()
        .stream()
        .filter(c -> projectUuids.contains(c.uuid()))
        .filter(componentDto -> componentDto.getMainBranchProjectUuid() != null)
        .collect(uniqueIndex(ComponentDto::uuid));
    }
    return Stream.concat(
      issueChangeData.getComponents().stream().filter(c -> projectUuids.contains(c.uuid())),
      dbClient.componentDao().selectByUuids(dbSession, missingProjectUuids).stream())
      .filter(componentDto -> componentDto.getMainBranchProjectUuid() != null)
      .collect(uniqueIndex(ComponentDto::uuid));
  }
}
