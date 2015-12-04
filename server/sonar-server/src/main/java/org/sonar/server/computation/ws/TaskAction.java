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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsCe;

import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class TaskAction implements CeWsAction {

  public static final String ACTION = "task";
  public static final String PARAM_TASK_UUID = "id";
  private static final Set<String> AUTHORIZED_PERMISSIONS = ImmutableSet.of(GlobalPermissions.SCAN_EXECUTION, GlobalPermissions.SYSTEM_ADMIN);

  private final DbClient dbClient;
  private final TaskFormatter wsTaskFormatter;
  private final UserSession userSession;

  public TaskAction(DbClient dbClient, TaskFormatter wsTaskFormatter, UserSession userSession) {
    this.dbClient = dbClient;
    this.wsTaskFormatter = wsTaskFormatter;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION)
      .setDescription("Give Compute Engine task details such as type, status, duration and associated component.<br />" +
        "Requires 'Administer System' or 'Execute Analysis' permission.")
      .setResponseExample(getClass().getResource("task-example.json"))
      .setSince("5.2")
      .setHandler(this);

    action
      .createParam(PARAM_TASK_UUID)
      .setRequired(true)
      .setDescription("Id of task")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    userSession.checkAnyGlobalPermissions(AUTHORIZED_PERMISSIONS);

    String taskUuid = wsRequest.mandatoryParam(PARAM_TASK_UUID);
    DbSession dbSession = dbClient.openSession(false);
    try {
      WsCe.TaskResponse.Builder wsTaskResponse = WsCe.TaskResponse.newBuilder();
      Optional<CeQueueDto> queueDto = dbClient.ceQueueDao().selectByUuid(dbSession, taskUuid);
      if (queueDto.isPresent()) {
        wsTaskResponse.setTask(wsTaskFormatter.formatQueue(dbSession, queueDto.get()));
      } else {
        Optional<CeActivityDto> activityDto = dbClient.ceActivityDao().selectByUuid(dbSession, taskUuid);
        if (activityDto.isPresent()) {
          wsTaskResponse.setTask(wsTaskFormatter.formatActivity(dbSession, activityDto.get()));
        } else {
          throw new NotFoundException();
        }
      }
      writeProtobuf(wsTaskResponse.build(), wsRequest, wsResponse);

    } finally {
      dbClient.closeSession(dbSession);
    }
  }
}
