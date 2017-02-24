/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.base.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentFinder.ParamNames;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.qualitygate.SelectWsRequest;

import static org.sonar.server.qualitygate.QualityGates.SONAR_QUALITYGATE_PROPERTY;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.ACTION_SELECT;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_GATE_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.qualitygate.QualityGatesWsParameters.PARAM_PROJECT_KEY;

public class SelectAction implements QualityGatesWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public SelectAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_SELECT)
      .setDescription("Associate a project to a quality gate.<br>" +
        "The '%s' or '%s' must be provided.<br>" +
        "Project id as a numeric value is deprecated since 6.1. Please use the id similar to '%s'.<br>" +
        "Require Administer Quality Gates permission.",
        PARAM_PROJECT_ID, PARAM_PROJECT_KEY,
        Uuids.UUID_EXAMPLE_02)
      .setPost(true)
      .setSince("4.3")
      .setHandler(this);

    action.createParam(PARAM_GATE_ID)
      .setDescription("Quality gate id")
      .setRequired(true)
      .setExampleValue("1");

    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project id. Project id as an numeric value is deprecated since 6.1")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setSince("6.1");
  }

  @Override
  public void handle(Request request, Response response) {
    doHandle(toSelectWsRequest(request));
    response.noContent();
  }

  private void doHandle(SelectWsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      checkQualityGate(dbSession, request.getGateId());
      ComponentDto project = getProject(dbSession, request.getProjectId(), request.getProjectKey());

      dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
        .setKey(SONAR_QUALITYGATE_PROPERTY)
        .setResourceId(project.getId())
        .setValue(String.valueOf(request.getGateId())));

      dbSession.commit();
    }
  }

  private static SelectWsRequest toSelectWsRequest(Request request) {
    return new SelectWsRequest()
      .setGateId(request.mandatoryParamAsLong(PARAM_GATE_ID))
      .setProjectId(request.param(PARAM_PROJECT_ID))
      .setProjectKey(request.param(PARAM_PROJECT_KEY));
  }

  private ComponentDto getProject(DbSession dbSession, @Nullable String projectId, @Nullable String projectKey) {
    ComponentDto project = selectProjectById(dbSession, projectId)
      .or(() -> componentFinder.getByUuidOrKey(dbSession, projectId, projectKey, ParamNames.PROJECT_ID_AND_KEY));

    if (!userSession.hasPermission(OrganizationPermission.ADMINISTER_QUALITY_GATES, project.getOrganizationUuid()) &&
      !userSession.hasComponentPermission(UserRole.ADMIN, project)) {
      throw insufficientPrivilegesException();
    }

    return project;
  }

  private Optional<ComponentDto> selectProjectById(DbSession dbSession, @Nullable String projectId) {
    if (projectId == null) {
      return Optional.absent();
    }

    try {
      long dbId = Long.parseLong(projectId);
      return dbClient.componentDao().selectById(dbSession, dbId);
    } catch (NumberFormatException e) {
      return Optional.absent();
    }
  }

  private void checkQualityGate(DbSession dbSession, long id) {
    checkFound(dbClient.qualityGateDao().selectById(dbSession, id), "There is no quality gate with id=" + id);
  }
}
