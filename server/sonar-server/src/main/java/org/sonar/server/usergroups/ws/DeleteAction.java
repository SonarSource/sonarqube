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

import java.util.Optional;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_ID;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_GROUP_NAME;
import static org.sonar.server.usergroups.ws.GroupWsSupport.defineGroupWsParameters;

public class DeleteAction implements UserGroupsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupWsSupport support;
  private final Settings settings;
  private final DefaultOrganizationProvider defaultOrganizationProvider;

  public DeleteAction(DbClient dbClient, UserSession userSession, GroupWsSupport support, Settings settings,
    DefaultOrganizationProvider defaultOrganizationProvider) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.support = support;
    this.settings = settings;
    this.defaultOrganizationProvider = defaultOrganizationProvider;
  }

  @Override
  public void define(NewController context) {
    WebService.NewAction action = context.createAction("delete")
      .setDescription(format("Delete a group. The default groups cannot be deleted.<br/>" +
        "'%s' or '%s' must be provided.<br />" +
        "Requires System Administrator permission.",
        PARAM_GROUP_ID, PARAM_GROUP_NAME))
      .setHandler(this)
      .setSince("5.2")
      .setPost(true);

    defineGroupWsParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      GroupId groupId = support.findGroup(dbSession, request);
      userSession.checkOrganizationPermission(groupId.getOrganizationUuid(), SYSTEM_ADMIN);

      checkNotTryingToDeleteDefaultGroup(dbSession, groupId);
      checkNotTryingToDeleteLastAdminGroup(dbSession, groupId);
      removeGroupPermissions(dbSession, groupId);
      removeFromPermissionTemplates(dbSession, groupId);
      updateRootFlagOfMembers(dbSession, groupId);
      removeGroupMembers(dbSession, groupId);
      dbClient.groupDao().deleteById(dbSession, groupId.getId());

      dbSession.commit();
      response.noContent();
    }
  }

  /**
   * The property "default group" is used when registering a new user so that
   * he automatically becomes a member of this group. This feature does not
   * not exist on non-default organizations yet as organization settings
   * are not implemented.
   */
  private void checkNotTryingToDeleteDefaultGroup(DbSession dbSession, GroupId group) {
    String defaultGroupName = settings.getString(CoreProperties.CORE_DEFAULT_GROUP);
    if (defaultGroupName != null && group.getOrganizationUuid().equals(defaultOrganizationProvider.get().getUuid())) {
      Optional<GroupDto> defaultGroup = dbClient.groupDao().selectByName(dbSession, group.getOrganizationUuid(), defaultGroupName);
      checkArgument(!defaultGroup.isPresent() || defaultGroup.get().getId() != group.getId(),
        format("Default group '%s' cannot be deleted", defaultGroupName));
    }
  }

  private void checkNotTryingToDeleteLastAdminGroup(DbSession dbSession, GroupId group) {
    int remaining = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingGroup(dbSession,
      group.getOrganizationUuid(), SYSTEM_ADMIN, group.getId());

    checkArgument(remaining > 0, "The last system admin group cannot be deleted");
  }

  private void removeGroupPermissions(DbSession dbSession, GroupId groupId) {
    dbClient.roleDao().deleteGroupRolesByGroupId(dbSession, groupId.getId());
  }

  private void removeFromPermissionTemplates(DbSession dbSession, GroupId groupId) {
    dbClient.permissionTemplateDao().deleteByGroup(dbSession, groupId.getId());
  }

  private void updateRootFlagOfMembers(DbSession dbSession, GroupId groupId) {
    dbClient.groupDao().updateRootFlagOfUsersInGroupFromPermissions(dbSession, groupId.getId(), defaultOrganizationProvider.get().getUuid());
  }

  private void removeGroupMembers(DbSession dbSession, GroupId groupId) {
    dbClient.userGroupDao().deleteByGroupId(dbSession, groupId.getId());
  }
}
