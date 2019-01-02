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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_ID;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.defineGroupWsParameters;

public class DeleteAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupWsSupport support;

  public DeleteAction(DbClient dbClient, UserSession userSession, GroupWsSupport support) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.support = support;
  }

  @Override
  public void define(NewController context) {
    WebService.NewAction action = context.createAction("delete")
      .setDescription(format("Delete a group. The default groups cannot be deleted.<br/>" +
        "'%s' or '%s' must be provided.<br />" +
        "Requires the following permission: 'Administer System'.",
        PARAM_GROUP_ID, PARAM_GROUP_NAME))
      .setHandler(this)
      .setSince("5.2")
      .setPost(true);

    defineGroupWsParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      GroupDto group = support.findGroupDto(dbSession, request);
      userSession.checkPermission(OrganizationPermission.ADMINISTER, group.getOrganizationUuid());

      support.checkGroupIsNotDefault(dbSession, group);
      checkNotTryingToDeleteLastAdminGroup(dbSession, group);
      removeGroupPermissions(dbSession, group);
      removeFromPermissionTemplates(dbSession, group);
      removeGroupMembers(dbSession, group);
      dbClient.qProfileEditGroupsDao().deleteByGroup(dbSession, group);
      dbClient.groupDao().deleteById(dbSession, group.getId());

      dbSession.commit();
      response.noContent();
    }
  }

  private void checkNotTryingToDeleteLastAdminGroup(DbSession dbSession, GroupDto group) {
    int remaining = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroup(dbSession,
      group.getOrganizationUuid(), OrganizationPermission.ADMINISTER.getKey(), group.getId());

    checkArgument(remaining > 0, "The last system admin group cannot be deleted");
  }

  private void removeGroupPermissions(DbSession dbSession, GroupDto groupId) {
    dbClient.roleDao().deleteGroupRolesByGroupId(dbSession, groupId.getId());
  }

  private void removeFromPermissionTemplates(DbSession dbSession, GroupDto groupId) {
    dbClient.permissionTemplateDao().deleteByGroup(dbSession, groupId.getId());
  }

  private void removeGroupMembers(DbSession dbSession, GroupDto groupId) {
    dbClient.userGroupDao().deleteByGroupId(dbSession, groupId.getId());
  }
}
