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
package org.sonar.server.usergroups.ws;

import java.util.Arrays;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.db.MyBatis.closeQuietly;
import static org.sonar.server.usergroups.ws.UserGroupsWsParameters.PARAM_GROUP_ID;
import static org.sonar.server.usergroups.ws.UserGroupsWsParameters.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.UserGroupsWsParameters.PARAM_LOGIN;
import static org.sonar.server.usergroups.ws.UserGroupsWsParameters.createGroupParameters;
import static org.sonar.server.usergroups.ws.UserGroupsWsParameters.createLoginParameter;

public class AddUserAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserGroupFinder userGroupFinder;
  private final UserSession userSession;

  public AddUserAction(DbClient dbClient, UserGroupFinder userGroupFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.userGroupFinder = userGroupFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("add_user")
      .setDescription(format("Add a user to a group.<br />" +
        "'%s' or '%s' must be provided", PARAM_GROUP_ID, PARAM_GROUP_NAME))
      .setHandler(this)
      .setPost(true)
      .setSince("5.2");

    createGroupParameters(action);
    createLoginParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    WsGroupRef wsGroupRef = WsGroupRef.newWsGroupRefFromUserGroupRequest(request);
    String login = request.mandatoryParam(PARAM_LOGIN);

    DbSession dbSession = dbClient.openSession(false);
    try {
      GroupDto group = userGroupFinder.getGroup(dbSession, wsGroupRef);

      UserDto user = dbClient.userDao().selectActiveUserByLogin(dbSession, login);
      if (user == null) {
        throw new NotFoundException(format("Could not find a user with login '%s'", login));
      }

      if (userIsNotYetMemberOf(dbSession, login, group)) {
        UserGroupDto userGroup = new UserGroupDto().setGroupId(group.getId()).setUserId(user.getId());
        dbClient.userGroupDao().insert(dbSession, userGroup);
        dbSession.commit();
      }

      response.noContent();
    } finally {
      closeQuietly(dbSession);
    }

  }

  private boolean userIsNotYetMemberOf(DbSession dbSession, String login, GroupDto group) {
    return !dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, Arrays.asList(login)).get(login).contains(group.getName());
  }
}
