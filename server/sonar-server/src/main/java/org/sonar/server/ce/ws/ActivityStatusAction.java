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
package org.sonar.server.ce.ws;

import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.Ce.ActivityStatusWsResponse;

import static org.sonar.server.ce.ws.CeWsParameters.DEPRECATED_PARAM_COMPONENT_KEY;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_COMPONENT_ID;
import static org.sonar.server.component.ComponentFinder.ParamNames.COMPONENT_ID_AND_KEY;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ActivityStatusAction implements CeWsAction {
  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;
  private final System2 system2;

  public ActivityStatusAction(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder, System2 system2) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
    this.system2 = system2;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("activity_status")
      .setDescription("Returns CE activity related metrics.<br>" +
        "Requires 'Administer System' permission or 'Administer' rights on the specified project.")
      .setSince("5.5")
      .setResponseExample(getClass().getResource("activity_status-example.json"))
      .setHandler(this);

    action.createParam(PARAM_COMPONENT_ID)
      .setDescription("Id of the component (project) to filter on")
      .setExampleValue(Uuids.UUID_EXAMPLE_03);
    action.createParam(DEPRECATED_PARAM_COMPONENT_KEY)
      .setDeprecatedSince("6.6")
      .setDescription("Key of the component (project) to filter on")
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);

    action.setChangelog(new Change("6.6", "New field 'inProgress' in response"));
    action.setChangelog(new Change("7.8", "New field 'pendingTime' in response, only included when there are pending tasks"));
  }

  @Override
  public void handle(org.sonar.api.server.ws.Request request, Response response) throws Exception {
    ActivityStatusWsResponse activityStatusResponse = doHandle(toWsRequest(request));
    writeProtobuf(activityStatusResponse, request, response);
  }

  private ActivityStatusWsResponse doHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<ComponentDto> component = searchComponent(dbSession, request);
      String componentUuid = component.map(ComponentDto::uuid).orElse(null);
      checkPermissions(component);
      int pendingCount = dbClient.ceQueueDao().countByStatusAndMainComponentUuid(dbSession, CeQueueDto.Status.PENDING, componentUuid);
      int inProgressCount = dbClient.ceQueueDao().countByStatusAndMainComponentUuid(dbSession, CeQueueDto.Status.IN_PROGRESS, componentUuid);
      int failingCount = dbClient.ceActivityDao().countLastByStatusAndMainComponentUuid(dbSession, CeActivityDto.Status.FAILED, componentUuid);

      Optional<Long> creationDate = dbClient.ceQueueDao().selectCreationDateOfOldestPendingByMainComponentUuid(dbSession, componentUuid);

      ActivityStatusWsResponse.Builder builder = ActivityStatusWsResponse.newBuilder()
        .setPending(pendingCount)
        .setInProgress(inProgressCount)
        .setFailing(failingCount);

      creationDate.ifPresent(d -> {
        long ageOfOldestPendingTime = system2.now() - d;
        builder.setPendingTime(ageOfOldestPendingTime);
      });

      return builder.build();
    }
  }

  private Optional<ComponentDto> searchComponent(DbSession dbSession, Request request) {
    ComponentDto component = null;
    if (hasComponentInRequest(request)) {
      component = componentFinder.getByUuidOrKey(dbSession, request.getComponentId(), request.getComponentKey(), COMPONENT_ID_AND_KEY);
    }
    return Optional.ofNullable(component);
  }

  private void checkPermissions(Optional<ComponentDto> component) {
    if (component.isPresent()) {
      userSession.checkComponentPermission(UserRole.ADMIN, component.get());
    } else {
      userSession.checkIsSystemAdministrator();
    }
  }

  private static boolean hasComponentInRequest(Request request) {
    return request.getComponentId() != null || request.getComponentKey() != null;
  }

  private static Request toWsRequest(org.sonar.api.server.ws.Request request) {
    return new Request(request.param(PARAM_COMPONENT_ID), request.param(DEPRECATED_PARAM_COMPONENT_KEY));
  }

  private static class Request {
    private String componentId;
    private String componentKey;

    Request(@Nullable String componentId, @Nullable String componentKey) {
      this.componentId = componentId;
      this.componentKey = componentKey;
    }

    @CheckForNull
    public String getComponentId() {
      return componentId;
    }

    @CheckForNull
    public String getComponentKey() {
      return componentKey;
    }
  }
}
