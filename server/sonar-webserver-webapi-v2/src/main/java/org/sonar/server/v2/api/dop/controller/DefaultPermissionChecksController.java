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
import org.sonar.server.common.almsettings.permission.AffectedInstallation;
import org.sonar.server.common.almsettings.permission.DopPermissionCheck;
import org.sonar.server.common.almsettings.permission.DopPermissionValidationService;
import org.sonar.server.common.almsettings.permission.InstallationCheckStatus;
import org.sonar.server.common.almsettings.permission.PermissionDeficit;
import org.sonar.server.common.almsettings.permission.TimestampedPermissionCheck;
import org.sonar.server.user.UserSession;
import org.sonar.server.v2.api.dop.response.AffectedInstallationResource;
import org.sonar.server.v2.api.dop.response.PermissionCheckResource;
import org.sonar.server.v2.api.dop.response.PermissionChecksRestResponse;
import org.sonar.server.v2.api.dop.response.PermissionDeficitResource;

import static org.sonar.db.permission.ProjectPermission.USER;
import static org.sonar.server.common.AlmSettingMapper.toResponseAlm;
import static org.sonar.server.exceptions.BadRequestException.throwBadRequestException;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;
import static org.sonar.server.user.ServiceIdentity.AGENTIC_SHARED;

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
  public PermissionChecksRestResponse checkPermissions(@Nullable String projectKey, @Nullable String configurationKey, boolean refresh) {
    if (projectKey != null && configurationKey != null) {
      throwBadRequestException("'project' parameter cannot be used together with the 'configuration' parameter");
    }
    if (refresh && configurationKey == null) {
      // Refreshing everything at once would let one page load fan out into a live call per configuration, so the
      // explicit re-check is deliberately scoped to the connection the administrator is looking at.
      throwBadRequestException("'refresh' parameter requires the 'configuration' parameter");
    }
    if (projectKey != null) {
      return checkProjectConfiguration(projectKey);
    }
    if (configurationKey != null) {
      return checkSingleConfiguration(configurationKey, refresh);
    }
    return checkAllConfigurations();
  }

  private PermissionChecksRestResponse checkAllConfigurations() {
    checkAdministratorOrTrustedService();
    List<AlmSettingDto> almSettings;
    try (DbSession dbSession = dbClient.openSession(false)) {
      almSettings = dbClient.almSettingDao().selectAll(dbSession).stream()
        .filter(almSetting -> SUPPORTED_ALMS.contains(almSetting.getAlm()))
        .toList();
    }
    // A cache hit is instant; a miss hits the DevOps Platform APIs, so this runs after the DB session is closed (no
    // connection held) and in parallel.
    List<TimestampedPermissionCheck> checks = dopPermissionValidationService.checkAllCached(almSettings);
    return toResponse(almSettings, checks, Scope.ADMINISTRATOR);
  }

  private PermissionChecksRestResponse checkSingleConfiguration(String configurationKey, boolean refresh) {
    if (refresh) {
      // No trusted-service bypass here: an explicit live check is an administrator action, not something a service
      // identity reading cached status should be able to trigger.
      userSession.checkIsSystemAdministrator();
    } else {
      checkAdministratorOrTrustedService();
    }
    AlmSettingDto almSetting;
    try (DbSession dbSession = dbClient.openSession(false)) {
      almSetting = checkFoundWithOptional(
        dbClient.almSettingDao().selectByKey(dbSession, configurationKey).filter(setting -> SUPPORTED_ALMS.contains(setting.getAlm())),
        "DevOps Platform configuration '%s' not found, or not supported by the Remediation Agent", configurationKey);
    }
    TimestampedPermissionCheck check = refresh
      ? dopPermissionValidationService.checkRefreshed(almSetting)
      : dopPermissionValidationService.checkCached(almSetting);
    return toResponse(List.of(almSetting), List.of(check), Scope.ADMINISTRATOR);
  }

  private PermissionChecksRestResponse checkProjectConfiguration(String projectKey) {
    Optional<AlmSettingDto> boundAlmSetting;
    String repositorySlug;
    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = checkFoundWithOptional(dbClient.projectDao().selectProjectByKey(dbSession, projectKey), "Project '%s' not found", projectKey);
      userSession.checkEntityPermission(USER, project);
      Optional<ProjectAlmSettingDto> projectBinding = dbClient.projectAlmSettingDao().selectByProject(dbSession, project);
      // The repository the project is bound to, not just its configuration: on GitHub it is the repository, not the
      // configuration, that identifies which installation's granted permissions actually apply to this project.
      repositorySlug = projectBinding.map(ProjectAlmSettingDto::getAlmRepo).orElse(null);
      boundAlmSetting = projectBinding
        .map(ProjectAlmSettingDto::getAlmSettingUuid)
        .flatMap(almSettingUuid -> dbClient.almSettingDao().selectByUuid(dbSession, almSettingUuid))
        .filter(almSetting -> SUPPORTED_ALMS.contains(almSetting.getAlm()));
    }
    if (boundAlmSetting.isEmpty()) {
      return new PermissionChecksRestResponse(List.of());
    }
    AlmSettingDto almSetting = boundAlmSetting.get();
    TimestampedPermissionCheck check = dopPermissionValidationService.checkForProject(almSetting, repositorySlug);
    return toResponse(List.of(almSetting), List.of(check), Scope.PROJECT);
  }

  private void checkAdministratorOrTrustedService() {
    if (userSession.getServiceIdentity().orElse(null) != AGENTIC_SHARED) {
      userSession.checkIsSystemAdministrator();
    }
  }

  private static PermissionChecksRestResponse toResponse(List<AlmSettingDto> almSettings, List<TimestampedPermissionCheck> checks, Scope scope) {
    List<PermissionCheckResource> resources = new ArrayList<>(almSettings.size());
    for (int i = 0; i < almSettings.size(); i++) {
      resources.add(toResource(almSettings.get(i), checks.get(i), scope));
    }
    return new PermissionChecksRestResponse(resources);
  }

  private static PermissionCheckResource toResource(AlmSettingDto almSetting, TimestampedPermissionCheck timestampedCheck, Scope scope) {
    DopPermissionCheck check = timestampedCheck.check();
    boolean isGithub = almSetting.getAlm() == ALM.GITHUB;
    // The installation detail is GitHub-specific, and the list answers an instance-wide question, so a project request
    // never carries it — a project's verdict is about its own installation.
    boolean exposeInstallations = isGithub && scope == Scope.ADMINISTRATOR && check.installationCheckStatus() == InstallationCheckStatus.COMPLETE;
    return new PermissionCheckResource(
      almSetting.getKey(),
      toResponseAlm(almSetting.getAlm()).name(),
      check.status(),
      timestampedCheck.checkedAt(),
      isGithub ? toDeficitResources(check.appMissingPermissions()) : null,
      isGithub ? check.installationCheckStatus() : null,
      exposeInstallations ? check.totalInstallationCount() : null,
      exposeInstallations ? check.affectedInstallationCount() : null,
      exposeInstallations ? toInstallationResources(check.affectedInstallations()) : null);
  }

  private static List<AffectedInstallationResource> toInstallationResources(List<AffectedInstallation> installations) {
    return installations.stream()
      .map(installation -> new AffectedInstallationResource(
        installation.installationId(),
        installation.installationOwner(),
        installation.settingsUrl(),
        toDeficitResources(installation.missingPermissions())))
      .toList();
  }

  private static List<PermissionDeficitResource> toDeficitResources(List<PermissionDeficit> deficits) {
    return deficits.stream()
      .map(deficit -> new PermissionDeficitResource(deficit.permission(), deficit.required(), deficit.granted()))
      .toList();
  }

  /**
   * Who is asking, and therefore how much of a GitHub result they get back: an administrator looking at the DevOps
   * Platform settings page needs the installations to chase, a project reader needs only their own project's verdict.
   */
  private enum Scope {
    ADMINISTRATOR, PROJECT
  }

}
