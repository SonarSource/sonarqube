/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.usergroups.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_ID;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_LOGIN;
import static org.sonar.server.usergroups.ws.GroupWsSupport.defineGroupWsParameters;
import static org.sonar.server.usergroups.ws.GroupWsSupport.defineLoginWsParameter;
import static org.sonar.server.ws.WsUtils.checkFound;

public class RemoveUserAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupWsSupport support;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public RemoveUserAction(DbClient dbClient, UserSession userSession, GroupWsSupport support, DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.support = support;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("remove_user")
      .setDescription(format("Remove a user from a group.<br />" +
        "'%s' or '%s' must be provided.", PARAM_GROUP_ID, PARAM_GROUP_NAME))
      .setHandler(this)
      .setPost(true)
      .setSince("5.2");

    defineGroupWsParameters(action);
    defineLoginWsParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    try (DbSession dbSession = dbClient.openSession(false)) {
      GroupId group = support.findGroup(dbSession, request);
      userSession.checkOrganizationPermission(group.getOrganizationUuid(), SYSTEM_ADMIN);

      String login = request.mandatoryParam(PARAM_LOGIN);
      UserDto user = getUser(dbSession, login);

      ensureLastAdminIsNotRemoved(dbSession, group, user);

      dbClient.userGroupDao().delete(dbSession, group.getId(), user.getId());
      dbClient.userDao().updateRootFlagFromPermissions(dbSession, user.getId(), defaultOrganizationProvider.get().getUuid());
      dbSession.commit();

      response.noContent();
    }
  }

  /**
   * Ensure that there are still users with admin global permission if user is removed from the group.
   */
  private void ensureLastAdminIsNotRemoved(DbSession dbSession, GroupId group, UserDto user) {
    int remainingAdmins = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroupMember(dbSession,
      group.getOrganizationUuid(), SYSTEM_ADMIN, group.getId(), user.getId());
    if (remainingAdmins == 0) {
      throw new BadRequestException("The last administrator user cannot be removed");
    }
  }

  private UserDto getUser(DbSession dbSession, String userLogin) {
    return checkFound(dbClient.userDao().selectActiveUserByLogin(dbSession, userLogin),
      "User with login '%s' is not found'", userLogin);
  }
}
