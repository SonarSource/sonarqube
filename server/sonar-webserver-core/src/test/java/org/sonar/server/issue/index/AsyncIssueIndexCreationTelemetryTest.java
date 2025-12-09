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
package org.sonar.server.issue.index;

import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.ce.CeQueueDao;
import org.sonar.telemetry.core.TelemetryClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

class AsyncIssueIndexCreationTelemetryTest {


  private final IssueIndexSyncProgressChecker issueIndexSyncProgressChecker = mock();
  private final DbClient dbClient = mock(Answers.RETURNS_DEEP_STUBS);
  private final TelemetryClient telemetryClient = mock();
  private final Server server = mock();
  private final Clock clock = mock();
  private final UuidFactory uuidFactory = new SequenceUuidFactory();
  private final ScheduledFuture mockFuture = mock();
  private final IssueIndexMonitoringScheduler scheduler = mock();
  private final ArgumentCaptor<Runnable> telemetrySyncRunnable = ArgumentCaptor.forClass(Runnable.class);
  private final Configuration configuration = mock();
  private final AsyncIssueIndexCreationTelemetry underTest = new AsyncIssueIndexCreationTelemetry(
    dbClient,
    telemetryClient,
    server,
    uuidFactory,
    clock,
    scheduler,
    configuration
  );

  @BeforeEach
  void before() {
    when(issueIndexSyncProgressChecker.isIssueSyncInProgress(any())).thenReturn(false);
    when(dbClient.ceActivityDao().countFailedOrCancelledIssueSyncTasks(any())).thenReturn(5);
    when(server.getId()).thenReturn("server-id");
    when(clock.millis()).thenReturn(1000L, 2000L, 3000L, 4000L, 5000L);
    when(scheduler.scheduleAtFixedRate(telemetrySyncRunnable.capture(), anyLong(), anyLong(), any())).thenReturn(mockFuture);
    when(configuration.getBoolean(SONAR_TELEMETRY_ENABLE.getKey())).thenReturn(Optional.of(true));
  }

  @Test
  void whenTelemetryDisabled_thenDoNothing() {
    reset(configuration);
    when(configuration.getBoolean(SONAR_TELEMETRY_ENABLE.getKey())).thenReturn(Optional.of(false));

    underTest.startIndexCreationMonitoringToSendTelemetry(100);

    verify(scheduler, never()).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());
  }

  @Test
  void whenSynchroIsNotFinished_thenRetry() throws IOException {
    underTest.startIndexCreationMonitoringToSendTelemetry(100);

    verify(scheduler).scheduleAtFixedRate(any(), anyLong(), anyLong(), any());

    Runnable telemetrySyncTry = telemetrySyncRunnable.getValue();

    telemetrySyncTry.run();

    verify(mockFuture).cancel(anyBoolean());
    ArgumentCaptor<String> jsonMessagePayloadCaptor = ArgumentCaptor.forClass(String.class);
    verify(telemetryClient).uploadMetric(jsonMessagePayloadCaptor.capture());
    String jsonMessage = jsonMessagePayloadCaptor.getValue();
    assertThat(jsonMessage).contains(AsyncIssueIndexCreationTelemetry.KEY_ASYNC_ISSUE_INDEXING_DURATION)
      .contains(AsyncIssueIndexCreationTelemetry.KEY_ASYNC_ISSUE_INDEXING_TASK_TOTAL_COUNT)
      .contains(AsyncIssueIndexCreationTelemetry.KEY_ASYNC_ISSUE_INDEXING_TASK_FAILURE_COUNT);
  }

  @Test
  void tryToSendTelemetry_whenNoPendingTasks_thenSendTelemetry() throws IOException, IllegalAccessException {
    CeQueueDao ceQueueDao = mock();
    when(ceQueueDao.hasAnyIssueSyncTaskPendingOrInProgress(any())).thenReturn(false);
    when(dbClient.ceQueueDao()).thenReturn(ceQueueDao);

    Field field = ReflectionUtils.findFields(underTest.getClass(), f -> f.getName().equals("currentMonitoring"), ReflectionUtils.HierarchyTraversalMode.TOP_DOWN).get(0);
    field.setAccessible(true);
    field.set(underTest, mock(ScheduledFuture.class));

    underTest.tryToSendTelemetry();

    verify(telemetryClient).uploadMetric(any());
  }
}
