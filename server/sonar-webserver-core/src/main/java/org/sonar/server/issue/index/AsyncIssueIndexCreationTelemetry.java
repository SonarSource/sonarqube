/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.time.Clock;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;
import org.sonar.api.platform.Server;
import org.sonar.api.server.ServerSide;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.telemetry.core.Dimension;
import org.sonar.telemetry.core.Granularity;
import org.sonar.telemetry.core.MessageSerializer;
import org.sonar.telemetry.core.TelemetryClient;
import org.sonar.telemetry.core.TelemetryDataType;
import org.sonar.telemetry.core.schema.BaseMessage;
import org.sonar.telemetry.core.schema.InstallationMetric;

import static org.sonar.process.ProcessProperties.Property.SONAR_TELEMETRY_ENABLE;

@ServerSide
public class AsyncIssueIndexCreationTelemetry {
  public static final String KEY_ASYNC_ISSUE_INDEXING_DURATION = "async_issue_indexing_duration";
  public static final String KEY_ASYNC_ISSUE_INDEXING_TASK_TOTAL_COUNT = "async_issue_indexing_task_total_count";
  public static final String KEY_ASYNC_ISSUE_INDEXING_TASK_FAILURE_COUNT = "async_issue_indexing_task_failure_count";

  private static final Logger LOGGER = LoggerFactory.getLogger(AsyncIssueIndexCreationTelemetry.class);

  private final DbClient dbClient;
  private final TelemetryClient telemetryClient;
  private final Server server;
  private final UuidFactory uuidFactory;
  private final Clock clock;
  private final IssueIndexMonitoringScheduler scheduler;
  private final Configuration config;

  private ScheduledFuture<?> currentMonitoring;
  private long startTime;
  private int nbIndexingTasks;

  public AsyncIssueIndexCreationTelemetry(DbClient dbClient, TelemetryClient telemetryClient, Server server, UuidFactory uuidFactory, Clock clock,
    IssueIndexMonitoringScheduler scheduler, Configuration config) {
    this.dbClient = dbClient;
    this.telemetryClient = telemetryClient;
    this.server = server;
    this.uuidFactory = uuidFactory;
    this.clock = clock;
    this.scheduler = scheduler;
    this.config = config;
  }

  /**
   * Initiates the monitoring of indexing progress using the IssueIndexSyncProgressChecker
   * and sends telemetry once the indexing process is completed.
   * <p>
   */
  public void startIndexCreationMonitoringToSendTelemetry(int nbIndexingTasks) {
    if (!config.getBoolean(SONAR_TELEMETRY_ENABLE.getKey()).orElse(false)) {
      return;
    }
    startTime = clock.millis();
    this.nbIndexingTasks = nbIndexingTasks;
    if (currentMonitoring != null) {
      currentMonitoring.cancel(false);
    }
    currentMonitoring = scheduler.scheduleAtFixedRate(this::tryToSendTelemetry, 0, 5, TimeUnit.SECONDS);

  }

  @VisibleForTesting
  void tryToSendTelemetry() {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (!dbClient.ceQueueDao().hasAnyIssueSyncTaskPendingOrInProgress(dbSession)) {
        sendIssueIndexationTelemetry(dbSession);
        currentMonitoring.cancel(false);
      }
    }
  }

  private void sendIssueIndexationTelemetry(DbSession dbSession) {
    int nbBranchNotIndexed = dbClient.ceActivityDao().countFailedOrCancelledIssueSyncTasks(dbSession);

    BaseMessage baseMessage = new BaseMessage.Builder()
      .setInstallationId(server.getId())
      .setDimension(Dimension.INSTALLATION)
      .setMessageUuid(uuidFactory.create())
      .setMetrics(Set.of(
        new InstallationMetric(KEY_ASYNC_ISSUE_INDEXING_DURATION, clock.millis() - startTime, TelemetryDataType.INTEGER, Granularity.ADHOC),
        new InstallationMetric(KEY_ASYNC_ISSUE_INDEXING_TASK_TOTAL_COUNT, nbIndexingTasks, TelemetryDataType.INTEGER, Granularity.ADHOC),
        new InstallationMetric(KEY_ASYNC_ISSUE_INDEXING_TASK_FAILURE_COUNT, nbBranchNotIndexed, TelemetryDataType.INTEGER, Granularity.ADHOC)
      )).build();
    String asyncIssueSyncJsonMessage = MessageSerializer.serialize(baseMessage);
    try {
      telemetryClient.uploadMetric(asyncIssueSyncJsonMessage);
    } catch (IOException e) {
      LOGGER.debug("Failed to upload telemetry data", e);
    }
  }
}
