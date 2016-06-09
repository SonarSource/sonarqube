/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.component.ComponentFinder.ParamNames;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsComponents.ShowWsResponse;
import org.sonarqube.ws.client.component.ShowWsRequest;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.sonar.core.util.Uuids.UUID_EXAMPLE_01;
import static org.sonar.server.component.ws.ComponentDtoToWsComponent.componentDtoToWsComponent;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.KeyExamples.KEY_PROJECT_EXAMPLE_001;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.ACTION_SHOW;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_ID;
import static org.sonarqube.ws.client.component.ComponentsWsParameters.PARAM_KEY;

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
          "The '%s' or '%s' must be provided.<br>" +
          "Requires one of the following permissions:" +
          "<ul>" +
          "<li>'Administer System'</li>" +
          "<li>'Administer' rights on the specified project</li>" +
          "<li>'Browse' on the specified project</li>" +
          "</ul>",
        PARAM_ID, PARAM_KEY))
      .setResponseExample(getClass().getResource("show-example.json"))
      .setSince("5.4")
      .setHandler(this);

    action.createParam(PARAM_ID)
      .setDescription("Component id")
      .setExampleValue(UUID_EXAMPLE_01);

    action.createParam(PARAM_KEY)
      .setDescription("Component key")
      .setExampleValue(KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ShowWsRequest showWsRequest = toShowWsRequest(request);
    ShowWsResponse showWsResponse = doHandle(showWsRequest);

    writeProtobuf(showWsResponse, request, response);
  }

  private ShowWsResponse doHandle(ShowWsRequest request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      ComponentDto component = getComponentByUuidOrKey(dbSession, request);
      SnapshotDto lastSnapshot = dbClient.snapshotDao().selectLastSnapshotByComponentUuid(dbSession, component.uuid());
      List<ComponentDto> orderedAncestors = emptyList();
      if (lastSnapshot != null) {
        ShowData.Builder showDataBuilder = ShowData.builder(lastSnapshot);
        List<SnapshotDto> ancestorsSnapshots = dbClient.snapshotDao().selectByIds(dbSession, showDataBuilder.getOrderedSnapshotIds());
        showDataBuilder.withAncestorsSnapshots(ancestorsSnapshots);
        List<ComponentDto> ancestorComponents = dbClient.componentDao().selectByUuids(dbSession, showDataBuilder.getOrderedComponentUuids());
        ShowData showData = showDataBuilder.andAncestorComponents(ancestorComponents);
        orderedAncestors = showData.getComponents();
      }

      return buildResponse(component, orderedAncestors);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static ShowWsResponse buildResponse(ComponentDto component, List<ComponentDto> orderedAncestorComponents) {
    ShowWsResponse.Builder response = ShowWsResponse.newBuilder();
    response.setComponent(componentDtoToWsComponent(component));

    for (ComponentDto ancestor : orderedAncestorComponents) {
      response.addAncestors(componentDtoToWsComponent(ancestor));
    }

    return response.build();
  }

  private ComponentDto getComponentByUuidOrKey(DbSession dbSession, ShowWsRequest request) {
    ComponentDto component = componentFinder.getByUuidOrKey(dbSession, request.getId(), request.getKey(), ParamNames.ID_AND_KEY);
    String projectUuid = firstNonNull(component.projectUuid(), component.uuid());
    if (!userSession.hasPermission(GlobalPermissions.SYSTEM_ADMIN) &&
      !userSession.hasComponentUuidPermission(UserRole.ADMIN, projectUuid) &&
      !userSession.hasComponentUuidPermission(UserRole.USER, projectUuid)) {
      throw insufficientPrivilegesException();
    }
    return component;
  }

  private static ShowWsRequest toShowWsRequest(Request request) {
    return new ShowWsRequest()
      .setId(request.param(PARAM_ID))
      .setKey(request.param(PARAM_KEY));
  }
}
