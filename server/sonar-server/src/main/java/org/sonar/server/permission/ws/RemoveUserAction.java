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

import com.google.common.base.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.permission.PermissionChange;
import org.sonar.server.permission.PermissionUpdater;
import org.sonarqube.ws.client.permission.RemoveUserWsRequest;

import static org.sonar.server.permission.ws.PermissionRequestValidator.validatePermission;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createPermissionParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createUserLoginParameter;
import static org.sonar.server.permission.ws.WsProjectRef.newOptionalWsProjectRef;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class RemoveUserAction implements PermissionsWsAction {

  public static final String ACTION = "remove_user";

  private final DbClient dbClient;
  private final PermissionUpdater permissionUpdater;
  private final PermissionChangeBuilder permissionChangeBuilder;

  public RemoveUserAction(DbClient dbClient, PermissionUpdater permissionUpdater, PermissionChangeBuilder permissionChangeBuilder) {
    this.dbClient = dbClient;
    this.permissionUpdater = permissionUpdater;
    this.permissionChangeBuilder = permissionChangeBuilder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setDescription("Remove permission from a user.<br /> " +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br /> " +
        "It requires administration permissions to access.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    createPermissionParameter(action);
    createUserLoginParameter(action);
    createProjectParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(toRemoveUserWsRequest(request));
    response.noContent();
  }

  private void doHandle(RemoveUserWsRequest request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<WsProjectRef> projectRef = newOptionalWsProjectRef(request.getProjectId(), request.getProjectKey());
      validatePermission(request.getPermission(), projectRef);
      PermissionChange permissionChange = permissionChangeBuilder.buildUserPermissionChange(
        dbSession,
        request.getPermission(),
        projectRef,
        request.getLogin());
      permissionUpdater.removePermission(permissionChange);

    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static RemoveUserWsRequest toRemoveUserWsRequest(Request request) {
    return new RemoveUserWsRequest()
      .setPermission(request.mandatoryParam(PARAM_PERMISSION))
      .setLogin(request.mandatoryParam(PARAM_USER_LOGIN))
      .setProjectId(request.param(PARAM_PROJECT_ID))
      .setProjectKey(request.param(PARAM_PROJECT_KEY));
  }
}
