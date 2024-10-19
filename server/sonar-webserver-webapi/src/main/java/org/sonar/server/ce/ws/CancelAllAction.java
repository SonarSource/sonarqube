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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.ce.queue.CeQueue;
import org.sonar.core.util.Uuids;
import org.sonar.server.user.UserSession;

public class CancelAllAction implements CeWsAction {

  public static final String INCLUDE_IN_PROGRESS_TASKS = "includeInProgressTasks";
  private final UserSession userSession;
  private final CeQueue queue;

  public CancelAllAction(UserSession userSession, CeQueue queue) {
    this.userSession = userSession;
    this.queue = queue;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("cancel_all")
            .setDescription(
                    "Cancels all pending tasks. Requires system administration permission. In-progress tasks are not "
                            + "canceled by default.")
            .setInternal(true)
            .setPost(true)
            .setSince("5.2")
            .setHandler(this);

    action
            .createParam(INCLUDE_IN_PROGRESS_TASKS)
            .setSince("23.2.8")
            .setDescription("Cancel in-progress tasks.")
            .setBooleanPossibleValues()
            .setDefaultValue("false");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) {
    boolean includeInProgressTasks = wsRequest.mandatoryParamAsBoolean(INCLUDE_IN_PROGRESS_TASKS);
    userSession.checkIsSystemAdministrator();
    queue.cancelAll(includeInProgressTasks);
    wsResponse.noContent();
  }
}
