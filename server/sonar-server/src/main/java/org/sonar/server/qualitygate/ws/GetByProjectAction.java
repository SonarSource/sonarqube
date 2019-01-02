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

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.qualitygate.QualityGateFinder;
import org.sonar.server.qualitygate.QualityGateFinder.QualityGateData;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Qualitygates.GetByProjectResponse;

import static java.lang.String.format;
import static org.sonar.api.web.UserRole.ADMIN;
import static org.sonar.api.web.UserRole.USER;
import static org.sonar.server.qualitygate.ws.QualityGatesWsParameters.ACTION_GET_BY_PROJECT;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class GetByProjectAction implements QualityGatesWsAction {
  private static final String PARAM_PROJECT = "project";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final QualityGateFinder qualityGateFinder;
  private final QualityGatesWsSupport wsSupport;

  public GetByProjectAction(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder, QualityGateFinder qualityGateFinder, QualityGatesWsSupport wsSupport) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.qualityGateFinder = qualityGateFinder;
    this.wsSupport = wsSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_GET_BY_PROJECT)
      .setInternal(false)
      .setSince("6.1")
      .setDescription("Get the quality gate of a project.<br />" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "<li>'Browse' on the specified project</li>" +
        "</ul>")
      .setResponseExample(getClass().getResource("get_by_project-example.json"))
      .setHandler(this)
      .setChangelog(
        new Change("6.6", "The parameter 'projectId' has been removed"),
        new Change("6.6", "The parameter 'projectKey' has been renamed to 'project'"),
        new Change("6.6", "This webservice is now part of the public API"));

    action.createParam(PARAM_PROJECT)
      .setDescription("Project key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001)
      .setRequired(true);

    wsSupport.createOrganizationParam(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = wsSupport.getOrganization(dbSession, request);
      ComponentDto project = componentFinder.getByKey(dbSession, request.mandatoryParam(PARAM_PROJECT));
      // As ComponentFinder doesn't handle organization yet, we only check here that the project belongs to the organization
      wsSupport.checkProjectBelongsToOrganization(organization, project);

      if (!userSession.hasComponentPermission(USER, project) &&
        !userSession.hasComponentPermission(ADMIN, project)) {
        throw insufficientPrivilegesException();
      }

      QualityGateData data = qualityGateFinder.getQualityGate(dbSession, organization, project)
        .orElseThrow(() -> new NotFoundException(format("Quality gate not found for project %s", project.getKey())));

      writeProtobuf(buildResponse(data), request, response);
    }
  }

  private static GetByProjectResponse buildResponse(QualityGateData data) {
    QualityGateDto qualityGate = data.getQualityGate();
    GetByProjectResponse.Builder response = GetByProjectResponse.newBuilder();

    response.getQualityGateBuilder()
      .setId(qualityGate.getId())
      .setName(qualityGate.getName())
      .setDefault(data.isDefault());

    return response.build();
  }

}
