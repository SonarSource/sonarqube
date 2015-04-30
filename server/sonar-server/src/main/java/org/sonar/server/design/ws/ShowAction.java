/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.design.ws;

import com.google.common.io.Resources;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.design.FileDependencyDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.ws.BaseUsersWsAction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShowAction implements BaseUsersWsAction {

  private static final String PARAM_FROM_PARENT_UUID = "fromParentUuid";
  private static final String PARAM_TO_PARENT_UUID = "toParentUuid";

  private final DbClient dbClient;

  public ShowAction(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("show")
      .setDescription("Show file dependencies between two directories")
      .setSince("5.2")
      .setInternal(true)
      .setHandler(this)
      .setResponseExample(Resources.getResource(getClass(), "example-show.json"));

    action.createParam(PARAM_FROM_PARENT_UUID)
      .setDescription("First directory uuid")
      .setRequired(true)
      .setExampleValue("2312cd03-b514-4acc-94f4-5c5e8e0062b2");

    action.createParam(PARAM_TO_PARENT_UUID)
      .setDescription("Second directory uuid")
      .setRequired(true)
      .setExampleValue("d38641a2-3166-451d-a2db-ab3b82e2d3ca");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String fromParentUuid = request.mandatoryParam(PARAM_FROM_PARENT_UUID);
    String toParentUuid = request.mandatoryParam(PARAM_TO_PARENT_UUID);

    DbSession session = dbClient.openSession(false);
    try {
      ComponentDto fromParent = dbClient.componentDao().getByUuid(session, fromParentUuid);
      ComponentDto project = dbClient.componentDao().getByUuid(session, fromParent.projectUuid());
      UserSession.get().checkProjectUuidPermission(UserRole.USER, project.uuid());

      List<FileDependencyDto> fileDependencies = dbClient.fileDependencyDao().selectFromParents(session, fromParentUuid, toParentUuid, project.getId());
      writeResponse(response, fileDependencies, componentsByUuid(session, fileDependencies));
    } finally {
      session.close();
    }
  }

  private void writeResponse(Response response, List<FileDependencyDto> fileDependencies, Map<String, ComponentDto> componentsByUuid) {
    JsonWriter json = response.newJsonWriter().beginObject();
    json.name("dependencies").beginArray();
    for (FileDependencyDto fileDependencyDto : fileDependencies) {
      ComponentDto fromComponent = getComponent(fileDependencyDto.getFromComponentUuid(), componentsByUuid);
      ComponentDto toComponent = getComponent(fileDependencyDto.getToComponentUuid(), componentsByUuid);
      json.beginObject()
        .prop("fromUuid", fromComponent.uuid())
        .prop("fromName", fromComponent.longName())
        .prop("toUuid", toComponent.uuid())
        .prop("toName", toComponent.longName())
        .prop("weight", fileDependencyDto.getWeight())
        .endObject();
    }
    json.endArray().endObject().close();
  }

  private Map<String, ComponentDto> componentsByUuid(DbSession session, List<FileDependencyDto> fileDependencies) {
    Set<String> uuids = new HashSet<>();
    for (FileDependencyDto fileDependency : fileDependencies) {
      uuids.add(fileDependency.getFromComponentUuid());
      uuids.add(fileDependency.getToComponentUuid());
    }
    Map<String, ComponentDto> componentsByUuid = new HashMap<>();
    for (ComponentDto componentDto : dbClient.componentDao().getByUuids(session, uuids)) {
      componentsByUuid.put(componentDto.uuid(), componentDto);
    }
    return componentsByUuid;
  }

  private static ComponentDto getComponent(String uuid, Map<String, ComponentDto> componentsByUuid) {
    ComponentDto component = componentsByUuid.get(uuid);
    if (component == null) {
      throw new IllegalStateException(String.format("Component with uuid '%s' does not exists", uuid));
    }
    return component;
  }

}
