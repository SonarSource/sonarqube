/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

import java.util.Optional;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.ce.queue.CeQueue;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.server.user.UserSession;

public class CancelAction implements CeWsAction {

  public static final String PARAM_TASK_ID = "id";

  private final UserSession userSession;
  private DbClient dbClient;
  private final CeQueue queue;

  public CancelAction(UserSession userSession, DbClient dbClient, CeQueue queue) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.queue = queue;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("cancel")
      .setDescription("Cancels a pending task.<br/>" +
        "In-progress tasks cannot be canceled.<br/>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the project related to the task</li>" +
        "</ul>")
      .setInternal(true)
      .setPost(true)
      .setSince("5.2")
      .setHandler(this);

    action
      .createParam(PARAM_TASK_ID)
      .setRequired(true)
      .setDescription("Id of the task to cancel.")
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) {
    String taskId = wsRequest.mandatoryParam(PARAM_TASK_ID);
    try (DbSession dbSession = dbClient.openSession(false)) {
      Optional<CeQueueDto> queueDto = dbClient.ceQueueDao().selectByUuid(dbSession, taskId);
      queueDto.ifPresent(dto -> {
        checkPermission(dbSession, dto);
        queue.cancel(dbSession, dto);
      });
    }
    wsResponse.noContent();
  }

  private void checkPermission(DbSession dbSession, CeQueueDto ceQueueDto) {
    if (userSession.isSystemAdministrator()) {
      return;
    }
    String componentUuid = ceQueueDto.getComponentUuid();
    if (componentUuid == null) {
      throw insufficientPrivilegesException();
    }
    com.google.common.base.Optional<ComponentDto> component = dbClient.componentDao().selectByUuid(dbSession, componentUuid);
    if (!component.isPresent()) {
      throw insufficientPrivilegesException();
    }
    userSession.checkComponentPermission(UserRole.ADMIN, component.get());
  }
}
