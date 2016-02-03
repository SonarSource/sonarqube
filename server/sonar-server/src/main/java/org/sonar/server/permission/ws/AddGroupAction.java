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
package org.sonar.server.permission.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionUpdater;
import org.sonarqube.ws.client.permission.AddGroupWsRequest;

import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createGroupIdParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createGroupNameParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createPermissionParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectParameters;
import static org.sonar.server.permission.ws.WsProjectRef.newOptionalWsProjectRef;
import static org.sonar.server.usergroups.ws.WsGroupRef.newWsGroupRef;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class AddGroupAction implements PermissionsWsAction {

  public static final String ACTION = "add_group";

  private final DbClient dbClient;
  private final PermissionChangeBuilder permissionChangeBuilder;
  private final PermissionUpdater permissionUpdater;

  public AddGroupAction(DbClient dbClient, PermissionChangeBuilder permissionChangeBuilder, PermissionUpdater permissionUpdater) {
    this.permissionChangeBuilder = permissionChangeBuilder;
    this.permissionUpdater = permissionUpdater;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setDescription("Add permission to a group.<br /> " +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br /> " +
        "The group name or group id must be provided. <br />" +
        "It requires administration permissions to access.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    createPermissionParameter(action);
    createGroupNameParameter(action);
    createGroupIdParameter(action);
    createProjectParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    AddGroupWsRequest addGroupWsRequest = toAddGroupWsRequest(request);
    doHandle(addGroupWsRequest);

    response.noContent();
  }

  private void doHandle(AddGroupWsRequest request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Long groupId = request.getGroupId() == null ? null : Long.valueOf(request.getGroupId());
      PermissionChange permissionChange = permissionChangeBuilder.buildGroupPermissionChange(
        dbSession,
        request.getPermission(),
        newOptionalWsProjectRef(request.getProjectId(), request.getProjectKey()),
        newWsGroupRef(groupId, request.getGroupName()));
      permissionUpdater.addPermission(permissionChange);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static AddGroupWsRequest toAddGroupWsRequest(Request request) {
    return new AddGroupWsRequest()
      .setPermission(request.mandatoryParam(PARAM_PERMISSION))
      .setGroupId(request.param(PARAM_GROUP_ID))
      .setGroupName(request.param(PARAM_GROUP_NAME))
      .setProjectId(request.param(PARAM_PROJECT_ID))
      .setProjectKey(request.param(PARAM_PROJECT_KEY));
  }
}
