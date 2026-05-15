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
package org.sonar.ce.cleaning;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.ce.CeDistributedInformation;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.purge.PurgeProfiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CeActivitiesPurgeSchedulerImplTest {

  private final CeActivitiesPurgeExecutorService executorService = mock(CeActivitiesPurgeExecutorService.class);
  private final CeDistributedInformation ceDistributedInformation = mock(CeDistributedInformation.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final PurgeDao purgeDao = mock(PurgeDao.class);
  private final PurgeProfiler profiler = mock(PurgeProfiler.class);
  private final Lock jobLock = mock(Lock.class);

  private final CeActivitiesPurgeSchedulerImpl underTest =
    new CeActivitiesPurgeSchedulerImpl(executorService, ceDistributedInformation, dbClient, profiler);

  @Test
  public void startScheduling_schedules_purge_with_fixed_delay_of_one_day() {
    underTest.startScheduling();

    ArgumentCaptor<Long> initialDelay = ArgumentCaptor.forClass(Long.class);
    ArgumentCaptor<Long> delay = ArgumentCaptor.forClass(Long.class);
    verify(executorService).scheduleWithFixedDelay(any(Runnable.class), initialDelay.capture(), delay.capture(), eq(TimeUnit.SECONDS));

    assertThat(delay.getValue()).isEqualTo(TimeUnit.DAYS.toSeconds(1));
    assertThat(initialDelay.getValue()).isPositive().isLessThanOrEqualTo(TimeUnit.DAYS.toSeconds(1));
  }

  @Test
  public void purgeCeActivities_executes_purge_when_lock_acquired() {
    when(ceDistributedInformation.acquireCleanJobLock()).thenReturn(jobLock);
    when(jobLock.tryLock()).thenReturn(true);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.purgeDao()).thenReturn(purgeDao);

    runScheduledTask();

    verify(purgeDao).purgeCeActivities(dbSession, profiler);
    verify(purgeDao).purgeCeScannerContexts(dbSession, profiler);
    verify(dbSession).commit();
    verify(jobLock).unlock();
  }

  @Test
  public void purgeCeActivities_does_not_execute_purge_when_lock_not_acquired() {
    when(ceDistributedInformation.acquireCleanJobLock()).thenReturn(jobLock);
    when(jobLock.tryLock()).thenReturn(false);

    runScheduledTask();

    verify(dbClient, never()).openSession(false);
    verify(jobLock, never()).unlock();
  }

  @Test
  public void purgeCeActivities_releases_lock_and_swallows_exception_when_purge_fails() {
    when(ceDistributedInformation.acquireCleanJobLock()).thenReturn(jobLock);
    when(jobLock.tryLock()).thenReturn(true);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.purgeDao()).thenReturn(purgeDao);
    doThrow(new RuntimeException("boom")).when(purgeDao).purgeCeActivities(dbSession, profiler);

    // must not throw — exception is logged, lock is still released
    runScheduledTask();

    verify(jobLock).unlock();
    verify(dbSession, never()).commit();
  }

  @Test
  public void purgeCeActivities_swallows_exception_when_lock_acquisition_fails() {
    doThrow(new RuntimeException("cluster unavailable")).when(ceDistributedInformation).acquireCleanJobLock();

    // must not throw — a transient lock/cluster error must not kill future scheduled executions
    runScheduledTask();

    verify(dbClient, never()).openSession(false);
  }

  private void runScheduledTask() {
    underTest.startScheduling();
    ArgumentCaptor<Runnable> task = ArgumentCaptor.forClass(Runnable.class);
    verify(executorService).scheduleWithFixedDelay(task.capture(), anyLong(), anyLong(), any(TimeUnit.class));
    task.getValue().run();
  }
}