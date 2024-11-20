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
package org.sonar.server.issue.notification;

import java.util.Map;
import java.util.stream.Collectors;
import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.metric.StandardToMQRMetrics;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.platform.db.migration.history.MigrationHistory;

import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_DEFAULT_VALUE;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;

public class NewModesNotificationsSender implements Startable {

  public static final int NEW_MODES_SQ_VERSION = 108_000;
  private final NotificationManager notificationManager;
  private final Configuration configuration;
  private final MigrationHistory migrationHistory;
  private final DbClient dbClient;

  public NewModesNotificationsSender(NotificationManager notificationManager, Configuration configuration, MigrationHistory migrationHistory, DbClient dbClient) {
    this.notificationManager = notificationManager;
    this.configuration = configuration;
    this.migrationHistory = migrationHistory;
    this.dbClient = dbClient;
  }

  @Override
  public void start() {
    if (migrationHistory.getInitialDbVersion() != -1 && migrationHistory.getInitialDbVersion() < NEW_MODES_SQ_VERSION) {
      boolean isMQRModeEnabled = configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED).orElse(MULTI_QUALITY_MODE_DEFAULT_VALUE);
      sendNewModesNotification(isMQRModeEnabled);
      sendQualityGateMetricsUpdateNotification(isMQRModeEnabled);
    }
  }

  private void sendQualityGateMetricsUpdateNotification(boolean isMQRModeEnabled) {
    try (DbSession dbSession = dbClient.openSession(false)) {

      Map<String, String> metricKeysByUuids = dbClient.metricDao().selectAll(dbSession).stream()
        .collect(Collectors.toMap(MetricDto::getUuid, MetricDto::getKey));

      boolean hasConditionsFromOtherMode = dbClient.gateConditionDao().selectAll(dbSession).stream()
        .anyMatch(c -> isMQRModeEnabled ? StandardToMQRMetrics.isStandardMetric(metricKeysByUuids.get(c.getMetricUuid()))
          : StandardToMQRMetrics.isMQRMetric(metricKeysByUuids.get(c.getMetricUuid())));

      if (hasConditionsFromOtherMode) {
        notificationManager.scheduleForSending(new QualityGateMetricsUpdateNotification(isMQRModeEnabled));
      }
    }
  }

  private void sendNewModesNotification(boolean isMQRModeEnabled) {
    notificationManager.scheduleForSending(new MQRAndStandardModesExistNotification(isMQRModeEnabled));
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}
