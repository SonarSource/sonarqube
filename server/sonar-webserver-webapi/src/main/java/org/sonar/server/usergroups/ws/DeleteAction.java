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
package org.sonar.server.usergroups.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.GlobalPermission;
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
      .setPost(true)
      .setChangelog(
        new Change("8.4", "Parameter 'id' is deprecated. Format changes from integer to string. Use 'name' instead."));

    defineGroupWsParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      GroupDto group = support.findGroupDto(dbSession, request);
      userSession.checkPermission(GlobalPermission.ADMINISTER);

      support.checkGroupIsNotDefault(dbSession, group);
      checkNotTryingToDeleteLastAdminGroup(dbSession, group);
      removeGroupPermissions(dbSession, group);
      removeFromPermissionTemplates(dbSession, group);
      removeGroupMembers(dbSession, group);
      dbClient.qProfileEditGroupsDao().deleteByGroup(dbSession, group);
      dbClient.qualityGateGroupPermissionsDao().deleteByGroup(dbSession, group);
      dbClient.groupDao().deleteByUuid(dbSession, group.getUuid(), group.getName());

      dbSession.commit();
      response.noContent();
    }
  }

  private void checkNotTryingToDeleteLastAdminGroup(DbSession dbSession, GroupDto group) {
    int remaining = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroup(dbSession,
      GlobalPermission.ADMINISTER.getKey(), group.getUuid());

    checkArgument(remaining > 0, "The last system admin group cannot be deleted");
  }

  private void removeGroupPermissions(DbSession dbSession, GroupDto group) {
    dbClient.roleDao().deleteGroupRolesByGroupUuid(dbSession, group.getUuid());
  }

  private void removeFromPermissionTemplates(DbSession dbSession, GroupDto group) {
    dbClient.permissionTemplateDao().deleteByGroup(dbSession, group.getUuid(), group.getName());
  }

  private void removeGroupMembers(DbSession dbSession, GroupDto group) {
    dbClient.userGroupDao().deleteByGroupUuid(dbSession, group.getUuid(), group.getName());
  }
}
