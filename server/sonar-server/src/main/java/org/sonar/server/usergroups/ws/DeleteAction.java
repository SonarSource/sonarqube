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

import com.google.common.base.Preconditions;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.user.GroupDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

public class DeleteAction implements UserGroupsWsAction {

  private static final String PARAM_ID = "id";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final Settings settings;

  public DeleteAction(DbClient dbClient, UserSession userSession, Settings settings) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.settings = settings;
  }

  @Override
  public void define(NewController context) {
    context.createAction("delete")
      .setDescription("Delete a group. The default group cannot be deleted. Requires System Administrator permission.")
      .setHandler(this)
      .setSince("5.2")
      .setPost(true)
      .createParam(PARAM_ID)
      .setDescription("ID of the group to delete.")
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    long groupId = request.mandatoryParamAsLong(PARAM_ID);


    DbSession dbSession = dbClient.openSession(false);
    try {
      if (dbClient.groupDao().selectById(dbSession, groupId) == null) {
        throw new NotFoundException(String.format("Could not find a group with id=%d", groupId));
      }

      checkNotTryingToDeleteDefaultGroup(dbSession, groupId);

      removeGroupMembers(groupId, dbSession);
      removeGroupPermissions(groupId, dbSession);
      removeFromPermissionTemplates(groupId, dbSession);
      dbClient.groupDao().deleteById(dbSession, groupId);

      dbSession.commit();
      response.noContent();
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
  }

  private void checkNotTryingToDeleteDefaultGroup(DbSession dbSession, long groupId) {
    String defaultGroupName = settings.getString(CoreProperties.CORE_DEFAULT_GROUP);
    GroupDto defaultGroup = dbClient.groupDao().selectOrFailByName(dbSession, defaultGroupName);
    Preconditions.checkArgument(groupId != defaultGroup.getId(),
      String.format("Default group '%s' cannot be deleted", defaultGroupName));
  }

  private void removeGroupMembers(long groupId, DbSession dbSession) {
    dbClient.userGroupDao().deleteMembersByGroupId(dbSession, groupId);
  }

  private void removeGroupPermissions(long groupId, DbSession dbSession) {
    dbClient.roleDao().deleteGroupRolesByGroupId(dbSession, groupId);
  }

  private void removeFromPermissionTemplates(long groupId, DbSession dbSession) {
    dbClient.permissionTemplateDao().deleteByGroup(dbSession, groupId);
  }
}
