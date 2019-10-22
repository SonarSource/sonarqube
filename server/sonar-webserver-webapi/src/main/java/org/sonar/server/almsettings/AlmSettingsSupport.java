/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

package org.sonar.server.almsettings;

import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmSettings;

import static java.lang.String.format;
import static org.sonar.api.web.UserRole.ADMIN;

class AlmSettingsSupport {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public AlmSettingsSupport(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  void checkAlmSettingDoesNotAlreadyExist(DbSession dbSession, String almSetting) {
    dbClient.almSettingDao().selectByKey(dbSession, almSetting)
      .ifPresent(a -> {
        throw new IllegalArgumentException(format("An ALM setting with key '%s' already exists", a.getKey()));
      });
  }

  ComponentDto getProject(DbSession dbSession, String projectKey) {
    ComponentDto project = componentFinder.getByKey(dbSession, projectKey);
    userSession.checkComponentPermission(ADMIN, project);
    return project;
  }

  AlmSettingDto getAlmSetting(DbSession dbSession, String almSetting) {
    return dbClient.almSettingDao().selectByKey(dbSession, almSetting)
      .orElseThrow(() -> new NotFoundException(format("ALM setting with key '%s' cannot be found", almSetting)));
  }

  static AlmSettings.Alm toAlmWs(ALM alm) {
    switch (alm) {
      case GITHUB:
        return AlmSettings.Alm.github;
      case BITBUCKET:
        return AlmSettings.Alm.bitbucket;
      case AZURE_DEVOPS:
        return AlmSettings.Alm.azure;
      default:
        throw new IllegalStateException(format("Unknown ALM '%s'", alm.name()));
    }
  }
}
