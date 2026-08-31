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
package org.sonar.server.issue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.rule.RuleType;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.telemetry.core.event.AnalyticsEventPublisher;
import org.sonar.telemetry.core.event.workflow.IssueUpdatedBatchEvent;
import org.sonar.telemetry.core.event.workflow.IssueUpdatedBatchEvent.IssueUpdate;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.issue.IssueChangeContext.issueChangeContextByUserBuilder;

class IssueUpdatedTelemetryPublisherTest {

  private static final String PROJECT_UUID = "project-uuid";
  private static final String BRANCH_UUID = "branch-uuid";
  private static final long NOW = 1_700_000_000_000L;

  private final DbClient dbClient = mock(DbClient.class);
  private final BranchDao branchDao = mock(BranchDao.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final AnalyticsEventPublisher analyticsEventPublisher = mock(AnalyticsEventPublisher.class);

  private final IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  private final IssueChangeContext context = issueChangeContextByUserBuilder(new Date(NOW), "user-uuid").build();
  private final List<BranchDto> branches = new ArrayList<>();

  private final IssueUpdatedTelemetryPublisher underTest = new IssueUpdatedTelemetryPublisher(dbClient, analyticsEventPublisher);

  @BeforeEach
  void setUp() {
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(analyticsEventPublisher.isTelemetryEnabled()).thenReturn(true);
    when(branchDao.selectByUuids(any(), anySet())).thenAnswer(invocation -> {
      Set<?> uuids = invocation.getArgument(1);
      return branches.stream().filter(b -> uuids.contains(b.getUuid())).toList();
    });
  }

  @Test
  void publish_whenTelemetryDisabled_doesNotQueryBranchesOrPublish() {
    when(analyticsEventPublisher.isTelemetryEnabled()).thenReturn(false);
    givenBranch(BRANCH_UUID, PROJECT_UUID, BranchType.BRANCH);
    DefaultIssue issue = existingIssue("issue1", BRANCH_UUID);
    issueFieldsSetter.setStatus(issue, Issue.STATUS_CONFIRMED, context);

    underTest.publish(dbSession, singletonList(issue));

    verifyNoInteractions(branchDao);
    verify(analyticsEventPublisher, never()).publishAll(any(), any());
  }

  @Test
  void publish_whenStatusDiff_publishesIssueUpdatedTelemetry() {
    givenBranch(BRANCH_UUID, PROJECT_UUID, BranchType.BRANCH);
    DefaultIssue issue = existingIssue("issue1", BRANCH_UUID);
    issueFieldsSetter.setStatus(issue, Issue.STATUS_CONFIRMED, context);

    underTest.publish(dbSession, singletonList(issue));

    List<IssueUpdatedBatchEvent> events = capturePublishedEvents();
    assertThat(events).hasSize(1);
    IssueUpdatedBatchEvent event = events.get(0);
    assertThat(event.projectUuid()).isEqualTo(PROJECT_UUID);
    assertThat(event.branchId()).isEqualTo(BRANCH_UUID);
    assertThat(event.branchType()).isEqualTo("BRANCH");
    assertThat(event.issues()).extracting(IssueUpdate::issueKey).containsExactly("issue1");
    assertThat(event.issues().get(0).issueStatus()).isEqualTo("CONFIRMED");
  }

  @Test
  void publish_whenResolutionDiff_publishesIssueUpdatedTelemetry() {
    givenBranch(BRANCH_UUID, PROJECT_UUID, BranchType.BRANCH);
    DefaultIssue issue = existingIssue("issue1", BRANCH_UUID);
    issueFieldsSetter.setResolution(issue, Issue.RESOLUTION_FALSE_POSITIVE, context);

    underTest.publish(dbSession, singletonList(issue));

    List<IssueUpdatedBatchEvent> events = capturePublishedEvents();
    assertThat(events).hasSize(1);
    assertThat(events.get(0).issues()).extracting(IssueUpdate::issueKey).containsExactly("issue1");
  }

  @Test
  void publish_whenOnlyAssigneeChanges_doesNotPublish() {
    DefaultIssue issue = existingIssue("issue1", BRANCH_UUID);
    issue.setFieldChange(context, "assignee", null, "some-user-uuid");

    underTest.publish(dbSession, singletonList(issue));

    verify(analyticsEventPublisher, never()).publishAll(any(), any());
  }

  @Test
  void publish_whenNoCurrentChange_doesNotPublish() {
    DefaultIssue issue = existingIssue("issue1", BRANCH_UUID);

    underTest.publish(dbSession, singletonList(issue));

    verify(analyticsEventPublisher, never()).publishAll(any(), any());
  }

  @Test
  void publish_resolvesBranchTypeInASingleBranchDaoCall_regardlessOfIssueCount() {
    givenBranch("branch-a", "project-a", BranchType.BRANCH);
    givenBranch("branch-b", "project-b", BranchType.BRANCH);
    DefaultIssue issueOnBranchA = existingIssue("issue1", "branch-a");
    issueFieldsSetter.setStatus(issueOnBranchA, Issue.STATUS_CONFIRMED, context);
    DefaultIssue issueOnBranchB = existingIssue("issue2", "branch-b");
    issueFieldsSetter.setStatus(issueOnBranchB, Issue.STATUS_CONFIRMED, context);

    underTest.publish(dbSession, List.of(issueOnBranchA, issueOnBranchB));

    verify(branchDao, times(1)).selectByUuids(eq(dbSession), anySet());
  }

  @Test
  void publish_whenSpansTwoBranches_publishesTwoEventsNeverMixingBranches() {
    givenBranch("branch-a", "project-a", BranchType.BRANCH);
    givenBranch("branch-b", "project-b", BranchType.BRANCH);
    DefaultIssue issueOnBranchA = existingIssue("issue1", "branch-a");
    issueFieldsSetter.setStatus(issueOnBranchA, Issue.STATUS_CONFIRMED, context);
    DefaultIssue issueOnBranchB = existingIssue("issue2", "branch-b");
    issueFieldsSetter.setStatus(issueOnBranchB, Issue.STATUS_CONFIRMED, context);

    underTest.publish(dbSession, List.of(issueOnBranchA, issueOnBranchB));

    List<IssueUpdatedBatchEvent> events = capturePublishedEvents();
    assertThat(events).hasSize(2);
    assertThat(events).extracting(IssueUpdatedBatchEvent::branchId).containsExactlyInAnyOrder("branch-a", "branch-b");
    events.forEach(event -> assertThat(event.issues()).hasSize(1));
  }

  @Test
  void publish_whenReopenedTransition_reportsReopenedNotOpen() {
    givenBranch(BRANCH_UUID, PROJECT_UUID, BranchType.BRANCH);
    DefaultIssue issue = existingIssue("issue1", BRANCH_UUID).setStatus(Issue.STATUS_CLOSED);
    issueFieldsSetter.setStatus(issue, Issue.STATUS_REOPENED, context);

    underTest.publish(dbSession, singletonList(issue));

    List<IssueUpdatedBatchEvent> events = capturePublishedEvents();
    assertThat(events.get(0).issues().get(0).issueStatus()).isEqualTo("REOPENED");
  }

  @Test
  void publish_whenBranchIssueAndPullRequestIssueBothTransitionToFalsePositive_onlyPublishesBranchIssue() {
    givenBranch(BRANCH_UUID, PROJECT_UUID, BranchType.BRANCH);
    givenBranch("pr-uuid", PROJECT_UUID, BranchType.PULL_REQUEST);
    DefaultIssue branchIssue = existingIssue("branch-issue", BRANCH_UUID);
    issueFieldsSetter.setResolution(branchIssue, Issue.RESOLUTION_FALSE_POSITIVE, context);
    issueFieldsSetter.setStatus(branchIssue, Issue.STATUS_RESOLVED, context);
    DefaultIssue prIssue = existingIssue("pr-issue", "pr-uuid");
    issueFieldsSetter.setResolution(prIssue, Issue.RESOLUTION_FALSE_POSITIVE, context);
    issueFieldsSetter.setStatus(prIssue, Issue.STATUS_RESOLVED, context);

    underTest.publish(dbSession, List.of(branchIssue, prIssue));

    List<IssueUpdatedBatchEvent> events = capturePublishedEvents();
    assertThat(events).hasSize(1);
    assertThat(events.get(0).branchId()).isEqualTo(BRANCH_UUID);
    assertThat(events.get(0).issues()).extracting(IssueUpdate::issueKey).containsExactly("branch-issue");
  }

  @Test
  void publish_whenPullRequestIssueResolvedAsFixed_isNotPublished() {
    // Even a fixed PR issue is out of scope here: it is re-reported once the PR is merged and the
    // target branch is re-analysed, so reporting it from this hook too would be a duplicate.
    givenBranch("pr-uuid", PROJECT_UUID, BranchType.PULL_REQUEST);
    DefaultIssue prIssue = existingIssue("pr-issue", "pr-uuid");
    issueFieldsSetter.setResolution(prIssue, Issue.RESOLUTION_FIXED, context);
    issueFieldsSetter.setStatus(prIssue, Issue.STATUS_RESOLVED, context);

    underTest.publish(dbSession, singletonList(prIssue));

    verify(analyticsEventPublisher, never()).publishAll(any(), any());
  }

  @Test
  void publish_whenOnlyPullRequestIssueChanges_doesNotPublish() {
    givenBranch("pr-uuid", PROJECT_UUID, BranchType.PULL_REQUEST);
    DefaultIssue prIssue = existingIssue("pr-issue", "pr-uuid");
    issueFieldsSetter.setStatus(prIssue, Issue.STATUS_CONFIRMED, context);

    underTest.publish(dbSession, singletonList(prIssue));

    verify(analyticsEventPublisher, never()).publishAll(any(), any());
  }

  @Test
  void publish_when501IssuesOnOneBranch_publishesEventsOf500And1WithNoDuplicatesOrDrops() {
    givenBranch(BRANCH_UUID, PROJECT_UUID, BranchType.BRANCH);
    List<DefaultIssue> issues = changedIssues(501);

    underTest.publish(dbSession, issues);

    List<IssueUpdatedBatchEvent> events = capturePublishedEvents();
    assertThat(events).extracting(e -> e.issues().size()).containsExactlyInAnyOrder(500, 1);
    List<String> issueKeys = events.stream().flatMap(e -> e.issues().stream()).map(IssueUpdate::issueKey).toList();
    assertThat(issueKeys).hasSize(501);
    assertThat(new HashSet<>(issueKeys)).hasSize(501);
  }

  private List<DefaultIssue> changedIssues(int count) {
    return IntStream.range(0, count)
      .mapToObj(i -> {
        DefaultIssue issue = existingIssue("issue" + i, BRANCH_UUID);
        issueFieldsSetter.setStatus(issue, Issue.STATUS_CONFIRMED, context);
        return issue;
      })
      .toList();
  }

  private void givenBranch(String branchUuid, String projectUuid, BranchType branchType) {
    branches.add(new BranchDto()
      .setUuid(branchUuid)
      .setProjectUuid(projectUuid)
      .setBranchType(branchType)
      .setKey(branchUuid));
  }

  @SuppressWarnings("unchecked")
  private List<IssueUpdatedBatchEvent> capturePublishedEvents() {
    ArgumentCaptor<Collection<IssueUpdatedBatchEvent>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(analyticsEventPublisher).publishAll(eq(IssueUpdatedBatchEvent.TYPE), captor.capture());
    return new ArrayList<>(captor.getValue());
  }

  private static DefaultIssue existingIssue(String key, String branchUuid) {
    return new DefaultIssue()
      .setKey(key)
      .setNew(false)
      .setType(RuleType.CODE_SMELL)
      .setRuleKey(RuleKey.of("java", "S1234"))
      .setProjectUuid(PROJECT_UUID)
      .setComponentUuid("component-" + key)
      .setBranchUuid(branchUuid)
      .setCreationDate(new Date(NOW))
      .setStatus(Issue.STATUS_OPEN)
      .setResolution(null);
  }
}
