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

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.IssueCountsByRuleHolderImpl;
import org.sonar.ce.task.projectanalysis.issue.ProtoIssueCache;
import org.sonar.ce.task.projectanalysis.issue.TargetBranchComponentUuids;
import org.sonar.ce.task.step.ComputationStep;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.rule.RuleType;
import org.sonar.db.component.BranchType;
import org.sonar.server.project.Project;
import org.sonar.telemetry.core.event.AnalyticsEventPublisher;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent.RuleCounts;
import org.sonar.telemetry.core.event.workflow.IssueDeltaTelemetryEvent;
import org.sonar.telemetry.core.event.workflow.IssueDeltaTelemetryEvent.RuleDeltaCounts;

import static java.util.concurrent.TimeUnit.DAYS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.config.PurgeConstants.BRANCHES_TO_KEEP_WHEN_INACTIVE;

class SendIssueTelemetryStepTest {

  private static final RuleKey RULE_1 = RuleKey.of("java", "S1234");
  private static final RuleKey RULE_2 = RuleKey.of("xoo", "x1");
  private static final long ANALYSIS_DATE = 1_700_000_000_000L;

  private final Configuration configuration = mock();
  private final AnalysisMetadataHolder analysisMetadataHolder = mock();
  private final TreeRootHolder treeRootHolder = mock();
  private final IssueCountsByRuleHolderImpl issueCountsByRuleHolder = new IssueCountsByRuleHolderImpl();
  private final TargetBranchComponentUuids targetBranchComponentUuids = mock();
  private final AnalyticsEventPublisher analyticsEventPublisher = mock();
  private final ConfigurationRepository configurationRepository = mock();
  private final Configuration projectConfiguration = mock();
  private final Branch branch = mock();
  private final Component root = mock();
  private final ComputationStep.Context context = mock();

  @TempDir
  private File tempDir;
  private ProtoIssueCache protoIssueCache;

  private SendIssueTelemetryStep underTest;

  {
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(true));
    when(analysisMetadataHolder.getProject()).thenReturn(new Project("project-uuid", "key", "name", null, Collections.emptyList()));
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);
    when(analysisMetadataHolder.getAnalysisDate()).thenReturn(ANALYSIS_DATE);
    when(branch.getType()).thenReturn(BranchType.BRANCH);
    when(branch.isMain()).thenReturn(false);
    when(analysisMetadataHolder.isPullRequest()).thenReturn(false);
    when(branch.getReferenceBranchUuid()).thenReturn("reference-branch-uuid");
    when(treeRootHolder.getRoot()).thenReturn(root);
    when(root.getUuid()).thenReturn("branch-uuid");
    when(configurationRepository.getConfiguration()).thenReturn(projectConfiguration);
    when(projectConfiguration.getStringArray(BRANCHES_TO_KEEP_WHEN_INACTIVE)).thenReturn(new String[0]);
  }

  @BeforeEach
  void setUpProtoIssueCache() {
    // @TempDir field injection happens after instance construction, so tempDir must not be
    // touched from the instance initializer above — it would still be null there.
    protoIssueCache = new ProtoIssueCache(new File(tempDir, "issues.dat"), System2.INSTANCE);
    underTest = new SendIssueTelemetryStep(configuration, analysisMetadataHolder,
      treeRootHolder, issueCountsByRuleHolder, targetBranchComponentUuids, protoIssueCache, analyticsEventPublisher, configurationRepository);
  }

  private static DefaultIssue newBaseIssue(String key, RuleKey ruleKey) {
    return new DefaultIssue()
      .setKey(key)
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(ruleKey)
      .setComponentKey("componentKey")
      .setProjectUuid("project-uuid")
      .setProjectKey("projectKey")
      .setCreationDate(new Date(ANALYSIS_DATE));
  }

  private static DefaultIssue newOpenIssue(RuleKey ruleKey) {
    return newBaseIssue("issue-" + ruleKey, ruleKey)
      .setStatus(Issue.STATUS_OPEN)
      .setNew(true);
  }

  private static DefaultIssue newInSandboxIssue(RuleKey ruleKey) {
    return newBaseIssue("issue-sandbox-" + ruleKey, ruleKey)
      .setStatus(Issue.STATUS_IN_SANDBOX)
      .setNew(true);
  }

  private static DefaultIssue newFixedIssue(RuleKey ruleKey, long fixDurationDays) {
    Date creationDate = new Date(ANALYSIS_DATE - DAYS.toMillis(fixDurationDays));
    return newBaseIssue("issue-fixed-" + ruleKey + "-" + fixDurationDays, ruleKey)
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setCreationDate(creationDate)
      .setCloseDate(new Date(ANALYSIS_DATE))
      .setBeingClosed(true);
  }

  private static DefaultIssue newFixedIssueWithoutCloseDate(RuleKey ruleKey, long createdDaysAgo) {
    Date creationDate = new Date(ANALYSIS_DATE - DAYS.toMillis(createdDaysAgo));
    return newBaseIssue("issue-fixed-no-close-date-" + ruleKey, ruleKey)
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_FIXED)
      .setCreationDate(creationDate)
      .setBeingClosed(true);
  }

  private static DefaultIssue newRemovedIssue(RuleKey ruleKey) {
    return newBaseIssue("issue-removed-" + ruleKey, ruleKey)
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(Issue.RESOLUTION_REMOVED)
      .setCloseDate(new Date(ANALYSIS_DATE))
      .setBeingClosed(true);
  }

  @Test
  void execute_whenTelemetryDisabled_doesNotPublish() {
    when(configuration.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(false));
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);

    underTest.execute(context);

    verifyNoInteractions(analyticsEventPublisher);
  }

  @Test
  void execute_whenNoIssueCounted_doesNotPublish() {
    when(branch.isMain()).thenReturn(true);

    underTest.execute(context);

    verifyNoInteractions(analyticsEventPublisher);
  }

  @Test
  void execute_whenPurgableRegularBranch_doesNotPublish() {
    when(branch.getName()).thenReturn("feature-branch");
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);

    underTest.execute(context);

    verifyNoInteractions(analyticsEventPublisher);
  }

  @Test
  void execute_whenCountsPresent_publishesOneEventWithAllRules() {
    when(branch.getName()).thenReturn("main-line");
    when(projectConfiguration.getStringArray(BRANCHES_TO_KEEP_WHEN_INACTIVE)).thenReturn(new String[] {"main-line"});
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.ACCEPTED);

    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.CONFIRMED);
    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.CONFIRMED);
    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.FALSE_POSITIVE);
    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.IN_SANDBOX);
    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.IN_SANDBOX);
    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.IN_SANDBOX);
    issueCountsByRuleHolder.increment(RULE_2, IssueStatus.IN_SANDBOX);

    underTest.execute(context);

    ArgumentCaptor<IssueBacklogTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueBacklogTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(eq(IssueBacklogTelemetryEvent.TYPE), eventCaptor.capture());
    IssueBacklogTelemetryEvent event = eventCaptor.getValue();

    assertThat(event.projectUuid()).isEqualTo("project-uuid");
    assertThat(event.branchUuid()).isEqualTo("branch-uuid");
    assertThat(event.branchType()).isEqualTo("BRANCH");
    assertThat(event.mergeBranchUuid()).isEqualTo("reference-branch-uuid");
    assertThat(event.analysisDate()).isEqualTo(1_700_000_000_000L);
    assertThat(event.rules()).containsExactlyInAnyOrder(
      new RuleCounts("java:S1234", 3, 0, 0, 1, 0),
      new RuleCounts("xoo:x1", 0, 2, 1, 0, 4));
    verifyNoInteractions(targetBranchComponentUuids);
  }

  @Test
  void execute_whenPullRequestAnalysis_reflectsBranchTypeAndTargetBranchUuidInEvent() {
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(analysisMetadataHolder.isPullRequest()).thenReturn(true);
    when(targetBranchComponentUuids.getTargetBranchUuid()).thenReturn("target-branch-uuid");
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);

    underTest.execute(context);

    ArgumentCaptor<IssueBacklogTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueBacklogTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(eq(IssueBacklogTelemetryEvent.TYPE), eventCaptor.capture());
    IssueBacklogTelemetryEvent event = eventCaptor.getValue();
    assertThat(event.branchType()).isEqualTo("PULL_REQUEST");
    assertThat(event.mergeBranchUuid()).isEqualTo("target-branch-uuid");
    verify(branch, never()).getReferenceBranchUuid();
  }

  @Test
  void execute_whenMainBranch_mergeBranchUuidIsNullAndReferenceBranchUuidIsNeverCalled() {
    when(branch.isMain()).thenReturn(true);
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);

    underTest.execute(context);

    ArgumentCaptor<IssueBacklogTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueBacklogTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(eq(IssueBacklogTelemetryEvent.TYPE), eventCaptor.capture());
    assertThat(eventCaptor.getValue().mergeBranchUuid()).isNull();
    verify(branch, never()).getReferenceBranchUuid();
    verifyNoInteractions(targetBranchComponentUuids);
  }

  @Test
  void execute_whenProtoIssueCacheEmpty_doesNotPublishDeltaEvent() {
    when(branch.isMain()).thenReturn(true);
    issueCountsByRuleHolder.increment(RULE_1, IssueStatus.OPEN);

    underTest.execute(context);

    verify(analyticsEventPublisher, never()).publish(eq(IssueDeltaTelemetryEvent.TYPE), any());
  }

  @Test
  void execute_whenPurgableRegularBranch_doesNotPublishDeltaEvent() {
    when(branch.getName()).thenReturn("feature-branch");
    protoIssueCache.newAppender().append(newOpenIssue(RULE_1)).close();

    underTest.execute(context);

    verify(analyticsEventPublisher, never()).publish(eq(IssueDeltaTelemetryEvent.TYPE), any());
  }

  @Test
  void execute_whenCacheHasNewAndFixedIssues_publishesOneDeltaEventWithAllRules() {
    when(branch.isMain()).thenReturn(true);

    protoIssueCache.newAppender()
      .append(newOpenIssue(RULE_1))
      .append(newOpenIssue(RULE_1))
      .append(newInSandboxIssue(RULE_1))
      .append(newFixedIssue(RULE_1, 0))
      .append(newFixedIssue(RULE_1, 5))
      .append(newFixedIssue(RULE_1, 20))
      .append(newFixedIssue(RULE_2, 40))
      .close();

    underTest.execute(context);

    ArgumentCaptor<IssueDeltaTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueDeltaTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(eq(IssueDeltaTelemetryEvent.TYPE), eventCaptor.capture());
    IssueDeltaTelemetryEvent event = eventCaptor.getValue();

    assertThat(event.projectUuid()).isEqualTo("project-uuid");
    assertThat(event.branchUuid()).isEqualTo("branch-uuid");
    assertThat(event.branchType()).isEqualTo("BRANCH");
    assertThat(event.mergeBranchUuid()).isNull();
    assertThat(event.analysisDate()).isEqualTo(ANALYSIS_DATE);
    assertThat(event.rules()).containsExactlyInAnyOrder(
      // RULE_1: 2 new open, 1 new in-sandbox, 3 fixed (0d, 5d, 20d) -> <=1d: 1, <=7d: 2, <=30d: 3
      new RuleDeltaCounts("java:S1234", 2, 1, 3, 1, 2, 3),
      // RULE_2: 1 fixed at 40d -> outside all three buckets
      new RuleDeltaCounts("xoo:x1", 0, 0, 1, 0, 0, 0));
  }

  @Test
  void execute_whenFixedIssueHasNoCloseDate_fallsBackToAnalysisDateAndStillPublishes() {
    when(branch.isMain()).thenReturn(true);
    protoIssueCache.newAppender()
      .append(newFixedIssueWithoutCloseDate(RULE_1, 5))
      .close();

    underTest.execute(context);

    ArgumentCaptor<IssueDeltaTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueDeltaTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(eq(IssueDeltaTelemetryEvent.TYPE), eventCaptor.capture());
    assertThat(eventCaptor.getValue().rules()).containsExactly(
      new RuleDeltaCounts("java:S1234", 0, 0, 1, 0, 1, 1));
  }

  @Test
  void execute_whenIssueIsClosedWithResolutionRemoved_isNotCountedAsFixed() {
    when(branch.isMain()).thenReturn(true);
    protoIssueCache.newAppender()
      .append(newFixedIssue(RULE_1, 5))
      .append(newRemovedIssue(RULE_1))
      .close();

    underTest.execute(context);

    ArgumentCaptor<IssueDeltaTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueDeltaTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(eq(IssueDeltaTelemetryEvent.TYPE), eventCaptor.capture());
    assertThat(eventCaptor.getValue().rules()).containsExactly(
      new RuleDeltaCounts("java:S1234", 0, 0, 1, 0, 1, 1));
  }

  @Test
  void execute_whenIssueIsCopied_isNotCountedAsNewOpen() {
    when(branch.isMain()).thenReturn(true);
    DefaultIssue copiedIssue = newBaseIssue("copied-issue", RULE_1)
      .setStatus(Issue.STATUS_OPEN)
      .setNew(false)
      .setCopied(true);
    protoIssueCache.newAppender().append(copiedIssue).close();

    underTest.execute(context);

    verify(analyticsEventPublisher, never()).publish(eq(IssueDeltaTelemetryEvent.TYPE), any());
  }
}
