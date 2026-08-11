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
package org.sonar.server.platform.issue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.api.issue.Issue;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.issue.IssueCountByStatusAndResolution;
import org.sonar.db.issue.IssueDao;
import org.sonar.server.property.InternalProperties;
import org.sonar.server.util.GlobalLockManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueCountsByStatusComputationTest {

  private static final String LOCK_NAME = "IssueCountsByStatus";
  private static final String PROPERTY_KEY = "issueCountsByStatus";

  @Mock
  private IssueCountsByStatusComputationExecutorService executorService;
  @Mock
  private DbClient dbClient;
  @Mock
  private DbSession dbSession;
  @Mock
  private IssueDao issueDao;
  @Mock
  private InternalProperties internalProperties;
  @Mock
  private GlobalLockManager lockManager;

  private IssueCountsByStatusComputation underTest;

  @BeforeEach
  void setUp() {
    underTest = new IssueCountsByStatusComputation(executorService, dbClient, internalProperties, lockManager);
  }

  @Test
  void start_schedulesHourlyTask() {
    underTest.start();

    verify(executorService).scheduleAtFixedRate(any(Runnable.class), eq(0L), eq(60 * 60L), eq(TimeUnit.SECONDS));
  }

  @Test
  void stop_doesNothing() {
    underTest.stop();

    verifyNoInteractions(executorService, dbClient, internalProperties, lockManager);
  }

  @Test
  void compute_whenLockAcquired_writesCountsAsJson() {
    when(lockManager.tryLock(LOCK_NAME, 20 * 60)).thenReturn(true);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.issueDao()).thenReturn(issueDao);

    IssueCountByStatusAndResolution open = new IssueCountByStatusAndResolution()
      .setStatus(Issue.STATUS_OPEN).setResolution(null).setCount(50);
    IssueCountByStatusAndResolution fixed = new IssueCountByStatusAndResolution()
      .setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED).setCount(20);
    when(issueDao.countIssuesByStatusOnMainBranches(dbSession)).thenReturn(List.of(open, fixed));

    runScheduledTask();

    ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
    verify(internalProperties).write(eq(PROPERTY_KEY), valueCaptor.capture());
    assertThat(new Gson().<Map<String, Integer>>fromJson(valueCaptor.getValue(), new TypeToken<Map<String, Integer>>() {
    }.getType()))
      .containsExactlyInAnyOrderEntriesOf(Map.of("open", 50, "fixed", 20));
  }

  @Test
  void compute_whenNoIssues_writesEmptyJsonObject() {
    when(lockManager.tryLock(LOCK_NAME, 20 * 60)).thenReturn(true);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.issueDao()).thenReturn(issueDao);
    when(issueDao.countIssuesByStatusOnMainBranches(dbSession)).thenReturn(List.of());

    runScheduledTask();

    verify(internalProperties).write(PROPERTY_KEY, "{}");
  }

  @Test
  void compute_whenLockNotAcquired_doesNothing() {
    when(lockManager.tryLock(LOCK_NAME, 20 * 60)).thenReturn(false);

    runScheduledTask();

    verifyNoInteractions(dbClient);
    verify(internalProperties, never()).write(any(), any());
  }

  @Test
  void compute_whenQueryFails_doesNotPropagate() {
    when(lockManager.tryLock(LOCK_NAME, 20 * 60)).thenReturn(true);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.issueDao()).thenReturn(issueDao);
    when(issueDao.countIssuesByStatusOnMainBranches(dbSession)).thenThrow(new RuntimeException("boom"));

    // must not throw
    runScheduledTask();

    verify(internalProperties, never()).write(any(), any());
  }

  private void runScheduledTask() {
    underTest.start();
    ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
    verify(executorService).scheduleAtFixedRate(taskCaptor.capture(), eq(0L), eq(60 * 60L), eq(TimeUnit.SECONDS));
    taskCaptor.getValue().run();
  }
}
