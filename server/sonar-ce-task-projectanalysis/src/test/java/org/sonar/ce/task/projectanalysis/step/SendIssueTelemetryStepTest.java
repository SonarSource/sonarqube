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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.System2;
import org.sonar.ce.task.projectanalysis.analysis.Analysis;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolderRule;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.issue.ProtoIssueCache;
import org.sonar.ce.task.step.TestComputationStepContext;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.rule.RuleType;
import org.sonar.db.component.BranchType;
import org.sonar.server.project.Project;
import org.sonar.telemetry.core.event.AnalyticsEventPublisher;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent;
import org.sonar.telemetry.core.event.workflow.IssueBacklogTelemetryEvent.RuleBacklog;
import org.sonar.telemetry.core.event.workflow.IssueUpdatedBatchEvent;
import org.sonar.telemetry.core.event.workflow.IssueUpdatedBatchEvent.IssueUpdate;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ISSUE_EVENTS_MAX_PER_ANALYSIS;

class SendIssueTelemetryStepTest {

  private static final String PROJECT_UUID = "project-uuid";
  private static final String BRANCH_UUID = "branch-uuid";
  private static final long ANALYSIS_DATE = 1_700_000_000_000L;

  @RegisterExtension
  private final TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();

  @RegisterExtension
  private final AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();

  @TempDir
  private Path tempDir;

  private final Configuration config = mock(Configuration.class);
  private final AnalyticsEventPublisher analyticsEventPublisher = mock(AnalyticsEventPublisher.class);
  private final Branch branch = mock(Branch.class);

  private ProtoIssueCache protoIssueCache;
  private SendIssueTelemetryStep underTest;

  @BeforeEach
  void setUp() throws IOException {
    protoIssueCache = new ProtoIssueCache(Files.createTempFile(tempDir, "issues", ".dat").toFile(), System2.INSTANCE);
    underTest = new SendIssueTelemetryStep(config, analysisMetadataHolder, treeRootHolder, protoIssueCache, analyticsEventPublisher);

    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1).setUuid(BRANCH_UUID).setKey("key").build());
    analysisMetadataHolder
      .setProject(new Project(PROJECT_UUID, "key", "name", null, emptyList()))
      .setBranch(branch)
      .setAnalysisDate(ANALYSIS_DATE);
    when(branch.getType()).thenReturn(BranchType.BRANCH);
    when(config.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(true));
  }

  @Test
  void execute_whenFirstAnalysis_publishesOneBacklogEventWithCountsPerRule() {
    givenFirstAnalysis();
    givenCachedIssues(
      openIssue("issue1", "java", "S1234"),
      openIssue("issue2", "java", "S1234"),
      openIssue("issue3", "javascript", "S5678"));

    underTest.execute(new TestComputationStepContext());

    ArgumentCaptor<IssueBacklogTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueBacklogTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(eq(IssueBacklogTelemetryEvent.TYPE), eventCaptor.capture());
    IssueBacklogTelemetryEvent event = eventCaptor.getValue();
    assertThat(event.projectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(event.branchUuid()).isEqualTo(BRANCH_UUID);
    assertThat(event.branchType()).isEqualTo("BRANCH");
    assertThat(event.analysisDate()).isEqualTo(ANALYSIS_DATE);
    assertThat(event.rules()).containsExactlyInAnyOrder(
      new RuleBacklog("java:S1234", 2, 2, 0, 0, 0, 0),
      new RuleBacklog("javascript:S5678", 1, 1, 0, 0, 0, 0));
  }

  @Test
  void execute_whenFirstAnalysis_shouldBucketOneIssuePerStatus() {
    givenFirstAnalysis();
    givenCachedIssues(
      issueWithStatus("issueOpen", "java", "S1234", Issue.STATUS_OPEN, null),
      issueWithStatus("issueConfirmed", "java", "S1234", Issue.STATUS_CONFIRMED, null),
      issueWithStatus("issueFalsePositive", "java", "S1234", Issue.STATUS_RESOLVED, Issue.RESOLUTION_FALSE_POSITIVE),
      issueWithStatus("issueAccepted", "java", "S1234", Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX),
      issueWithStatus("issueInSandbox", "java", "S1234", Issue.STATUS_IN_SANDBOX, null));

    underTest.execute(new TestComputationStepContext());

    ArgumentCaptor<IssueBacklogTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueBacklogTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(any(), eventCaptor.capture());
    IssueBacklogTelemetryEvent event = eventCaptor.getValue();
    assertThat(event.rules()).containsExactly(new RuleBacklog("java:S1234", 5, 1, 1, 1, 1, 1));
  }

  @Test
  void execute_whenFirstAnalysis_shouldIncludeClosedIssueInTotalButNotInBreakdown() {
    givenFirstAnalysis();
    givenCachedIssues(
      openIssue("issue1", "java", "S1234"),
      issueWithStatus("issue2", "java", "S1234", Issue.STATUS_CLOSED, null));

    underTest.execute(new TestComputationStepContext());

    ArgumentCaptor<IssueBacklogTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueBacklogTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(any(), eventCaptor.capture());
    IssueBacklogTelemetryEvent event = eventCaptor.getValue();
    assertThat(event.rules()).containsExactly(new RuleBacklog("java:S1234", 2, 1, 0, 0, 0, 0));
  }

  @Test
  void execute_whenFirstAnalysis_shouldIncludeFixedIssueInTotalButNotInBreakdown() {
    // e.g. copied from the reference branch's non-closed issues on a secondary branch's first
    // analysis, where it was already manually "Resolved as Fixed" but not yet swept to CLOSED.
    givenFirstAnalysis();
    givenCachedIssues(
      openIssue("issue1", "java", "S1234"),
      issueWithStatus("issue2", "java", "S1234", Issue.STATUS_RESOLVED, Issue.RESOLUTION_FIXED));

    underTest.execute(new TestComputationStepContext());

    ArgumentCaptor<IssueBacklogTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueBacklogTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(any(), eventCaptor.capture());
    IssueBacklogTelemetryEvent event = eventCaptor.getValue();
    assertThat(event.rules()).containsExactly(new RuleBacklog("java:S1234", 2, 1, 0, 0, 0, 0));
  }

  @Test
  void execute_whenFirstAnalysis_shouldIncludeHotspotInTotalButNotInBreakdown() {
    givenFirstAnalysis();
    givenCachedIssues(
      openIssue("issue1", "java", "S1234"),
      issueWithStatus("issue2", "java", "S1234", Issue.STATUS_TO_REVIEW, null),
      issueWithStatus("issue3", "java", "S1234", Issue.STATUS_REVIEWED, null));

    underTest.execute(new TestComputationStepContext());

    ArgumentCaptor<IssueBacklogTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueBacklogTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(any(), eventCaptor.capture());
    IssueBacklogTelemetryEvent event = eventCaptor.getValue();
    assertThat(event.rules()).containsExactly(new RuleBacklog("java:S1234", 3, 1, 0, 0, 0, 0));
  }

  @Test
  void execute_whenFirstAnalysis_shouldEmitRecordWithZeroBreakdownForRuleWithOnlyClosedOrHotspotIssues() {
    givenFirstAnalysis();
    givenCachedIssues(
      openIssue("issue1", "java", "S1234"),
      issueWithStatus("issue2", "javascript", "S5678", Issue.STATUS_CLOSED, null),
      issueWithStatus("issue3", "python", "S9999", Issue.STATUS_TO_REVIEW, null));

    underTest.execute(new TestComputationStepContext());

    ArgumentCaptor<IssueBacklogTelemetryEvent> eventCaptor = ArgumentCaptor.forClass(IssueBacklogTelemetryEvent.class);
    verify(analyticsEventPublisher).publish(any(), eventCaptor.capture());
    IssueBacklogTelemetryEvent event = eventCaptor.getValue();
    assertThat(event.rules()).containsExactlyInAnyOrder(
      new RuleBacklog("java:S1234", 1, 1, 0, 0, 0, 0),
      new RuleBacklog("javascript:S5678", 1, 0, 0, 0, 0, 0),
      new RuleBacklog("python:S9999", 1, 0, 0, 0, 0, 0));
  }

  @Test
  void execute_whenCacheEmpty_doesNotPublish() {
    givenFirstAnalysis();
    givenCachedIssues();

    underTest.execute(new TestComputationStepContext());

    verifyNoInteractions(analyticsEventPublisher);
  }

  @Test
  void execute_whenNotFirstAnalysis_publishesOnlyNewChangedAndCopiedIssues() {
    givenNotFirstAnalysis();
    givenCachedIssues(
      newIssue("issueNew", "S1234", Issue.STATUS_OPEN, null),
      changedIssue("issueChanged", "S1234", Issue.STATUS_CONFIRMED, null),
      copiedIssue("issueCopied", "S1234", Issue.STATUS_OPEN, null),
      untouchedIssue("issueUntouched", "S1234", Issue.STATUS_OPEN, null));

    underTest.execute(new TestComputationStepContext());

    ArgumentCaptor<IssueUpdatedBatchEvent> eventCaptor = ArgumentCaptor.forClass(IssueUpdatedBatchEvent.class);
    verify(analyticsEventPublisher).publish(eq(IssueUpdatedBatchEvent.TYPE), eventCaptor.capture());
    IssueUpdatedBatchEvent event = eventCaptor.getValue();
    assertThat(event.projectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(event.branchId()).isEqualTo(BRANCH_UUID);
    assertThat(event.branchType()).isEqualTo("BRANCH");
    assertThat(event.issues()).extracting(IssueUpdate::issueKey)
      .containsExactlyInAnyOrder("issueNew", "issueChanged", "issueCopied");
  }

  @Test
  void execute_whenNotFirstAnalysis_mapsIssueFieldsAndStatus() {
    givenNotFirstAnalysis();
    DefaultIssue issue = changedIssue("issue1", "S1234", Issue.STATUS_CLOSED, Issue.RESOLUTION_REMOVED)
      .setCloseDate(new Date(ANALYSIS_DATE + 1_000));
    givenCachedIssues(issue);

    underTest.execute(new TestComputationStepContext());

    ArgumentCaptor<IssueUpdatedBatchEvent> eventCaptor = ArgumentCaptor.forClass(IssueUpdatedBatchEvent.class);
    verify(analyticsEventPublisher).publish(eq(IssueUpdatedBatchEvent.TYPE), eventCaptor.capture());
    IssueUpdate issueUpdate = eventCaptor.getValue().issues().get(0);
    assertThat(issueUpdate.issueKey()).isEqualTo("issue1");
    assertThat(issueUpdate.pluginRuleKey()).isEqualTo("java:S1234");
    assertThat(issueUpdate.issueRaisedAt()).isEqualTo(ANALYSIS_DATE);
    assertThat(issueUpdate.issueStatus()).isEqualTo("REMOVED");
    assertThat(issueUpdate.issueResolvedAt()).isEqualTo(ANALYSIS_DATE + 1_000);
  }

  @Test
  void execute_whenIssueStatusSetDirectlyBypassingWorkflow_isStillReported() {
    // Simulates UpdateConflictResolver / AnalyzerUpdateIssueVisitor / HunterAgentIssueDiffer, which
    // set status directly and flip isChanged() without going through workflow or changelog.
    givenNotFirstAnalysis();
    givenCachedIssues(changedIssue("issue1", "S1234", Issue.STATUS_CLOSED, Issue.RESOLUTION_REMOVED));

    List<IssueUpdatedBatchEvent> events = executeAndCaptureIssueUpdateEvents();

    assertThat(events).hasSize(1);
    assertThat(events.get(0).issues().get(0).issueStatus()).isEqualTo("REMOVED");
  }

  @Test
  void execute_whenNotFirstAnalysis_andNoChangedIssues_doesNotPublish() {
    givenNotFirstAnalysis();
    givenCachedIssues(untouchedIssue("issue1", "S1234", Issue.STATUS_OPEN, null));

    underTest.execute(new TestComputationStepContext());

    verifyNoInteractions(analyticsEventPublisher);
  }

  @Test
  void execute_whenNotFirstAnalysis_andCacheEmpty_doesNotPublish() {
    givenNotFirstAnalysis();
    givenCachedIssues();

    underTest.execute(new TestComputationStepContext());

    verifyNoInteractions(analyticsEventPublisher);
  }

  @Test
  void execute_whenNotFirstAnalysis_andOneChangedIssue_publishesOneEventWithOneElementArray() {
    givenNotFirstAnalysis();
    givenCachedIssues(changedIssues(1));

    List<IssueUpdatedBatchEvent> events = executeAndCaptureIssueUpdateEvents();

    assertThat(events).hasSize(1);
    assertThat(events.get(0).issues()).hasSize(1);
  }

  @Test
  void execute_whenNotFirstAnalysis_andExactly500ChangedIssues_publishesOneEventNotOnePlusEmpty() {
    givenNotFirstAnalysis();
    givenCachedIssues(changedIssues(500));

    List<IssueUpdatedBatchEvent> events = executeAndCaptureIssueUpdateEvents();

    assertThat(events).hasSize(1);
    assertThat(events.get(0).issues()).hasSize(500);
  }

  @Test
  void execute_whenNotFirstAnalysis_and501ChangedIssues_publishesTwoEventsOf500And1() {
    givenNotFirstAnalysis();
    givenCachedIssues(changedIssues(501));

    List<IssueUpdatedBatchEvent> events = executeAndCaptureIssueUpdateEvents();

    assertThat(events).extracting(e -> e.issues().size()).containsExactly(500, 1);
    assertUnionOfIssueKeysHasNoDuplicatesOrDrops(events, 501);
  }

  @Test
  void execute_whenNotFirstAnalysis_and1000ChangedIssues_publishesExactlyTwoFullEvents() {
    givenNotFirstAnalysis();
    givenCachedIssues(changedIssues(1_000));

    List<IssueUpdatedBatchEvent> events = executeAndCaptureIssueUpdateEvents();

    assertThat(events).extracting(e -> e.issues().size()).containsExactly(500, 500);
    assertUnionOfIssueKeysHasNoDuplicatesOrDrops(events, 1_000);
  }

  @Test
  void execute_whenChangedIssueCountExceedsMaxPerAnalysis_sendsOnlyTheFirstMaxPerAnalysisIssues() {
    when(config.getInt(SONAR_TELEMETRY_ISSUE_EVENTS_MAX_PER_ANALYSIS.getKey())).thenReturn(Optional.of(2));
    givenNotFirstAnalysis();
    givenCachedIssues(changedIssues(3));

    List<IssueUpdatedBatchEvent> events = executeAndCaptureIssueUpdateEvents();

    verify(analyticsEventPublisher, never()).publish(eq(IssueBacklogTelemetryEvent.TYPE), any());
    assertThat(events).hasSize(1);
    assertThat(events.get(0).issues()).hasSize(2);
  }

  @Test
  void execute_whenNotFirstAnalysisAndPullRequest_doesNotPublish() {
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    givenNotFirstAnalysis();
    givenCachedIssues(changedIssues(1));

    underTest.execute(new TestComputationStepContext());

    verifyNoInteractions(analyticsEventPublisher);
  }

  @Test
  void execute_whenPullRequestFirstAnalysis_doesNotPublish() {
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    givenFirstAnalysis();
    givenCachedIssues(openIssue("issue1", "java", "S1234"));

    underTest.execute(new TestComputationStepContext());

    verifyNoInteractions(analyticsEventPublisher);
  }

  @Test
  void execute_whenTelemetryDisabled_doesNotPublish() {
    when(config.getBoolean("sonar.telemetry.enable")).thenReturn(Optional.of(false));
    givenFirstAnalysis();
    givenCachedIssues(openIssue("issue1", "java", "S1234"));

    underTest.execute(new TestComputationStepContext());

    verifyNoInteractions(analyticsEventPublisher);
  }

  private void givenFirstAnalysis() {
    analysisMetadataHolder.setBaseAnalysis(null);
  }

  private void givenNotFirstAnalysis() {
    analysisMetadataHolder.setBaseAnalysis(mock(Analysis.class));
  }

  private List<IssueUpdatedBatchEvent> executeAndCaptureIssueUpdateEvents() {
    underTest.execute(new TestComputationStepContext());
    ArgumentCaptor<IssueUpdatedBatchEvent> eventCaptor = ArgumentCaptor.forClass(IssueUpdatedBatchEvent.class);
    verify(analyticsEventPublisher, atLeastOnce()).publish(eq(IssueUpdatedBatchEvent.TYPE), eventCaptor.capture());
    return eventCaptor.getAllValues();
  }

  private static void assertUnionOfIssueKeysHasNoDuplicatesOrDrops(List<IssueUpdatedBatchEvent> events, int expectedCount) {
    List<String> issueKeys = events.stream()
      .flatMap(event -> event.issues().stream())
      .map(IssueUpdate::issueKey)
      .toList();
    assertThat(issueKeys).hasSize(expectedCount);
    assertThat(new HashSet<>(issueKeys)).hasSize(expectedCount);
  }

  private void givenCachedIssues(DefaultIssue... issues) {
    var appender = protoIssueCache.newAppender();
    for (DefaultIssue issue : issues) {
      appender.append(issue);
    }
    appender.close();
  }

  private static DefaultIssue openIssue(String issueKey, String repository, String ruleKey) {
    return issueWithStatus(issueKey, repository, ruleKey, Issue.STATUS_OPEN, null);
  }

  private static DefaultIssue issueWithStatus(String issueKey, String repository, String ruleKey, String status, String resolution) {
    return new DefaultIssue()
      .setKey(issueKey)
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(RuleKey.of(repository, ruleKey))
      .setComponentUuid("fileUuid")
      .setComponentKey("fileKey")
      .setProjectUuid(BRANCH_UUID)
      .setProjectKey("key")
      .setStatus(status)
      .setResolution(resolution)
      .setCreationDate(new Date(ANALYSIS_DATE))
      .setNew(true);
  }

  private static DefaultIssue newIssue(String issueKey, String ruleKey, String status, String resolution) {
    return issueWithStatus(issueKey, "java", ruleKey, status, resolution);
  }

  private static DefaultIssue changedIssue(String issueKey, String ruleKey, String status, String resolution) {
    return issueWithStatus(issueKey, "java", ruleKey, status, resolution).setNew(false).setChanged(true);
  }

  private static DefaultIssue copiedIssue(String issueKey, String ruleKey, String status, String resolution) {
    return issueWithStatus(issueKey, "java", ruleKey, status, resolution).setNew(false).setCopied(true);
  }

  private static DefaultIssue untouchedIssue(String issueKey, String ruleKey, String status, String resolution) {
    return issueWithStatus(issueKey, "java", ruleKey, status, resolution).setNew(false);
  }

  private static DefaultIssue[] changedIssues(int count) {
    return IntStream.range(0, count)
      .mapToObj(i -> changedIssue("issue" + i, "S1234", Issue.STATUS_OPEN, null))
      .toArray(DefaultIssue[]::new);
  }
}
