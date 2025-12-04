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
package org.sonar.server.dismissmessage.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDismissedMessageDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;

import static org.sonar.server.dismissmessage.ws.DismissMessageWsAction.parseMessageType;
import static org.sonar.server.dismissmessage.ws.DismissMessageWsAction.verifyProjectKeyAndMessageType;

public class DismissAction implements DismissMessageWsAction {

  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public DismissAction(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }
  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("dismiss")
      .setDescription("Dismiss a message.")
      .setSince("10.2")
      .setPost(true)
      .setInternal(true)
      .setHandler(this);

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("The project key");

    action.createParam(PARAM_MESSAGE_TYPE)
      .setDescription("The type of the message dismissed")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    ProjectDto project = null;
    String projectKey = request.param(PARAM_PROJECT_KEY);
    String messageType = request.mandatoryParam(PARAM_MESSAGE_TYPE);

    MessageType type = parseMessageType(messageType);
    verifyProjectKeyAndMessageType(projectKey, type);
    try (DbSession dbSession = dbClient.openSession(false)) {
      if(projectKey != null) {
        project = componentFinder.getProjectByKey(dbSession, projectKey);
      }
      dismissMessage(dbSession, project, type);
      dbSession.commit();
    }
    response.noContent();
  }

  private void dismissMessage(DbSession dbSession, @Nullable ProjectDto project, MessageType type) {
    Optional<UserDismissedMessageDto> result;
    if (project == null) {
      result = dbClient.userDismissedMessagesDao().selectByUserUuidAndMessageType(dbSession, userSession.getUuid(), type);
    } else {
      result = dbClient.userDismissedMessagesDao().selectByUserAndProjectAndMessageType(dbSession, userSession.getUuid(), project, type);
    }
    if (result.isEmpty()) {
      dbClient.userDismissedMessagesDao().insert(dbSession, new UserDismissedMessageDto()
        .setUuid(Uuids.create())
        .setUserUuid(userSession.getUuid())
        .setProjectUuid(project == null ? null: project.getUuid())
        .setMessageType(type));
    }
  }



}
