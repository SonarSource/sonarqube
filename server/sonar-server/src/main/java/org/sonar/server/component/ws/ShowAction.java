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
package org.sonar.server.component.ws;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentFinder.ParamNames;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsComponents.ShowWsResponse;
import org.sonarqube.ws.client.component.ShowWsRequest;

import static java.lang.String.format;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.component.ws.ComponentDtoToWsComponent.componentDtoToWsComponent;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SHOW;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_COMPONENT_ID;

public class ShowAction implements ComponentsWsAction {
  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public ShowAction(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SHOW)
      .setDescription(format("Returns a component (file, directory, project, view…) and its ancestors. " +
        "The ancestors are ordered from the parent to the root project. " +
        "The '%s' or '%s' parameter must be provided.<br>" +
        "Requires the following permission: 'Browse' on the project of the specified component.",
        PARAM_COMPONENT_ID, PARAM_COMPONENT))
      .setResponseExample(getClass().getResource("show-example.json"))
      .setSince("5.4")
      .setChangelog(
        new Change("6.4", "Analysis date has been added to the response"),
        new Change("6.4", "The field 'id' is deprecated in the response"),
        new Change("6.4", "The 'visibility' field is added")
      )
      .setHandler(this);

    action.createParam(PARAM_COMPONENT_ID)
      .setDescription("Component id")
      .setDeprecatedKey("id", "6.4")
      .setDeprecatedSince("6.4")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_COMPONENT)
      .setDescription("Component key")
      .setDeprecatedKey("key", "6.4")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ShowWsRequest showWsRequest = toShowWsRequest(request);
    ShowWsResponse showWsResponse = doHandle(showWsRequest);

    writeProtobuf(showWsResponse, request, response);
  }

  private ShowWsResponse doHandle(ShowWsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = getComponentByUuidOrKey(dbSession, request);
      Optional<SnapshotDto> lastAnalysis = dbClient.snapshotDao().selectLastAnalysisByComponentUuid(dbSession, component.projectUuid());
      List<ComponentDto> ancestors = dbClient.componentDao().selectAncestors(dbSession, component);
      OrganizationDto organizationDto = componentFinder.getOrganization(dbSession, component);
      return buildResponse(component, organizationDto, ancestors, lastAnalysis);
    }
  }

  private ComponentDto getComponentByUuidOrKey(DbSession dbSession, ShowWsRequest request) {
    ComponentDto component = componentFinder.getByUuidOrKey(dbSession, request.getId(), request.getKey(), ParamNames.COMPONENT_ID_AND_COMPONENT);
    userSession.checkComponentPermission(UserRole.USER, component);
    return component;
  }

  private static ShowWsResponse buildResponse(ComponentDto component, OrganizationDto organizationDto, List<ComponentDto> orderedAncestors, Optional<SnapshotDto> lastAnalysis) {
    ShowWsResponse.Builder response = ShowWsResponse.newBuilder();
    response.setComponent(componentDtoToWsComponent(component, organizationDto, lastAnalysis));

    // ancestors are ordered from root to leaf, whereas it's the opposite in WS response
    int size = orderedAncestors.size() - 1;
    IntStream.rangeClosed(0, size).forEach(
      index -> response.addAncestors(componentDtoToWsComponent(orderedAncestors.get(size - index), organizationDto, lastAnalysis)));
    return response.build();
  }

  private static ShowWsRequest toShowWsRequest(Request request) {
    return new ShowWsRequest()
      .setId(request.param(PARAM_COMPONENT_ID))
      .setKey(request.param(PARAM_COMPONENT));
  }
}
