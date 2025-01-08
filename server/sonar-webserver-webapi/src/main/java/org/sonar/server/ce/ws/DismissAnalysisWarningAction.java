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
package org.sonar.server.ce.ws;

import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeTaskMessageDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDismissedMessageDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;

public class DismissAnalysisWarningAction implements CeWsAction {

  private static final String MESSAGE_NOT_FOUND = "Message '%s' not found.";
  private static final String MESSAGE_CANNOT_BE_DISMISSED = "Message '%s' cannot be dismissed.";
  private static final String PARAM_COMPONENT_KEY = "component";
  private static final String PARAM_MESSAGE_KEY = "warning";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final ComponentFinder componentFinder;

  public DismissAnalysisWarningAction(UserSession userSession, DbClient dbClient, ComponentFinder componentFinder) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller
      .createAction("dismiss_analysis_warning")
      .setPost(true)
      .setDescription("Permanently dismiss a specific analysis warning. Requires authentication and 'Browse' permission on the specified project.")
      .setSince("8.5")
      .setInternal(true)
      .setHandler(this);

    action.createParam(PARAM_COMPONENT_KEY)
      .setDescription("Key of the project")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_01);
    action.createParam(PARAM_MESSAGE_KEY)
      .setDescription("Key of the warning")
      .setRequired(true)
      .setExampleValue(Uuids.UUID_EXAMPLE_02);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    String projectKey = request.mandatoryParam(PARAM_COMPONENT_KEY);
    String messageKey = request.mandatoryParam(PARAM_MESSAGE_KEY);

    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectDto project = componentFinder.getProjectByKey(dbSession, projectKey);
      userSession.checkEntityPermission(UserRole.USER, project);

      CeTaskMessageDto messageDto = dbClient.ceTaskMessageDao()
        .selectByUuid(dbSession, messageKey)
        .orElseThrow(() -> new NotFoundException(format(MESSAGE_NOT_FOUND, messageKey)));
      if (!messageDto.getType().isDismissible()) {
        throw new IllegalArgumentException(format(MESSAGE_CANNOT_BE_DISMISSED, messageKey));
      }

      Optional<UserDismissedMessageDto> result = dbClient.userDismissedMessagesDao().selectByUserAndProjectAndMessageType(dbSession,
        userSession.getUuid(), project, messageDto.getType());
      if (!result.isPresent()) {
        dbClient.userDismissedMessagesDao().insert(dbSession, new UserDismissedMessageDto()
          .setUuid(Uuids.create())
          .setUserUuid(userSession.getUuid())
          .setProjectUuid(project.getUuid())
          .setMessageType(messageDto.getType()));
        dbSession.commit();
      }

      response.noContent();
    }
  }

}
