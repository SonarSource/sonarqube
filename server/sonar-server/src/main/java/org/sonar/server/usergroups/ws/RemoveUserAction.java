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

import javax.annotation.CheckForNull;
import org.sonar.api.security.DefaultGroups;
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
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.api.security.DefaultGroups.isAnyone;
import static org.sonar.server.usergroups.ws.UserGroupsWsParameters.PARAM_GROUP_ID;
import static org.sonar.server.usergroups.ws.UserGroupsWsParameters.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.UserGroupsWsParameters.PARAM_LOGIN;
import static org.sonar.server.usergroups.ws.UserGroupsWsParameters.createGroupParameters;
import static org.sonar.server.usergroups.ws.UserGroupsWsParameters.createLoginParameter;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class RemoveUserAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;

  public RemoveUserAction(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("remove_user")
      .setDescription(format("Remove a user from a group.<br />" +
        "'%s' or '%s' must be provided.", PARAM_GROUP_ID, PARAM_GROUP_NAME))
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
      GroupDto group = getGroup(dbSession, wsGroupRef);
      checkRequest(group != null, "It is not possible to remove a user from the '%s' group.", DefaultGroups.ANYONE);
      UserDto user = getUser(dbSession, login);

      UserGroupDto userGroup = new UserGroupDto().setGroupId(group.getId()).setUserId(user.getId());
      dbClient.userGroupDao().delete(dbSession, userGroup);
      dbSession.commit();
      response.noContent();
    } finally {
      dbClient.closeSession(dbSession);
    }

  }

  /**
   *
   * @return null if it's the anyone group
   */
  @CheckForNull
  private GroupDto getGroup(DbSession dbSession, WsGroupRef group) {
    Long groupId = group.id();
    String groupName = group.name();

    if (isAnyone(groupName)) {
      return null;
    }

    GroupDto groupDto = null;

    if (groupId != null) {
      groupDto = checkFound(dbClient.groupDao().selectById(dbSession, groupId),
        "Group with id '%d' is not found", groupId);
    }

    if (groupName != null) {
      groupDto = checkFound(dbClient.groupDao().selectByName(dbSession, groupName),
        "Group with name '%s' is not found", groupName);
    }

    return groupDto;
  }

  private UserDto getUser(DbSession dbSession, String userLogin) {
    return checkFound(dbClient.userDao().selectActiveUserByLogin(dbSession, userLogin),
      "User with login '%s' is not found'", userLogin);
  }
}
