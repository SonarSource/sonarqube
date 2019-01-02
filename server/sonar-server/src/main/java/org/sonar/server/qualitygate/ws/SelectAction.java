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
package org.sonar.server.qualitygate.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.qualitygate.QGateWithOrgDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentFinder.ParamNames;

import static org.sonar.server.qualitygate.QualityGateFinder.SONAR_QUALITYGATE_PROPERTY;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_SELECT;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_GATE_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PROJECT_ID;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.PARAM_PROJECT_KEY;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;

public class SelectAction implements QualityGatesWsAction {
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final QualityGatesWsSupport wsSupport;

  public SelectAction(DbClient dbClient, ComponentFinder componentFinder, QualityGatesWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_SELECT)
      .setDescription("Associate a project to a quality gate.<br>" +
        "The '%s' or '%s' must be provided.<br>" +
        "Project id as a numeric value is deprecated since 6.1. Please use the id similar to '%s'.<br>" +
        "Requires the 'Administer Quality Gates' permission.",
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

    wsSupport.createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) {
    long gateId = request.mandatoryParamAsLong(PARAM_GATE_ID);
    String projectId = request.param(PARAM_PROJECT_ID);
    String projectKey = request.param(PARAM_PROJECT_KEY);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      QGateWithOrgDto qualityGate = wsSupport.getByOrganizationAndId(dbSession, organization, gateId);
      ComponentDto project = getProject(dbSession, organization, projectId, projectKey);
      wsSupport.checkCanAdminProject(organization, project);

      dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
        .setKey(SONAR_QUALITYGATE_PROPERTY)
        .setResourceId(project.getId())
        .setValue(String.valueOf(qualityGate.getId())));

      dbSession.commit();
    }
    response.noContent();
  }

  private ComponentDto getProject(DbSession dbSession, OrganizationDto organization, @Nullable String projectId, @Nullable String projectKey) {
    ComponentDto project = selectProjectById(dbSession, projectId)
      .orElseGet(() -> componentFinder.getByUuidOrKey(dbSession, projectId, projectKey, ParamNames.PROJECT_ID_AND_KEY));
    wsSupport.checkProjectBelongsToOrganization(organization, project);
    return project;
  }

  private Optional<ComponentDto> selectProjectById(DbSession dbSession, @Nullable String projectId) {
    if (projectId == null) {
      return Optional.empty();
    }

    try {
      long dbId = Long.parseLong(projectId);
      return Optional.ofNullable(dbClient.componentDao().selectById(dbSession, dbId).orElse(null));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

}
