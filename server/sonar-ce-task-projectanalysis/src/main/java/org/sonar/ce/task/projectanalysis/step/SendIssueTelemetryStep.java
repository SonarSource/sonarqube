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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.CheckForNull;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.IssueCounts;
import org.sonar.ce.task.projectanalysis.issue.IssueCountsByRuleHolder;
import org.sonar.ce.task.projectanalysis.issue.TargetBranchComponentUuids;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.telemetry.core.event.AnalyticsEventPublisher;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent.RuleCounts;

import static org.sonar.core.config.PurgeConstants.BRANCHES_TO_KEEP_WHEN_INACTIVE;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

public class SendIssueTelemetryStep implements ComputationStep {

  private static final Logger LOG = LoggerFactory.getLogger(SendIssueTelemetryStep.class);

  private final Configuration config;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final TreeRootHolder treeRootHolder;
  private final IssueCountsByRuleHolder issueCountsByRuleHolder;
  private final TargetBranchComponentUuids targetBranchComponentUuids;
  private final AnalyticsEventPublisher analyticsEventPublisher;
  private final ConfigurationRepository configurationRepository;

  public SendIssueTelemetryStep(Configuration config, AnalysisMetadataHolder analysisMetadataHolder,
    TreeRootHolder treeRootHolder, IssueCountsByRuleHolder issueCountsByRuleHolder, TargetBranchComponentUuids targetBranchComponentUuids,
    AnalyticsEventPublisher analyticsEventPublisher, ConfigurationRepository configurationRepository) {
    this.config = config;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.treeRootHolder = treeRootHolder;
    this.issueCountsByRuleHolder = issueCountsByRuleHolder;
    this.targetBranchComponentUuids = targetBranchComponentUuids;
    this.analyticsEventPublisher = analyticsEventPublisher;
    this.configurationRepository = configurationRepository;
  }

  @Override
  public void execute(Context context) {
    if (!config.getBoolean(SONAR_TELEMETRY_ENABLE.getKey()).orElse(false)) {
      return;
    }
    try {
      publishIssueCountsByRule();
    } catch (RuntimeException e) {
      // Telemetry must never fail an analysis.
      LOG.warn("Failed to send issue telemetry", e);
    }
  }

  private void publishIssueCountsByRule() {
    Branch branch = analysisMetadataHolder.getBranch();
    if (!isNonPurgableBranchOrPullRequest(branch)) {
      return;
    }

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

  /**
   * Only branches excluded from automatic purge (main branch, or a branch matching
   * {@code sonar.dbcleaner.branchesToKeepWhenInactive}) are worth reporting backlog telemetry
   * for, since purgable branches are short-lived. Pull requests are always purgable but are
   * reported anyway, as their backlog is still a useful signal.
   */
  private boolean isNonPurgableBranchOrPullRequest(Branch branch) {
    if (branch.isMain() || analysisMetadataHolder.isPullRequest()) {
      return true;
    }
    String[] branchesToKeep = configurationRepository.getConfiguration().getStringArray(BRANCHES_TO_KEEP_WHEN_INACTIVE);
    return Arrays.stream(branchesToKeep)
      .map(Pattern::compile)
      .anyMatch(pattern -> pattern.matcher(branch.getName()).matches());
  }

  /**
   * The branch this analysis is compared against: the target branch for a pull request, or the
   * reference branch for a regular branch — {@code null} only for the main branch, which has
   * nothing to merge into. {@link Branch#getReferenceBranchUuid()} is the new-code-period
   * reference branch, which is unrelated to (and, unless left unset, different from) a PR's
   * target/base branch, so it must not be used for pull requests; {@link TargetBranchComponentUuids}
   * already resolves the target branch's uuid as a side effect of PR issue tracking
   * ({@code IntegrateIssuesVisitor} triggers it for every component, earlier in the same
   * {@code ExecuteVisitorsStep} crawl), so reading it here adds no extra DB IO.
   */
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

  @Override
  public String getDescription() {
    return "Send issue telemetry";
  }
}
