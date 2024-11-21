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

import org.sonar.api.Startable;
import org.sonar.api.config.Configuration;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.platform.db.migration.history.MigrationHistory;
import org.sonar.server.qualitygate.QualityGateConditionsValidator;

import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_DEFAULT_VALUE;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;

public class NewModesNotificationsSender implements Startable {

  public static final int NEW_MODES_SQ_VERSION = 108_000;
  private final NotificationManager notificationManager;
  private final Configuration configuration;
  private final MigrationHistory migrationHistory;
  private final QualityGateConditionsValidator qualityGateConditionsValidator;

  public NewModesNotificationsSender(NotificationManager notificationManager, Configuration configuration, MigrationHistory migrationHistory,
    QualityGateConditionsValidator qualityGateConditionsValidator) {
    this.notificationManager = notificationManager;
    this.configuration = configuration;
    this.migrationHistory = migrationHistory;
    this.qualityGateConditionsValidator = qualityGateConditionsValidator;
  }

  @Override
  public void start() {
    if (migrationHistory.getInitialDbVersion() != -1 && migrationHistory.getInitialDbVersion() < NEW_MODES_SQ_VERSION) {
      boolean isMQRModeEnabled = configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED).orElse(MULTI_QUALITY_MODE_DEFAULT_VALUE);
      notificationManager.scheduleForSending(new MQRAndStandardModesExistNotification(isMQRModeEnabled));
      if (qualityGateConditionsValidator.hasConditionsMismatch(isMQRModeEnabled)) {
        notificationManager.scheduleForSending(new QualityGateMetricsUpdateNotification(isMQRModeEnabled));
      }
    }
  }

  @Override
  public void stop() {
    // Nothing to do
  }
}
