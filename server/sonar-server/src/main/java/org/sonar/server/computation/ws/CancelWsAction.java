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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.server.computation.CeQueue;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;

public class CancelWsAction implements CeWsAction {

  public static final String PARAM_TASK_ID = "id";
  public static final String PARAM_ALL = "all";

  private final UserSession userSession;
  private final CeQueue queue;

  public CancelWsAction(UserSession userSession, CeQueue queue) {
    this.userSession = userSession;
    this.queue = queue;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("cancel")
      .setDescription("Cancels a pending task. Requires system administration permission.")
      .setInternal(true)
      .setPost(true)
      .setHandler(this);

    action
      .createParam(PARAM_TASK_ID)
      .setDescription("Optional id of the task to cancel.")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);

    action
      .createParam(PARAM_ALL)
      .setDescription("Cancels all pending tasks if this parameter is set. Ignored if the parameter " + PARAM_TASK_ID + " is set.")
      .setBooleanPossibleValues()
      .setDefaultValue("false");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    userSession.checkGlobalPermission(UserRole.ADMIN);
    String taskId = wsRequest.param(PARAM_TASK_ID);
    if (taskId != null) {
      queue.cancel(taskId);
    } else if (wsRequest.paramAsBoolean(PARAM_ALL)) {
      queue.cancelAll();
    } else {
      throw new BadRequestException("Missing parameters");
    }
    wsResponse.noContent();
  }
}
