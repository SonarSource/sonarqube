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
package org.sonar.server.v2.api.mode.controller;

import org.sonar.api.config.Configuration;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.issue.notification.QualityGateMetricsUpdateNotification;
import org.sonar.server.notification.NotificationManager;
import org.sonar.server.qualitygate.QualityGateConditionsValidator;
import org.sonar.server.setting.SettingsChangeNotifier;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.mode.enums.ModeEnum;
import org.sonar.server.v2.api.mode.resources.ModeResource;

import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_DEFAULT_VALUE;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_ENABLED;
import static org.sonar.core.config.MQRModeConstants.MULTI_QUALITY_MODE_MODIFIED;

public class DefaultModeController implements ModeController {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final Configuration configuration;
  private final SettingsChangeNotifier settingsChangeNotifier;
  private final NotificationManager notificationManager;
  private final QualityGateConditionsValidator qualityGateConditionsValidator;

  public DefaultModeController(UserSession userSession, DbClient dbClient, Configuration configuration,
    SettingsChangeNotifier settingsChangeNotifier, NotificationManager notificationManager, QualityGateConditionsValidator qualityGateConditionsValidator) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.configuration = configuration;
    this.settingsChangeNotifier = settingsChangeNotifier;
    this.notificationManager = notificationManager;
    this.qualityGateConditionsValidator = qualityGateConditionsValidator;
  }

  @Override
  public ModeResource getMode() {
    ModeEnum mode = configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED).orElse(MULTI_QUALITY_MODE_DEFAULT_VALUE) ? ModeEnum.MQR : ModeEnum.STANDARD_EXPERIENCE;
    try (DbSession dbSession = dbClient.openSession(false)) {
      return new ModeResource(mode, isModeModified(dbSession));
    }
  }

  private boolean isModeModified(DbSession dbSession) {
    return dbClient.internalPropertiesDao().selectByKey(dbSession, MULTI_QUALITY_MODE_MODIFIED)
      .map(Boolean::parseBoolean)
      .orElse(false);
  }

  @Override
  public ModeResource patchMode(ModeResource modeResource) {
    userSession.checkIsSystemAdministrator();
    Boolean isMQREnabledRequest = modeResource.mode().equals(ModeEnum.MQR);
    Boolean isMqREnabled = configuration.getBoolean(MULTI_QUALITY_MODE_ENABLED).orElse(MULTI_QUALITY_MODE_DEFAULT_VALUE);

    try (DbSession dbSession = dbClient.openSession(false)) {
      if (!isMQREnabledRequest.equals(isMqREnabled)) {

        dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto().setKey(MULTI_QUALITY_MODE_ENABLED).setValue(isMQREnabledRequest.toString()));
        dbClient.internalPropertiesDao().save(dbSession, MULTI_QUALITY_MODE_MODIFIED, "true");
        dbSession.commit();

        settingsChangeNotifier.onGlobalPropertyChange(MULTI_QUALITY_MODE_ENABLED, isMQREnabledRequest.toString());
        if (qualityGateConditionsValidator.hasConditionsMismatch(isMQREnabledRequest)) {
          notificationManager.scheduleForSending(new QualityGateMetricsUpdateNotification(isMQREnabledRequest));
        }
      }

      return new ModeResource(Boolean.TRUE.equals(isMQREnabledRequest) ? ModeEnum.MQR : ModeEnum.STANDARD_EXPERIENCE, isModeModified(dbSession));
    }
  }
}
