/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.usergroups.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_ID;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_LOGIN;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_ORGANIZATION_KEY;
import static org.sonar.server.usergroups.ws.GroupWsSupport.defineGroupWsParameters;
import static org.sonar.server.usergroups.ws.GroupWsSupport.defineLoginWsParameter;
import static org.sonar.server.ws.WsUtils.checkFound;

public class AddUserAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupWsSupport support;

  public AddUserAction(DbClient dbClient, UserSession userSession, GroupWsSupport support) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.support = support;
  }

  @Override
  public void define(NewController context) {
    NewAction action = context.createAction("add_user")
      .setDescription(format("Add a user to a group.<br />" +
        "'%s' or '%s' must be provided.<br />" +
        "Requires the following permission: 'Administer System'.", PARAM_GROUP_ID, PARAM_GROUP_NAME))
      .setHandler(this)
      .setPost(true)
      .setSince("5.2")
      .setChangelog(new Change("6.4", "It's no longer possible to add a user to the default group"));

    defineGroupWsParameters(action);
    defineLoginWsParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      GroupDto group = support.findGroupDto(dbSession, request);
      userSession.checkLoggedIn().checkPermission(ADMINISTER, group.getOrganizationUuid());

      String login = request.mandatoryParam(PARAM_LOGIN);
      UserDto user = dbClient.userDao().selectActiveUserByLogin(dbSession, login);
      checkFound(user, "Could not find a user with login '%s'", login);

      OrganizationDto organization = support.findOrganizationByKey(dbSession, request.param(PARAM_ORGANIZATION_KEY));
      checkMembership(dbSession, organization, user);
      support.checkGroupIsNotDefault(dbSession, group);

      if (!isMemberOf(dbSession, user, group)) {
        UserGroupDto membershipDto = new UserGroupDto().setGroupId(group.getId()).setUserId(user.getId());
        dbClient.userGroupDao().insert(dbSession, membershipDto);
        dbSession.commit();
      }

      response.noContent();
    }
  }

  private boolean isMemberOf(DbSession dbSession, UserDto user, GroupDto groupId) {
    return dbClient.groupMembershipDao().selectGroupIdsByUserId(dbSession, user.getId()).contains(groupId.getId());
  }

  private void checkMembership(DbSession dbSession, OrganizationDto organization, UserDto user) {
    checkArgument(dbClient.organizationMemberDao().select(dbSession, organization.getUuid(), user.getId()).isPresent(),
      "User '%s' is not member of organization '%s'", user.getLogin(), organization.getKey());
  }
}
