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
package org.sonar.server.projectlink.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ProjectLinkDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static org.sonar.db.component.ProjectLinkDto.PROVIDED_TYPES;
import static org.sonar.server.component.ComponentFinder.ParamNames.PROJECT_ID_AND_KEY;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.ACTION_DELETE;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_ID;

public class DeleteAction implements ProjectLinksWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final ComponentFinder componentFinder;

  public DeleteAction(DbClient dbClient, UserSession userSession, ComponentFinder componentFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_DELETE)
      .setDescription("Delete existing project link.<br>" +
        "Requires 'Administer' permission on the specified project, " +
        "or global 'Administer' permission.")
      .setHandler(this)
      .setPost(true)
      .setSince("6.1");

    action.createParam(PARAM_ID)
      .setRequired(true)
      .setDescription("Link id")
      .setExampleValue("17");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(request.mandatoryParam(PARAM_ID));
    response.noContent();
  }

  private void doHandle(String id) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      ProjectLinkDto link = dbClient.projectLinkDao().selectByUuid(dbSession, id);

      link = NotFoundException.checkFound(link, "Link with id '%s' not found", id);
      componentFinder.getProjectByUuidOrKey(dbSession, link.getProjectUuid(), null, PROJECT_ID_AND_KEY);
      ProjectDto projectDto = componentFinder.getProjectByUuidOrKey(dbSession, link.getProjectUuid(), null, PROJECT_ID_AND_KEY);
      checkProjectAdminPermission(projectDto);
      checkNotProvided(link);

      dbClient.projectLinkDao().delete(dbSession, link.getUuid());
      dbSession.commit();
    }
  }

  private void checkProjectAdminPermission(ProjectDto projectDto) {
    if (userSession.hasPermission(GlobalPermission.ADMINISTER)) {
      return;
    }
    userSession.checkEntityPermission(UserRole.ADMIN, projectDto);
  }

  private static void checkNotProvided(ProjectLinkDto link) {
    String type = link.getType();
    boolean isProvided = type != null && PROVIDED_TYPES.contains(type);
    BadRequestException.checkRequest(!isProvided, "Provided link cannot be deleted.");
  }
}
