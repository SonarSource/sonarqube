/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.almsettings.ws;

import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.almsettings.MultipleAlmFeatureProvider;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.AlmSettings;

import static java.lang.String.format;
import static org.sonar.api.web.UserRole.ADMIN;

@ServerSide
public class AlmSettingsSupport {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;
  private final MultipleAlmFeatureProvider multipleAlmFeatureProvider;

  public AlmSettingsSupport(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder,
    MultipleAlmFeatureProvider multipleAlmFeatureProvider) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
    this.multipleAlmFeatureProvider = multipleAlmFeatureProvider;
  }

  public MultipleAlmFeatureProvider getMultipleAlmFeatureProvider() {
    return multipleAlmFeatureProvider;
  }

  public void checkAlmSettingDoesNotAlreadyExist(DbSession dbSession, String almSetting) {
    dbClient.almSettingDao().selectByKey(dbSession, almSetting)
      .ifPresent(a -> {
        throw new IllegalArgumentException(format("An ALM setting with key '%s' already exists", a.getKey()));
      });
  }

  public void checkAlmMultipleFeatureEnabled(ALM alm) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (!multipleAlmFeatureProvider.enabled() && !dbClient.almSettingDao().selectByAlm(dbSession, alm).isEmpty()) {
        throw BadRequestException.create("A " + alm + " setting is already defined");
      }
    }
  }

  public ProjectDto getProjectAsAdmin(DbSession dbSession, String projectKey) {
    return getProject(dbSession, projectKey, ADMIN);
  }

  public ProjectDto getProject(DbSession dbSession, String projectKey, String projectPermission) {
    ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
    userSession.checkProjectPermission(projectPermission, project);
    return project;
  }

  public AlmSettingDto getAlmSetting(DbSession dbSession, String almSetting) {
    return dbClient.almSettingDao().selectByKey(dbSession, almSetting)
      .orElseThrow(() -> new NotFoundException(format("ALM setting with key '%s' cannot be found", almSetting)));
  }

  public static AlmSettings.Alm toAlmWs(ALM alm) {
    switch (alm) {
      case GITHUB:
        return AlmSettings.Alm.github;
      case BITBUCKET:
        return AlmSettings.Alm.bitbucket;
      case BITBUCKET_CLOUD:
        return AlmSettings.Alm.bitbucketcloud;
      case AZURE_DEVOPS:
        return AlmSettings.Alm.azure;
      case GITLAB:
        return AlmSettings.Alm.gitlab;
      default:
        throw new IllegalStateException(format("Unknown ALM '%s'", alm.name()));
    }
  }
}
