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
import org.sonar.db.component.ComponentDto;
import org.sonar.server.component.ComponentFinder;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionUpdater;

public class AddGroupAction implements PermissionsWsAction {

  public static final String ACTION = "add_group";
  public static final String PARAM_PERMISSION = "permission";
  public static final String PARAM_GROUP_NAME = "groupName";
  public static final String PARAM_GROUP_ID = "groupId";
  public static final String PARAM_PROJECT_ID = "projectId";
  public static final String PARAM_PROJECT_KEY = "projectKey";

  private final DbClient dbClient;
  private final PermissionWsCommons permissionWsCommons;
  private final PermissionUpdater permissionUpdater;
  private final ComponentFinder componentFinder;

  public AddGroupAction(DbClient dbClient, PermissionWsCommons permissionWsCommons, PermissionUpdater permissionUpdater, ComponentFinder componentFinder) {
    this.permissionWsCommons = permissionWsCommons;
    this.permissionUpdater = permissionUpdater;
    this.dbClient = dbClient;
    this.componentFinder = componentFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setDescription("Add permission to a group.<br /> " +
        "If the project id is provided, a project permission is created.<br />" +
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
      .setDescription("Group id")
      .setExampleValue("42");

    action.createParam(PARAM_PROJECT_ID)
      .setDescription("Project id")
      .setExampleValue("ce4c03d6-430f-40a9-b777-ad877c00aa4d");

    action.createParam(PARAM_PROJECT_KEY)
      .setDescription("Project key")
      .setExampleValue("org.apache.hbas:hbase");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String permission = request.mandatoryParam(PARAM_PERMISSION);
    String groupNameParam = request.param(PARAM_GROUP_NAME);
    Long groupId = request.paramAsLong(PARAM_GROUP_ID);
    String projectUuid = request.param(PARAM_PROJECT_ID);
    String projectKey = request.param(PARAM_PROJECT_KEY);

    DbSession dbSession = dbClient.openSession(false);
    try {
      String groupName = permissionWsCommons.searchGroupName(dbSession, groupNameParam, groupId);
      PermissionChange permissionChange = permissionChange(dbSession, permission, groupName, projectUuid, projectKey);

      permissionUpdater.addPermission(permissionChange);
    } finally {
      dbClient.closeSession(dbSession);
    }

    response.noContent();
  }

  private PermissionChange permissionChange(DbSession dbSession, String permission, String groupName, @Nullable String projectUuid, @Nullable String projectKey) {
    PermissionChange permissionChange = new PermissionChange()
      .setPermission(permission)
      .setGroup(groupName);
    if (isProjectUuidOrProjectKeyProvided(projectUuid, projectKey)) {
      ComponentDto project = componentFinder.getProjectByUuidOrKey(dbSession, projectUuid, projectKey);
      permissionChange.setComponentKey(project.key());
    }
    return permissionChange;
  }

  private static boolean isProjectUuidOrProjectKeyProvided(@Nullable String projectUuid, @Nullable String projectKey) {
    return projectUuid != null || projectKey != null;
  }
}
