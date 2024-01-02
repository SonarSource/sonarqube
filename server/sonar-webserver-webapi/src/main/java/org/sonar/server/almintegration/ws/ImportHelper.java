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

import java.util.List;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.api.server.ws.Request;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.project.ProjectDto;
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

  public AlmSettingDto getAlmSettingDto(Request request) {
    return getAlmSettingDto(request, null);
  }

  public AlmSettingDto getAlmSettingDtoForAlm(Request request, ALM alm) {
    return getAlmSettingDto(request, alm);
  }

  private AlmSettingDto getAlmSettingDto(Request request, @Nullable ALM alm) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String almSettingKey = request.param(PARAM_ALM_SETTING);
      if (almSettingKey != null) {
        return getAlmSettingDtoFromKey(dbSession, almSettingKey);
      }
      return getUniqueAlmSettingDtoForAlm(dbSession, alm);
    }
  }

  private AlmSettingDto getAlmSettingDtoFromKey(DbSession dbSession, String almSettingKey) {
    return dbClient.almSettingDao().selectByKey(dbSession, almSettingKey)
      .orElseThrow(() -> new NotFoundException(String.format("DevOps Platform configuration '%s' not found.", almSettingKey)));
  }

  private AlmSettingDto getUniqueAlmSettingDtoForAlm(DbSession dbSession, @Nullable ALM alm) {
    List<AlmSettingDto> almSettingDtos = getAlmSettingDtos(dbSession, alm);

    if (almSettingDtos.isEmpty()) {
      String almString = alm == null ? "" : (alm.name() + " ");
      throw new NotFoundException("There is no " + almString + "configuration for DevOps Platform. Please add one.");
    }
    if (almSettingDtos.size() == 1) {
      return almSettingDtos.get(0);
    }
    throw new IllegalArgumentException(String.format("Parameter %s is required as there are multiple DevOps Platform configurations.", PARAM_ALM_SETTING));
  }

  private List<AlmSettingDto> getAlmSettingDtos(DbSession dbSession, @Nullable ALM alm) {
    if (alm == null) {
      return dbClient.almSettingDao().selectAll(dbSession);
    }
    return dbClient.almSettingDao().selectByAlm(dbSession, alm);
  }

  public String getUserUuid() {
    return requireNonNull(userSession.getUuid(), "User UUID cannot be null.");
  }

  public static CreateWsResponse toCreateResponse(ProjectDto projectDto) {
    return newBuilder()
      .setProject(Project.newBuilder()
        .setKey(projectDto.getKey())
        .setName(projectDto.getName())
        .setQualifier(projectDto.getQualifier())
        .setVisibility(Visibility.getLabel(projectDto.isPrivate())))
      .build();
  }
}
