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
package org.sonar.server.history;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.issue.IssueDao;
import org.sonar.server.qualitygate.changeevent.QGChangeEvent;
import org.sonar.server.qualitygate.changeevent.QGChangeEventListener.ChangedIssue;
import org.sonarsource.history.server.service.IssueCountHistoryRecordingService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IssueCountHistoryQGChangeEventListenerTest {
  private static final String BRANCH = "branch";
  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession session = mock(DbSession.class);
  private final BranchDao branchDao = mock(BranchDao.class);
  private final IssueDao issueDao = mock(IssueDao.class);
  private final IssueCountHistoryRecordingService recorder = mock(IssueCountHistoryRecordingService.class);
  private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
  private final QGChangeEvent event = mock(QGChangeEvent.class);
  private final ChangedIssue issue = mock(ChangedIssue.class);
  private final IssueCountHistoryQGChangeEventListener underTest = new IssueCountHistoryQGChangeEventListener(
    dbClient, recorder, System2.INSTANCE, tasks::add);

  @BeforeEach
  void setUp() {
    when(event.getBranch()).thenReturn(new BranchDto().setUuid(BRANCH).setBranchType(BranchType.BRANCH));
    when(dbClient.openSession(false)).thenReturn(session);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.issueDao()).thenReturn(issueDao);
    when(branchDao.lockForIssueCountHistory(session, BRANCH)).thenReturn(true);
    when(issueDao.selectIssueCountDimensionsForBranches(session, List.of(BRANCH))).thenReturn(List.of());
  }

  @Test
  void coalesces_pending_events_without_reading_on_caller_thread() {
    notifyChange();
    notifyChange();
    verifyNoInteractions(dbClient, recorder);
    assertThat(tasks).hasSize(1);

    tasks.remove().run();

    var order = inOrder(branchDao, issueDao, recorder, session);
    order.verify(branchDao).lockForIssueCountHistory(session, BRANCH);
    order.verify(issueDao).selectIssueCountDimensionsForBranches(session, List.of(BRANCH));
    order.verify(recorder).recordIssueHistoryForBranch(eq(BRANCH), any(), any());
    order.verify(session).close();
    notifyChange();
    assertThat(tasks).hasSize(1);
  }

  @Test
  void change_during_recording_is_requeued() {
    doAnswer(invocation -> {
      notifyChange();
      notifyChange();
      return null;
    }).doNothing().when(recorder).recordIssueHistoryForBranch(eq(BRANCH), any(), any());
    notifyChange();

    tasks.remove().run();

    verify(recorder).recordIssueHistoryForBranch(eq(BRANCH), any(), any());
    assertThat(tasks).hasSize(1);

    tasks.remove().run();

    verify(recorder, times(2)).recordIssueHistoryForBranch(eq(BRANCH), any(), any());
    assertThat(tasks).isEmpty();
  }

  @Test
  void different_branches_are_scheduled_independently() {
    notifyChange();
    QGChangeEvent otherEvent = mock(QGChangeEvent.class);
    when(otherEvent.getBranch()).thenReturn(new BranchDto().setUuid("other").setBranchType(BranchType.BRANCH));

    underTest.onIssueChanges(otherEvent, Set.of(issue));

    assertThat(tasks).hasSize(2);
  }

  @Test
  void ignores_events_without_changed_issues() {
    underTest.onIssueChanges(event, Set.of());

    assertThat(tasks).isEmpty();
  }

  @Test
  void ignores_pull_requests() {
    when(event.getBranch()).thenReturn(new BranchDto().setUuid(BRANCH).setBranchType(BranchType.PULL_REQUEST));

    notifyChange();

    assertThat(tasks).isEmpty();
  }

  @Test
  void failure_does_not_prevent_later_events_from_refreshing() {
    doThrow(new IllegalStateException("write failed")).doNothing()
      .when(recorder).recordIssueHistoryForBranch(eq(BRANCH), any(), any());
    notifyChange();
    tasks.remove().run();
    notifyChange();
    tasks.remove().run();

    verify(recorder, times(2)).recordIssueHistoryForBranch(eq(BRANCH), any(), any());
    verify(session, times(2)).close();
  }

  @Test
  void error_during_recording_does_not_drop_pending_refresh() {
    doAnswer(invocation -> {
      notifyChange();
      throw new AssertionError("write failed");
    }).doNothing()
      .when(recorder).recordIssueHistoryForBranch(eq(BRANCH), any(), any());
    notifyChange();
    Runnable task = tasks.remove();
    assertThatThrownBy(task::run).isInstanceOf(AssertionError.class);
    assertThat(tasks).hasSize(1);

    tasks.remove().run();

    verify(recorder, times(2)).recordIssueHistoryForBranch(eq(BRANCH), any(), any());
    verify(session, times(2)).close();
  }

  @Test
  void requeues_follow_up_refresh_after_other_branches() {
    QGChangeEvent otherEvent = mock(QGChangeEvent.class);
    when(otherEvent.getBranch()).thenReturn(new BranchDto().setUuid("other").setBranchType(BranchType.BRANCH));
    doAnswer(invocation -> {
      notifyChange();
      return null;
    }).doNothing().when(recorder).recordIssueHistoryForBranch(eq(BRANCH), any(), any());

    notifyChange();
    underTest.onIssueChanges(otherEvent, Set.of(issue));

    tasks.remove().run();

    assertThat(tasks).hasSize(2);
    verify(recorder, times(1)).recordIssueHistoryForBranch(eq(BRANCH), any(), any());

    tasks.remove().run();
    verify(recorder, times(1)).recordIssueHistoryForBranch(eq(BRANCH), any(), any());

    tasks.remove().run();
    verify(recorder, times(2)).recordIssueHistoryForBranch(eq(BRANCH), any(), any());
    assertThat(tasks).isEmpty();
  }

  @Test
  void rejected_submission_does_not_leave_branch_pending() {
    IssueCountHistoryExecutor executor = mock(IssueCountHistoryExecutor.class);
    doThrow(new RejectedExecutionException()).doAnswer(invocation -> {
      tasks.add(invocation.getArgument(0));
      return null;
    }).when(executor).execute(any());
    var listener = new IssueCountHistoryQGChangeEventListener(dbClient, recorder, System2.INSTANCE, executor);
    Set<ChangedIssue> changedIssues = Set.of(issue);
    assertThatThrownBy(() -> listener.onIssueChanges(event, changedIssues)).isInstanceOf(RejectedExecutionException.class);

    listener.onIssueChanges(event, Set.of(issue));

    assertThat(tasks).hasSize(1);
  }

  @Test
  void rejected_follow_up_submission_does_not_leave_branch_pending() {
    IssueCountHistoryExecutor executor = mock(IssueCountHistoryExecutor.class);
    doAnswer(invocation -> {
      tasks.add(invocation.getArgument(0));
      return null;
    }).doThrow(new RejectedExecutionException()).doAnswer(invocation -> {
      tasks.add(invocation.getArgument(0));
      return null;
    }).when(executor).execute(any());
    var listener = new IssueCountHistoryQGChangeEventListener(dbClient, recorder, System2.INSTANCE, executor);
    doAnswer(invocation -> {
      listener.onIssueChanges(event, Set.of(issue));
      return null;
    }).when(recorder).recordIssueHistoryForBranch(eq(BRANCH), any(), any());

    listener.onIssueChanges(event, Set.of(issue));
    tasks.remove().run();

    listener.onIssueChanges(event, Set.of(issue));

    assertThat(tasks).hasSize(1);
  }

  @Test
  void skips_branch_deleted_before_execution() {
    when(branchDao.lockForIssueCountHistory(session, BRANCH)).thenReturn(false);
    notifyChange();
    tasks.remove().run();

    verifyNoInteractions(issueDao, recorder);
    verify(session).close();
  }

  private void notifyChange() {
    underTest.onIssueChanges(event, Set.of(issue));
  }
}
