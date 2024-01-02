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
package org.sonar.server.almintegration.ws;

import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.Request;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Projects.CreateWsResponse.Project;

import static java.util.Objects.requireNonNull;
import static org.sonar.db.permission.GlobalPermission.PROVISION_PROJECTS;
import static org.sonarqube.ws.Projects.CreateWsResponse;
import static org.sonarqube.ws.Projects.CreateWsResponse.newBuilder;

@ServerSide
public class ImportHelper {
  public static final String PARAM_ALM_SETTING = "almSetting";

  private final DbClient dbClient;
  private final UserSession userSession;

  public ImportHelper(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  public void checkProvisionProjectPermission() {
    userSession.checkLoggedIn().checkPermission(PROVISION_PROJECTS);
  }

  public AlmSettingDto getAlmSetting(Request request) {
    String almSettingKey = request.mandatoryParam(PARAM_ALM_SETTING);
    try (DbSession dbSession = dbClient.openSession(false)) {
      return dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
        .orElseThrow(() -> new NotFoundException(String.format("DevOps Platform Setting '%s' not found", almSettingKey)));
    }
  }

  public String getUserUuid() {
    return requireNonNull(userSession.getUuid(), "User UUID cannot be null");
  }

  public static CreateWsResponse toCreateResponse(ComponentDto componentDto) {
    return newBuilder()
      .setProject(Project.newBuilder()
        .setKey(componentDto.getKey())
        .setName(componentDto.name())
        .setQualifier(componentDto.qualifier())
        .setVisibility(Visibility.getLabel(componentDto.isPrivate())))
      .build();
  }
}
