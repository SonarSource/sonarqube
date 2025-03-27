/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.Ce.ActivityStatusWsResponse;

import static org.sonar.server.ce.ws.CeWsParameters.PARAM_COMPONENT;
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

    action.createParam(PARAM_COMPONENT)
      .setDescription("Key of the component (project) to filter on")
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);

    action.setChangelog(
      new Change("6.6", "New field 'inProgress' in response"),
      new Change("7.8", "New field 'pendingTime' in response, only included when there are pending tasks"),
      new Change("8.8", "Parameter 'componentId' is now deprecated."),
      new Change("8.8", "Parameter 'componentKey' is now removed. Please use parameter 'component' instead."),
      new Change("10.0", "Remove deprecated field 'componentId'"));
  }

  @Override
  public void handle(org.sonar.api.server.ws.Request request, Response response) throws Exception {
    ActivityStatusWsResponse activityStatusResponse = doHandle(toWsRequest(request));
    writeProtobuf(activityStatusResponse, request, response);
  }

  private ActivityStatusWsResponse doHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<EntityDto> entity = searchEntity(dbSession, request);
      String entityUuid = entity.map(EntityDto::getUuid).orElse(null);
      checkPermissions(entity.orElse(null));
      int pendingCount = dbClient.ceQueueDao().countByStatusAndEntityUuid(dbSession, CeQueueDto.Status.PENDING, entityUuid);
      int inProgressCount = dbClient.ceQueueDao().countByStatusAndEntityUuid(dbSession, CeQueueDto.Status.IN_PROGRESS, entityUuid);
      int failingCount = dbClient.ceActivityDao().countLastByStatusAndEntityUuid(dbSession, CeActivityDto.Status.FAILED, entityUuid);

      Optional<Long> creationDate = dbClient.ceQueueDao().selectCreationDateOfOldestPendingByEntityUuid(dbSession, entityUuid);

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

  private Optional<EntityDto> searchEntity(DbSession dbSession, Request request) {
    EntityDto entity = null;
    if (request.getComponentKey() != null) {
      entity = componentFinder.getEntityByKey(dbSession, request.getComponentKey());
    }
    return Optional.ofNullable(entity);
  }

  private void checkPermissions(@Nullable EntityDto entity) {
    if (entity != null) {
      userSession.checkEntityPermission(ProjectPermission.ADMIN, entity);
    } else {
      userSession.checkIsSystemAdministrator();
    }
  }

  private static Request toWsRequest(org.sonar.api.server.ws.Request request) {
    return new Request(request.param(PARAM_COMPONENT));
  }

  private static class Request {
    private final String componentKey;

    Request(@Nullable String componentKey) {
      this.componentKey = componentKey;
    }

    @CheckForNull
    public String getComponentKey() {
      return componentKey;
    }
  }
}
