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
package org.sonar.server.ce.ws;

import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskQuery;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;

import static org.sonar.db.Pagination.forPage;
import static org.sonar.server.component.ComponentFinder.ParamNames.COMPONENT_ID_AND_KEY;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.WsCe.ProjectResponse;

public class ComponentAction implements CeWsAction {

  public static final String PARAM_COMPONENT_ID = "componentId";
  public static final String PARAM_COMPONENT_KEY = "componentKey";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final TaskFormatter formatter;
  private final ComponentFinder componentFinder;

  public ComponentAction(UserSession userSession, DbClient dbClient, TaskFormatter formatter, ComponentFinder componentFinder) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.formatter = formatter;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("component")
      .setDescription("Get the pending tasks, in-progress tasks and the last executed task of a given component (usually a project).<br>" +
        "Requires the following permission: 'Browse' on the specified component.<br>" +
        "Either '%s' or '%s' must be provided, not both.<br>" +
        "Since 6.1, field \"logs\" is deprecated and its value is always false.",
        PARAM_COMPONENT_ID, PARAM_COMPONENT_KEY)
      .setSince("5.2")
      .setResponseExample(getClass().getResource("component-example.json"))
      .setHandler(this);

    action.createParam(PARAM_COMPONENT_ID)
      .setRequired(false)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    action.createParam(PARAM_COMPONENT_KEY)
      .setRequired(false)
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = componentFinder.getByUuidOrKey(dbSession, wsRequest.param(PARAM_COMPONENT_ID), wsRequest.param(PARAM_COMPONENT_KEY), COMPONENT_ID_AND_KEY);
      userSession.checkComponentPermission(UserRole.USER, component);
      List<CeQueueDto> queueDtos = dbClient.ceQueueDao().selectByComponentUuid(dbSession, component.uuid());
      CeTaskQuery activityQuery = new CeTaskQuery()
        .setComponentUuid(component.uuid())
        .setOnlyCurrents(true);
      List<CeActivityDto> activityDtos = dbClient.ceActivityDao().selectByQuery(dbSession, activityQuery, forPage(1).andSize(1));

      ProjectResponse.Builder wsResponseBuilder = ProjectResponse.newBuilder();
      wsResponseBuilder.addAllQueue(formatter.formatQueue(dbSession, queueDtos));
      if (activityDtos.size() == 1) {
        wsResponseBuilder.setCurrent(formatter.formatActivity(dbSession, activityDtos.get(0)));
      }
      writeProtobuf(wsResponseBuilder.build(), wsRequest, wsResponse);
    }
  }
}
