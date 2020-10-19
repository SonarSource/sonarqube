/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.qualitygate.ws;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.db.qualitygate.QualityGateConditionDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Qualitygates;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER_QUALITY_GATES;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_ORGANIZATION;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

public class QualityGatesWsSupport {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final DefaultOrganizationProvider defaultOrganizationProvider;
  private final ComponentFinder componentFinder;

  public QualityGatesWsSupport(DbClient dbClient, UserSession userSession, DefaultOrganizationProvider defaultOrganizationProvider, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
    this.componentFinder = componentFinder;
  }

  public QGateWithOrgDto getByOrganizationAndUuid(DbSession dbSession, OrganizationDto organization, String qualityGateUuid) {
    return checkFound(
      dbClient.qualityGateDao().selectByOrganizationAndUuid(dbSession, organization, qualityGateUuid),
      "No quality gate has been found for id %s in organization %s", qualityGateUuid, organization.getName());
  }

  public QGateWithOrgDto getByOrganizationAndName(DbSession dbSession, OrganizationDto organization, String qualityGateName) {
    return checkFound(
      dbClient.qualityGateDao().selectByOrganizationAndName(dbSession, organization, qualityGateName),
      "No quality gate has been found for name %s in organization %s", qualityGateName, organization.getName());
  }

  QualityGateConditionDto getCondition(DbSession dbSession, String uuid) {
    return checkFound(dbClient.gateConditionDao().selectByUuid(uuid, dbSession), "No quality gate condition with uuid '%s'", uuid);
  }

  boolean isQualityGateAdmin() {
    return userSession.hasPermission(ADMINISTER_QUALITY_GATES);
  }

  WebService.NewParam createOrganizationParam(NewAction action) {
    return action
      .createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key. If no organization is provided, the default organization is used.")
      .setSince("7.0")
      .setRequired(false)
      .setInternal(false)
      .setExampleValue("my-org");
  }

  Qualitygates.Actions getActions(QualityGateDto qualityGate, @Nullable QualityGateDto defaultQualityGate) {
    boolean isDefault = defaultQualityGate != null && Objects.equals(defaultQualityGate.getUuid(), qualityGate.getUuid());
    boolean isBuiltIn = qualityGate.isBuiltIn();
    boolean isQualityGateAdmin = isQualityGateAdmin();
    return Qualitygates.Actions.newBuilder()
      .setCopy(isQualityGateAdmin)
      .setRename(!isBuiltIn && isQualityGateAdmin)
      .setManageConditions(!isBuiltIn && isQualityGateAdmin)
      .setDelete(!isDefault && !isBuiltIn && isQualityGateAdmin)
      .setSetAsDefault(!isDefault && isQualityGateAdmin)
      .setAssociateProjects(!isDefault && isQualityGateAdmin)
      .build();
  }

  OrganizationDto getDefaultOrganization(DbSession dbSession) {
    return getOrganization(dbSession, defaultOrganizationProvider.get().getKey());
  }

  OrganizationDto getOrganization(DbSession dbSession, String key) {
    Optional<OrganizationDto> organizationDto = dbClient.organizationDao().selectByKey(dbSession, key);
    return checkFoundWithOptional(organizationDto, "No organization with key '%s'", key);
  }

  OrganizationDto getOrganization(DbSession dbSession, Request request) {
    String organizationKey = Optional.ofNullable(request.param(PARAM_ORGANIZATION))
      .orElseGet(() -> defaultOrganizationProvider.get().getKey());
    return getOrganization(dbSession, organizationKey);
  }

  void checkCanEdit(QGateWithOrgDto qualityGate) {
    checkNotBuiltIn(qualityGate);
    userSession.checkPermission(ADMINISTER_QUALITY_GATES);
  }

  void checkCanAdminProject(ProjectDto project) {
    if (userSession.hasPermission(ADMINISTER_QUALITY_GATES)
      || userSession.hasProjectPermission(ADMIN, project)) {
      return;
    }
    throw insufficientPrivilegesException();
  }

  ProjectDto getProject(DbSession dbSession, OrganizationDto organization, String projectKey) {
    ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
    checkProjectBelongsToOrganization(organization, project);
    return project;
  }

  void checkProjectBelongsToOrganization(OrganizationDto organization, ProjectDto project) {
    if (project.getOrganizationUuid().equals(organization.getUuid())) {
      return;
    }
    throw new NotFoundException(format("Project '%s' doesn't exist in organization '%s'", project.getKey(), organization.getKey()));
  }

  private static void checkNotBuiltIn(QualityGateDto qualityGate) {
    checkArgument(!qualityGate.isBuiltIn(), "Operation forbidden for built-in Quality Gate '%s'", qualityGate.getName());
  }

}
