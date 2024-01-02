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
package org.sonar.server.dismissmessage.ws;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDismissedMessageDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.MediaTypes;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.sonar.server.dismissmessage.ws.DismissMessageWsAction.parseMessageType;
import static org.sonar.server.dismissmessage.ws.DismissMessageWsAction.verifyProjectKeyAndMessageType;

public class CheckAction implements DismissMessageWsAction {
  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public CheckAction(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("check")
      .setDescription("Check if a message has been dismissed.")
      .setSince("10.2")
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
    String projectKey = request.param(PARAM_PROJECT_KEY);
    String messageType = request.mandatoryParam(PARAM_MESSAGE_TYPE);

    MessageType type = parseMessageType(messageType);
    verifyProjectKeyAndMessageType(projectKey, type);
    Optional<UserDismissedMessageDto> userDismissedMessage;
    try (DbSession dbSession = dbClient.openSession(false)) {
      if (projectKey != null) {
        ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
        userDismissedMessage = dbClient.userDismissedMessagesDao().selectByUserAndProjectAndMessageType(dbSession, userSession.getUuid(), project, type);
      } else {
        userDismissedMessage = dbClient.userDismissedMessagesDao().selectByUserUuidAndMessageType(dbSession, userSession.getUuid(), type);
      }
    }
    writeToResponse(response, userDismissedMessage.isPresent());
  }

  private static void writeToResponse(Response response, boolean dismissed) throws IOException {
    String json = new GsonBuilder().create().toJson(new DismissRecord(dismissed));
    response.stream().setStatus(HTTP_OK);
    response.stream().setMediaType(MediaTypes.JSON);
    response.stream().output().write(json.getBytes(StandardCharsets.UTF_8));
    response.stream().output().flush();
  }

  private record DismissRecord(@SerializedName("dismissed") boolean dismissed) {
  }
}
