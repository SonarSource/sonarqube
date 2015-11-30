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
package org.sonar.server.computation.ws;

import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;
import org.sonarqube.ws.WsCe;

public class QueueAction implements CeWsAction {

  public static final String PARAM_COMPONENT_UUID = "componentId";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final TaskFormatter formatter;

  public QueueAction(UserSession userSession, DbClient dbClient, TaskFormatter formatter) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.formatter = formatter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("queue")
      .setDescription("Gets the pending and in-progress tasks. Requires system administration permission.")
      .setInternal(true)
      .setSince("5.2")
      .setResponseExample(getClass().getResource("queue-example.json"))
      .setHandler(this);

    action
      .createParam(PARAM_COMPONENT_UUID)
      .setDescription("Optional filter on component. Requires administration permission of the component.")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    String componentUuid = wsRequest.param(PARAM_COMPONENT_UUID);

    DbSession dbSession = dbClient.openSession(false);
    try {
      List<CeQueueDto> dtos;
      if (componentUuid == null) {
        // no filters
        userSession.checkGlobalPermission(UserRole.ADMIN);
        dtos = dbClient.ceQueueDao().selectAllInAscOrder(dbSession);
      } else {
        // filter by component
        if (userSession.hasGlobalPermission(GlobalPermissions.SYSTEM_ADMIN) || userSession.hasComponentUuidPermission(UserRole.ADMIN, componentUuid)) {
          dtos = dbClient.ceQueueDao().selectByComponentUuid(dbSession, componentUuid);
        } else {
          throw new ForbiddenException("Requires system administration permission");
        }
      }

      WsCe.QueueResponse.Builder wsResponseBuilder = WsCe.QueueResponse.newBuilder();
      wsResponseBuilder.addAllTasks(formatter.formatQueue(dbSession, dtos));
      WsUtils.writeProtobuf(wsResponseBuilder.build(), wsRequest, wsResponse);

    } finally {
      dbClient.closeSession(dbSession);
    }
  }
}
