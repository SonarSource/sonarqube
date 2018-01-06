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
package org.sonar.server.projectlink.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentLinkDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.ws.WsUtils;

import static org.sonar.db.component.ComponentLinkDto.PROVIDED_TYPES;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.ACTION_DELETE;
import static org.sonar.server.projectlink.ws.ProjectLinksWsParameters.PARAM_ID;

public class DeleteAction implements ProjectLinksWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;

  public DeleteAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
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

  private void doHandle(String idParam) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      long id = Long.parseLong(idParam);
      ComponentLinkDto link = dbClient.componentLinkDao().selectById(dbSession, id);

      link = WsUtils.checkFound(link, "Link with id '%s' not found", id);
      checkProjectAdminPermission(link);
      checkNotProvided(link);

      dbClient.componentLinkDao().delete(dbSession, link.getId());
      dbSession.commit();
    }
  }

  private static void checkNotProvided(ComponentLinkDto link) {
    String type = link.getType();
    boolean isProvided = type != null && PROVIDED_TYPES.contains(type);
    WsUtils.checkRequest(!isProvided, "Provided link cannot be deleted.");
  }

  private void checkProjectAdminPermission(ComponentLinkDto link) {
    userSession.checkComponentUuidPermission(UserRole.ADMIN, link.getComponentUuid());
  }
}
