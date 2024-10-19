/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.pushevent.PushEventDto;
import org.sonar.server.util.GlobalLockManagerImpl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.pushapi.scheduler.purge.PushEventsPurgeScheduler.ENQUEUE_DELAY_IN_SECONDS;
import static org.sonar.server.pushapi.scheduler.purge.PushEventsPurgeScheduler.INITIAL_DELAY_IN_SECONDS;

public class PushEventsPurgeSchedulerAndExecutorIT {

  private static final String PUSH_EVENT_UUID = "push_event_uuid";
  private static final long INITIAL_AND_ENQUE_DELAY_MS = 1L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  private PushEventsPurgeScheduler pushEventsPurgeScheduler;

  @Before
  public void setup() {
    Configuration configuration = mock(Configuration.class);
    when(configuration.getLong(INITIAL_DELAY_IN_SECONDS)).thenReturn(Optional.of(INITIAL_AND_ENQUE_DELAY_MS));
    when(configuration.getLong(ENQUEUE_DELAY_IN_SECONDS)).thenReturn(Optional.of(INITIAL_AND_ENQUE_DELAY_MS));

    GlobalLockManagerImpl lockManager = mock(GlobalLockManagerImpl.class);
    when(lockManager.tryLock(any(), anyInt())).thenReturn(true);

    DbClient dbClient = dbTester.getDbClient();
    pushEventsPurgeScheduler = new PushEventsPurgeScheduler(
      dbClient,
      configuration,
      lockManager,
      new PushEventsPurgeExecutorServiceImpl(),
      System2.INSTANCE
    );
  }

  @Test
  public void pushEventsPurgeScheduler_shouldCleanUpPeriodically() throws InterruptedException {
    insertOldPushEvent();
    assertThat(pushEventIsCleanedUp()).isFalse();

    pushEventsPurgeScheduler.start();

    await()
      .atMost(10, TimeUnit.SECONDS)
      .pollDelay(500, TimeUnit.MILLISECONDS)
      .until(this::pushEventIsCleanedUp);
  }

  private void insertOldPushEvent() {
    PushEventDto pushEventDto = new PushEventDto();
    pushEventDto.setUuid(PUSH_EVENT_UUID);
    pushEventDto.setName("test_event");
    pushEventDto.setProjectUuid("test_project_uuid");
    pushEventDto.setPayload("payload".getBytes(StandardCharsets.UTF_8));
    pushEventDto.setCreatedAt(1656633600);
    dbTester.getDbClient().pushEventDao().insert(dbTester.getSession(), pushEventDto);
    dbTester.commit();
  }

  private boolean pushEventIsCleanedUp() {
    return dbTester.getDbClient().pushEventDao().selectByUuid(dbTester.getSession(), PUSH_EVENT_UUID) == null;
  }

}
