/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.pushapi.scheduler.purge;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.pushevent.PushEventDao;
import org.sonar.server.util.AbstractStoppableExecutorService;
import org.sonar.server.util.GlobalLockManager;
import org.sonar.server.util.GlobalLockManagerImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PushEventsPurgeSchedulerTest {
  private final DbClient dbClient = mock(DbClient.class);
  private final DbSession dbSession = mock(DbSession.class);
  private final GlobalLockManager lockManager = mock(GlobalLockManagerImpl.class);
  private final PushEventDao pushEventDao = mock(PushEventDao.class);
  private final PushEventsPurgeExecutorServiceImpl executorService = new PushEventsPurgeExecutorServiceImpl();
  private final Configuration configuration = mock(Configuration.class);
  private final System2 system2 = mock(System2.class);
  private final PushEventsPurgeScheduler underTest = new PushEventsPurgeScheduler(dbClient, configuration,
    lockManager, executorService, system2);

  @Before
  public void prepare() {
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);
  }

  @Test
  public void doNothingIfLocked() {
    when(lockManager.tryLock(any(), anyInt())).thenReturn(false);

    underTest.start();

    executorService.runCommand();

    verifyNoInteractions(dbClient);
  }

  @Test
  public void doNothingIfExceptionIsThrown() {
    when(lockManager.tryLock(any(), anyInt())).thenThrow(new IllegalArgumentException("Oops"));

    underTest.start();

    executorService.runCommand();

    verifyNoInteractions(dbClient);
  }

  @Test
  public void schedulePurgeTaskWhenNotLocked() {
    when(system2.now()).thenReturn(100000000L);
    when(dbClient.pushEventDao()).thenReturn(pushEventDao);
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.pushEventDao().selectUuidsOfExpiredEvents(any(), anyLong())).thenReturn(Set.of("1", "2"));

    underTest.start();

    executorService.runCommand();

    ArgumentCaptor<Set> uuidsCaptor = ArgumentCaptor.forClass(Set.class);

    verify(pushEventDao).deleteByUuids(any(), uuidsCaptor.capture());
    Set<String> uuids = uuidsCaptor.getValue();
    assertThat(uuids).containsExactlyInAnyOrder("1", "2");
  }

  private static class PushEventsPurgeExecutorServiceImpl extends AbstractStoppableExecutorService<ScheduledExecutorService>
    implements PushEventsPurgeExecutorService {

    private Runnable command;

    public PushEventsPurgeExecutorServiceImpl() {
      super(null);
    }

    public void runCommand() {
      command.run();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
      this.command = command;
      return null;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      return null;
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
      return null;
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
      return null;
    }

  }
}
