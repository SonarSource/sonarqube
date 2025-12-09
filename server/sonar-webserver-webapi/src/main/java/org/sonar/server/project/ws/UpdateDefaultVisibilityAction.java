/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.project.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.project.ProjectDefaultVisibility;
import org.sonar.server.project.Visibility;
import org.sonar.server.user.UserSession;

import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

public class UpdateDefaultVisibilityAction implements ProjectsWsAction {
  static final String ACTION = "update_default_visibility";
  static final String PARAM_PROJECT_VISIBILITY = "projectVisibility";

  private final UserSession userSession;
  private final DbClient dbClient;
  private final ProjectDefaultVisibility projectDefaultVisibility;

  public UpdateDefaultVisibilityAction(UserSession userSession, DbClient dbClient, ProjectDefaultVisibility projectDefaultVisibility) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.projectDefaultVisibility = projectDefaultVisibility;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setPost(true)
      .setDescription("Update the default visibility for new projects.<br/>Requires System Administrator privileges")
      .setChangelog(
        new Change("7.3", "This WS used to be located at /api/organizations/update_project_visibility"))
      .setInternal(true)
      .setSince("6.4")
      .setHandler(this);

    action.createParam(PARAM_PROJECT_VISIBILITY)
      .setRequired(true)
      .setDescription("Default visibility for projects")
      .setPossibleValues(Visibility.getLabels());
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String newDefaultProjectVisibility = request.mandatoryParam(PARAM_PROJECT_VISIBILITY);

    try (DbSession dbSession = dbClient.openSession(false)) {
      if (!userSession.isSystemAdministrator()) {
        throw insufficientPrivilegesException();
      }
      projectDefaultVisibility.set(dbSession, newDefaultProjectVisibility);
      dbSession.commit();
    }
    response.noContent();
  }
}
