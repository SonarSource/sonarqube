/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskQuery;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.KeyExamples;
import org.sonarqube.ws.Ce;
import org.sonarqube.ws.Ce.ComponentResponse;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.sonar.db.Pagination.forPage;
import static org.sonar.server.ce.ws.CeWsParameters.PARAM_COMPONENT;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class ComponentAction implements CeWsAction {

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
        "Requires the following permission: 'Browse' on the specified component.")
      .setSince("5.2")
      .setResponseExample(getClass().getResource("component-example.json"))
      .setChangelog(
        new Change("6.1", "field \"logs\" is deprecated and its value is always false"),
        new Change("6.6", "fields \"branch\" and \"branchType\" added"),
        new Change("7.6", format("The use of module keys in parameter \"%s\" is deprecated", PARAM_COMPONENT)),
        new Change("8.8", "Deprecated parameter 'componentId' has been removed."),
        new Change("8.8", "Parameter 'component' is now required."))
      .setHandler(this);

    action.createParam(PARAM_COMPONENT)
      .setRequired(true)
      .setExampleValue(KeyExamples.KEY_PROJECT_EXAMPLE_001);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ComponentDto component = loadComponent(dbSession, wsRequest);
      userSession.checkComponentPermission(UserRole.USER, component);
      List<CeQueueDto> queueDtos = dbClient.ceQueueDao().selectByMainComponentUuid(dbSession, component.uuid());
      CeTaskQuery activityQuery = new CeTaskQuery()
        .setMainComponentUuid(component.uuid())
        .setOnlyCurrents(true);
      List<CeActivityDto> activityDtos = dbClient.ceActivityDao().selectByQuery(dbSession, activityQuery, forPage(1).andSize(1));

      Ce.ComponentResponse.Builder wsResponseBuilder = ComponentResponse.newBuilder();
      wsResponseBuilder.addAllQueue(formatter.formatQueue(dbSession, queueDtos));
      if (activityDtos.size() == 1) {
        wsResponseBuilder.setCurrent(formatter.formatActivity(dbSession, activityDtos.get(0), null, emptyList()));
      }
      writeProtobuf(wsResponseBuilder.build(), wsRequest, wsResponse);
    }
  }

  private ComponentDto loadComponent(DbSession dbSession, Request wsRequest) {
    String componentKey = wsRequest.mandatoryParam(PARAM_COMPONENT);
    return componentFinder.getByKey(dbSession, componentKey);
  }
}
