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

package org.sonar.server.ce.ws;

import com.google.common.base.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.WsCe.ActivityStatusWsResponse;
import org.sonarqube.ws.client.ce.ActivityStatusWsRequest;

import static org.sonar.server.component.ComponentFinder.ParamNames.COMPONENT_ID_AND_KEY;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_COMPONENT_ID;
import static org.sonarqube.ws.client.ce.CeWsParameters.PARAM_COMPONENT_KEY;

public class ActivityStatusAction implements CeWsAction {
  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public ActivityStatusAction(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("activity_status")
      .setDescription("Return CE activity related metrics.<br>" +
        "Requires 'Administer System' permission or 'Administer' rights on the specified project.")
      .setSince("5.5")
      .setResponseExample(getClass().getResource("activity_status-example.json"))
      .setInternal(true)
      .setHandler(this);

    action.createParam(PARAM_COMPONENT_ID)
      .setDescription("Id of the component (project) to filter on")
      .setExampleValue(Uuids.UUID_EXAMPLE_03);
    action.createParam(PARAM_COMPONENT_KEY)
      .setDescription("Key of the component (project) to filter on")
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    ActivityStatusWsResponse activityStatusResponse = doHandle(toWsRequest(request));
    writeProtobuf(activityStatusResponse, request, response);
  }

  private ActivityStatusWsResponse doHandle(ActivityStatusWsRequest request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<ComponentDto> component = searchComponent(dbSession, request);
      String componentUuid = component.isPresent() ? component.get().uuid() : null;
      checkPermissions(componentUuid);
      int pendingCount = dbClient.ceQueueDao().countByStatusAndComponentUuid(dbSession, CeQueueDto.Status.PENDING, componentUuid);
      int failingCount = dbClient.ceActivityDao().countLastByStatusAndComponentUuid(dbSession, CeActivityDto.Status.FAILED, componentUuid);

      return ActivityStatusWsResponse.newBuilder()
        .setPending(pendingCount)
        .setFailing(failingCount)
        .build();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private Optional<ComponentDto> searchComponent(DbSession dbSession, ActivityStatusWsRequest request) {
    ComponentDto component = null;
    if (hasComponentInRequest(request)) {
      component = componentFinder.getByUuidOrKey(dbSession, request.getComponentId(), request.getComponentKey(), COMPONENT_ID_AND_KEY);
    }
    return Optional.fromNullable(component);
  }

  private void checkPermissions(@Nullable String componentUuid) {
    if (componentUuid == null) {
      userSession.checkPermission(GlobalPermissions.SYSTEM_ADMIN);
    } else {
      userSession.checkComponentUuidPermission(UserRole.ADMIN, componentUuid);
    }
  }

  private static boolean hasComponentInRequest(ActivityStatusWsRequest request) {
    return request.getComponentId() != null || request.getComponentKey() != null;
  }

  private static ActivityStatusWsRequest toWsRequest(Request request) {
    return ActivityStatusWsRequest.newBuilder()
      .setComponentId(request.param(PARAM_COMPONENT_ID))
      .setComponentKey(request.param(PARAM_COMPONENT_KEY))
      .build();
  }
}
