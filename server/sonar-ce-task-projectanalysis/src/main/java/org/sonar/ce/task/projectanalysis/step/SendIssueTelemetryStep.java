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

import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.CheckForNull;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.IssueCounts;
import org.sonar.ce.task.projectanalysis.issue.IssueCountsByRuleHolder;
import org.sonar.ce.task.projectanalysis.issue.ProtoIssueCache;
import org.sonar.ce.task.projectanalysis.issue.TargetBranchComponentUuids;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.scanner.protobuf.utils.CloseableIterator;
import org.sonar.telemetry.core.event.AnalyticsEventPublisher;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent.RuleCounts;
import org.sonar.telemetry.core.event.workflow.IssueDeltaTelemetryEvent;
import org.sonar.telemetry.core.event.workflow.IssueDeltaTelemetryEvent.RuleDeltaCounts;

import static org.sonar.core.config.PurgeConstants.BRANCHES_TO_KEEP_WHEN_INACTIVE;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

public class SendIssueTelemetryStep implements ComputationStep {

  private static final Logger LOG = LoggerFactory.getLogger(SendIssueTelemetryStep.class);

  private final Configuration config;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TreeRootHolder treeRootHolder;
  private final IssueCountsByRuleHolder issueCountsByRuleHolder;
  private final TargetBranchComponentUuids targetBranchComponentUuids;
  private final ProtoIssueCache protoIssueCache;
  private final AnalyticsEventPublisher analyticsEventPublisher;
  private final ConfigurationRepository configurationRepository;

  public SendIssueTelemetryStep(Configuration config, AnalysisMetadataHolder analysisMetadataHolder,
    TreeRootHolder treeRootHolder, IssueCountsByRuleHolder issueCountsByRuleHolder, TargetBranchComponentUuids targetBranchComponentUuids,
    ProtoIssueCache protoIssueCache, AnalyticsEventPublisher analyticsEventPublisher, ConfigurationRepository configurationRepository) {
    this.config = config;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.treeRootHolder = treeRootHolder;
    this.issueCountsByRuleHolder = issueCountsByRuleHolder;
    this.targetBranchComponentUuids = targetBranchComponentUuids;
    this.protoIssueCache = protoIssueCache;
    this.analyticsEventPublisher = analyticsEventPublisher;
    this.configurationRepository = configurationRepository;
  }

  @Override
  public void execute(Context context) {
    if (!config.getBoolean(SONAR_TELEMETRY_ENABLE.getKey()).orElse(false)) {
      return;
    }

    Branch branch = analysisMetadataHolder.getBranch();
    if (isNonPurgableBranchOrPullRequest(branch)) {
      try {
        publishIssueCountsByRule(branch);
      } catch (RuntimeException e) {
        // Telemetry must never fail an analysis.
        LOG.debug("Failed to send issue backlog telemetry", e);
      }
      try {
        publishIssueDeltaByRule(branch);
      } catch (RuntimeException e) {
        // Telemetry must never fail an analysis.
        LOG.debug("Failed to send issue delta telemetry", e);
      }
    }
  }

  private void publishIssueCountsByRule(Branch branch) {
    Map<RuleKey, IssueCounts> counts = issueCountsByRuleHolder.getCounts();
    if (counts.isEmpty()) {
      return;
    }

    List<RuleCounts> rules = counts.entrySet().stream()
      .map(entry -> toRuleCounts(entry.getKey(), entry.getValue()))
      .toList();

    IssueBacklogTelemetryEvent event = new IssueBacklogTelemetryEvent(
      analysisMetadataHolder.getProject().getUuid(),
      treeRootHolder.getRoot().getUuid(),
      branch.getType().name(),
      getMergeBranchUuid(branch),
      analysisMetadataHolder.getAnalysisDate(),
      rules);

    analyticsEventPublisher.publish(IssueBacklogTelemetryEvent.TYPE, event);
  }

  private boolean isNonPurgableBranchOrPullRequest(Branch branch) {
    if (branch.isMain() || analysisMetadataHolder.isPullRequest()) {
      return true;
    }
    String[] branchesToKeep = configurationRepository.getConfiguration().getStringArray(BRANCHES_TO_KEEP_WHEN_INACTIVE);
    return Arrays.stream(branchesToKeep)
      .map(Pattern::compile)
      .anyMatch(pattern -> pattern.matcher(branch.getName()).matches());
  }

  @CheckForNull
  private String getMergeBranchUuid(Branch branch) {
    if (branch.isMain()) {
      return null;
    }
    if (analysisMetadataHolder.isPullRequest()) {
      return targetBranchComponentUuids.getTargetBranchUuid();
    }
    return branch.getReferenceBranchUuid();
  }

  private static RuleCounts toRuleCounts(RuleKey ruleKey, IssueCounts counts) {
    return new RuleCounts(ruleKey.toString(), counts.open(), counts.confirmed(),
      counts.falsePositive(), counts.accepted(), counts.inSandbox());
  }


  private void publishIssueDeltaByRule(Branch branch) {
    Map<RuleKey, DeltaCounts> counts = new HashMap<>();
    long analysisDate = analysisMetadataHolder.getAnalysisDate();
    try (CloseableIterator<DefaultIssue> issues = protoIssueCache.traverse()) {
      while (issues.hasNext()) {
        accumulateDelta(counts, issues.next(), new Date(analysisDate));
      }
    }
    if (counts.isEmpty()) {
      return;
    }

    List<RuleDeltaCounts> rules = counts.entrySet().stream()
      .map(entry -> toRuleDeltaCounts(entry.getKey(), entry.getValue()))
      .toList();

    IssueDeltaTelemetryEvent event = new IssueDeltaTelemetryEvent(
      analysisMetadataHolder.getProject().getUuid(),
      treeRootHolder.getRoot().getUuid(),
      branch.getType().name(),
      getMergeBranchUuid(branch),
      analysisDate,
      rules);

    analyticsEventPublisher.publish(IssueDeltaTelemetryEvent.TYPE, event);
  }


  private static void accumulateDelta(Map<RuleKey, DeltaCounts> counts, DefaultIssue issue, Date analysisDate) {
    if (issue.isBeingClosed()) {
      if (Issue.RESOLUTION_FIXED.equals(issue.resolution())) {
        Date closeDate = issue.closeDate() != null ? issue.closeDate() : analysisDate;
        long fixDurationDays = Duration.between(getDetectionDate(issue).toInstant(), closeDate.toInstant()).toDays();
        counts.computeIfAbsent(issue.ruleKey(), k -> new DeltaCounts()).incrementFixed(fixDurationDays);
      }
      return;
    }
    if (!issue.isNew()) {
      return;
    }
    IssueStatus status = issue.issueStatus();
    if (status == IssueStatus.OPEN) {
      counts.computeIfAbsent(issue.ruleKey(), k -> new DeltaCounts()).incrementNewOpen();
    } else if (status == IssueStatus.IN_SANDBOX) {
      counts.computeIfAbsent(issue.ruleKey(), k -> new DeltaCounts()).incrementNewInSandbox();
    }
  }

  /**
   * Mirrors {@code FixedIssueVisitor#getDetectionDate}, so time-to-fix stays consistent with the
   * Hunter Agent TTR history feature: {@code detectionDate} reflects when the issue was actually
   * first seen, falling back to {@code creationDate} when unset.
   */
  private static Date getDetectionDate(DefaultIssue issue) {
    Date detectionDate = issue.detectionDate();
    return detectionDate != null ? detectionDate : issue.creationDate();
  }

  private static RuleDeltaCounts toRuleDeltaCounts(RuleKey ruleKey, DeltaCounts counts) {
    return new RuleDeltaCounts(ruleKey.toString(), counts.newOpen, counts.newInSandbox, counts.newFixed,
      counts.newFixedIn1Days, counts.newFixedIn7Days, counts.newFixedIn30Days);
  }

  @Override
  public String getDescription() {
    return "Send issue telemetry";
  }

  /**
   * Mutable per-rule delta accumulator. {@code newFixedInNDays} counters are cumulative
   * (fixed within &le; N days), not disjoint histogram buckets.
   */
  private static final class DeltaCounts {
    private int newOpen = 0;
    private int newInSandbox = 0;
    private int newFixed = 0;
    private int newFixedIn1Days = 0;
    private int newFixedIn7Days = 0;
    private int newFixedIn30Days = 0;

    private void incrementNewOpen() {
      newOpen++;
    }

    private void incrementNewInSandbox() {
      newInSandbox++;
    }

    private void incrementFixed(long fixDurationDays) {
      newFixed++;
      if (fixDurationDays <= 1) {
        newFixedIn1Days++;
      }
      if (fixDurationDays <= 7) {
        newFixedIn7Days++;
      }
      if (fixDurationDays <= 30) {
        newFixedIn30Days++;
      }
    }
  }
}
