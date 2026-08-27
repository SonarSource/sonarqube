/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.ce.task.projectanalysis.step;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.IssueStatus;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.ProtoIssueCache;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.scanner.protobuf.utils.CloseableIterator;
import org.sonar.telemetry.core.event.AnalyticsEventPublisher;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent.RuleBacklog;
import org.sonar.telemetry.core.event.workflow.IssueTelemetryStatus;
import org.sonar.telemetry.core.event.workflow.IssueUpdatedBatchEvent;
import org.sonar.telemetry.core.event.workflow.IssueUpdatedBatchEvent.IssueUpdate;

import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ISSUE_EVENTS_MAX_PER_ANALYSIS;

public class SendIssueTelemetryStep implements ComputationStep {

  private static final Logger LOG = LoggerFactory.getLogger(SendIssueTelemetryStep.class);

  private final Configuration config;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TreeRootHolder treeRootHolder;
  private final ProtoIssueCache protoIssueCache;
  private final AnalyticsEventPublisher analyticsEventPublisher;

  public SendIssueTelemetryStep(Configuration config, AnalysisMetadataHolder analysisMetadataHolder,
    TreeRootHolder treeRootHolder, ProtoIssueCache protoIssueCache, AnalyticsEventPublisher analyticsEventPublisher) {
    this.config = config;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.treeRootHolder = treeRootHolder;
    this.protoIssueCache = protoIssueCache;
    this.analyticsEventPublisher = analyticsEventPublisher;
  }

  @Override
  public void execute(Context context) {
    if (!config.getBoolean(SONAR_TELEMETRY_ENABLE.getKey()).orElse(false) || analysisMetadataHolder.isPullRequest()) {
      return;
    }

    try {
      if (analysisMetadataHolder.isFirstAnalysis()) {
        publishBacklogAggregate();
      } else {
        publishIssueUpdates();
      }
    } catch (RuntimeException e) {
      // Telemetry must never fail an analysis.
      LOG.warn("Failed to send issue telemetry", e);
    }
  }

  private void publishBacklogAggregate() {
    RuleCounts ruleCounts = countIssuesByRule();
    if (ruleCounts.totalByRule.isEmpty()) {
      return;
    }

    List<RuleBacklog> rules = ruleCounts.totalByRule.entrySet().stream()
      .map(entry -> toRuleBacklog(entry.getKey(), entry.getValue(), ruleCounts.statusCountsByRule.getOrDefault(entry.getKey(), Map.of())))
      .toList();

    IssueBacklogTelemetryEvent event = new IssueBacklogTelemetryEvent(
      analysisMetadataHolder.getProject().getUuid(),
      treeRootHolder.getRoot().getUuid(),
      analysisMetadataHolder.getBranch().getType().name(),
      analysisMetadataHolder.getAnalysisDate(),
      rules);

    analyticsEventPublisher.publish(IssueBacklogTelemetryEvent.TYPE, event);
  }

  private void publishIssueUpdates() {
    int maxPerAnalysis = config.getInt(SONAR_TELEMETRY_ISSUE_EVENTS_MAX_PER_ANALYSIS.getKey())
      .orElse(Integer.parseInt(SONAR_TELEMETRY_ISSUE_EVENTS_MAX_PER_ANALYSIS.getDefaultValue()));

    List<IssueUpdate> buffered = new ArrayList<>();
    try (CloseableIterator<DefaultIssue> issues = protoIssueCache.traverse()) {
      while (issues.hasNext()) {
        DefaultIssue issue = issues.next();
        // Hotspot has null status, and we don't care about hotspot anymore
        String issueStatus = IssueTelemetryStatus.of(issue.status(), issue.resolution());
        if (isChangedIssue(issue) && issueStatus != null) {
          buffered.add(toIssueUpdate(issue));
          if (buffered.size() >= maxPerAnalysis) {
            break;
          }
        }
      }
    }

    if (buffered.isEmpty()) {
      return;
    }

    publishIssueUpdatesInChunks(buffered);
  }

  private void publishIssueUpdatesInChunks(List<IssueUpdate> issueUpdates) {
    String projectUuid = analysisMetadataHolder.getProject().getUuid();
    String branchId = treeRootHolder.getRoot().getUuid();
    String branchType = analysisMetadataHolder.getBranch().getType().name();

    for (int fromIndex = 0; fromIndex < issueUpdates.size(); fromIndex += IssueUpdatedBatchEvent.MAX_ISSUES_PER_EVENT) {
      int toIndex = Math.min(fromIndex + IssueUpdatedBatchEvent.MAX_ISSUES_PER_EVENT, issueUpdates.size());
      publishChunk(projectUuid, branchId, branchType, issueUpdates.subList(fromIndex, toIndex));
    }
  }

  private void publishChunk(String projectUuid, String branchId, String branchType, List<IssueUpdate> chunk) {
    IssueUpdatedBatchEvent event = new IssueUpdatedBatchEvent(projectUuid, branchId, branchType, List.copyOf(chunk));
    analyticsEventPublisher.publish(IssueUpdatedBatchEvent.TYPE, event);
  }

  private static boolean isChangedIssue(DefaultIssue issue) {
    return issue.isNew() || issue.isChanged() || issue.isCopied();
  }

  private static IssueUpdate toIssueUpdate(DefaultIssue issue) {
    Long issueResolvedAt = issue.closeDate() != null ? issue.closeDate().getTime() : null;
    return new IssueUpdate(
      issue.key(),
      issue.ruleKey().toString(),
      issue.creationDate().getTime(),
      IssueTelemetryStatus.of(issue.status(), issue.resolution()),
      issueResolvedAt);
  }

  private RuleCounts countIssuesByRule() {
    Map<String, Integer> totalByRule = new HashMap<>();
    Map<String, Map<IssueStatus, Integer>> statusCountsByRule = new HashMap<>();
    try (CloseableIterator<DefaultIssue> issues = protoIssueCache.traverse()) {
      while (issues.hasNext()) {
        DefaultIssue issue = issues.next();
        String ruleKey = issue.ruleKey().toString();
        totalByRule.merge(ruleKey, 1, Integer::sum);

        IssueStatus issueStatus = issue.issueStatus();
        if (issueStatus != null) {
          statusCountsByRule.computeIfAbsent(ruleKey, k -> new EnumMap<>(IssueStatus.class))
            .merge(issueStatus, 1, Integer::sum);
        }
      }
    }
    return new RuleCounts(totalByRule, statusCountsByRule);
  }

  private static RuleBacklog toRuleBacklog(String ruleKey, int total, Map<IssueStatus, Integer> statusCounts) {
    int open = statusCounts.getOrDefault(IssueStatus.OPEN, 0);
    int confirmed = statusCounts.getOrDefault(IssueStatus.CONFIRMED, 0);
    int falsePositive = statusCounts.getOrDefault(IssueStatus.FALSE_POSITIVE, 0);
    int accepted = statusCounts.getOrDefault(IssueStatus.ACCEPTED, 0);
    int inSandbox = statusCounts.getOrDefault(IssueStatus.IN_SANDBOX, 0);
    return new RuleBacklog(ruleKey, total, open, confirmed, falsePositive, accepted, inSandbox);
  }

  @Override
  public String getDescription() {
    return "Send issue backlog telemetry on first analysis, and per-issue update telemetry on subsequent analyses, excluding pull requests";
  }

  private record RuleCounts(Map<String, Integer> totalByRule, Map<String, Map<IssueStatus, Integer>> statusCountsByRule) {
  }
}
