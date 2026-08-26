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

import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

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
    if (!config.getBoolean(SONAR_TELEMETRY_ENABLE.getKey()).orElse(false)
      || !analysisMetadataHolder.isFirstAnalysis()
      || analysisMetadataHolder.isPullRequest()) {
      return;
    }

    try {
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
    } catch (RuntimeException e) {
      // Telemetry must never fail an analysis.
      LOG.warn("Failed to send issue backlog telemetry", e);
    }
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
    return "Send issue backlog telemetry on first analysis of a branch, excluding pull requests";
  }

  private record RuleCounts(Map<String, Integer> totalByRule, Map<String, Map<IssueStatus, Integer>> statusCountsByRule) {
  }
}
