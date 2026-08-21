/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.v2.api.dop.controller;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.alm.setting.ALM;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.alm.setting.ProjectAlmSettingDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.common.almsettings.permission.DopPermissionCheck;
import org.sonar.server.common.almsettings.permission.DopPermissionValidationService;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.dop.response.PermissionCheckResource;
import org.sonar.server.v2.api.dop.response.PermissionChecksRestResponse;

import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.server.common.AlmSettingMapper.toResponseAlm;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;

public class DefaultPermissionChecksController implements PermissionChecksController {

  private static final Set<ALM> SUPPORTED_ALMS = EnumSet.of(ALM.GITHUB, ALM.GITLAB, ALM.AZURE_DEVOPS);

  private final UserSession userSession;
  private final DbClient dbClient;
  private final DopPermissionValidationService dopPermissionValidationService;

  public DefaultPermissionChecksController(UserSession userSession, DbClient dbClient, DopPermissionValidationService dopPermissionValidationService) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.dopPermissionValidationService = dopPermissionValidationService;
  }

  @Override
  public PermissionChecksRestResponse checkPermissions(@Nullable String projectKey) {
    if (projectKey == null) {
      return checkAllConfigurations();
    }
    return checkProjectConfiguration(projectKey);
  }

  private PermissionChecksRestResponse checkAllConfigurations() {
    userSession.checkIsSystemAdministrator();
    List<AlmSettingDto> almSettings;
    try (DbSession dbSession = dbClient.openSession(false)) {
      almSettings = dbClient.almSettingDao().selectAll(dbSession).stream()
        .filter(almSetting -> SUPPORTED_ALMS.contains(almSetting.getAlm()))
        .toList();
    }
    // Validation hits the DevOps Platform APIs, so it runs after the DB session is closed (no connection held) and in
    // parallel.
    List<DopPermissionCheck> checks = dopPermissionValidationService.checkAll(almSettings);
    return toResponse(almSettings, checks);
  }

  private PermissionChecksRestResponse checkProjectConfiguration(String projectKey) {
    Optional<AlmSettingDto> boundAlmSetting;
    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = checkFoundWithOptional(dbClient.projectDao().selectProjectByKey(dbSession, projectKey), "Project '%s' not found", projectKey);
      userSession.checkEntityPermission(USER, project);
      boundAlmSetting = dbClient.projectAlmSettingDao().selectByProject(dbSession, project)
        .map(ProjectAlmSettingDto::getAlmSettingUuid)
        .flatMap(almSettingUuid -> dbClient.almSettingDao().selectByUuid(dbSession, almSettingUuid))
        .filter(almSetting -> SUPPORTED_ALMS.contains(almSetting.getAlm()));
    }
    if (boundAlmSetting.isEmpty()) {
      return new PermissionChecksRestResponse(List.of());
    }
    AlmSettingDto almSetting = boundAlmSetting.get();
    DopPermissionCheck check = dopPermissionValidationService.check(almSetting);
    return toResponse(List.of(almSetting), List.of(check));
  }

  private static PermissionChecksRestResponse toResponse(List<AlmSettingDto> almSettings, List<DopPermissionCheck> checks) {
    List<PermissionCheckResource> resources = new ArrayList<>(almSettings.size());
    for (int i = 0; i < almSettings.size(); i++) {
      resources.add(toResource(almSettings.get(i), checks.get(i)));
    }
    return new PermissionChecksRestResponse(resources);
  }

  private static PermissionCheckResource toResource(AlmSettingDto almSetting, DopPermissionCheck check) {
    return new PermissionCheckResource(
      almSetting.getKey(),
      toResponseAlm(almSetting.getAlm()).name(),
      check.status());
  }

}
