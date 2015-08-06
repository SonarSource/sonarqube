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

package org.sonar.server.permission.ws;

import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionUpdater;

public class AddGroupAction implements PermissionsWsAction {

  public static final String ACTION = "add_group";
  public static final String PARAM_PERMISSION = "permission";
  public static final String PARAM_GROUP_NAME = "groupName";
  public static final String PARAM_GROUP_ID = "groupId";

  private final PermissionUpdater permissionUpdater;
  private final DbClient dbClient;

  public AddGroupAction(PermissionUpdater permissionUpdater, DbClient dbClient) {
    this.permissionUpdater = permissionUpdater;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setDescription("Add permission to a group.<br /> " +
        "The group name or group id must be provided. <br />" +
        "Requires 'Administer System' permission.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_PERMISSION)
      .setDescription("Permission")
      .setRequired(true)
      .setPossibleValues(GlobalPermissions.ALL);

    action.createParam(PARAM_GROUP_NAME)
      .setDescription("Group name or 'anyone' (whatever the case)")
      .setExampleValue("sonar-administrators");

    action.createParam(PARAM_GROUP_ID)
      .setDescription("Group ID")
      .setExampleValue("42");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String permission = request.mandatoryParam(PARAM_PERMISSION);
    String groupNameParam = request.param(PARAM_GROUP_NAME);
    Long groupId = request.paramAsLong(PARAM_GROUP_ID);

    String groupName = searchName(groupNameParam, groupId);

    permissionUpdater.addPermission(
      new PermissionChange()
        .setPermission(permission)
        .setGroup(groupName)
    );

    response.noContent();
  }

  private String searchName(@Nullable String groupNameParam, @Nullable Long groupId) {
    checkParameters(groupNameParam, groupId);
    if (groupNameParam != null) {
      return groupNameParam;
    }

    DbSession dbSession = dbClient.openSession(false);
    try {
      GroupDto group = dbClient.groupDao().selectById(dbSession, groupId);
      if (group == null) {
        throw new NotFoundException(String.format("Group with id '%d' not found", groupId));
      }

      return group.getName();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private void checkParameters(@Nullable String groupName, @Nullable Long groupId) {
    if (groupName != null ^ groupId != null) {
      return;
    }

    throw new BadRequestException("Group name or group id must be provided, not both");
  }
}
