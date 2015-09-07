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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_USER_LOGIN;
import static org.sonar.server.permission.ws.WsPermissionParameters.createProjectPermissionParameter;
import static org.sonar.server.permission.ws.WsPermissionParameters.createTemplateParameters;
import static org.sonar.server.permission.ws.WsPermissionParameters.createUserLoginParameter;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPermission;

public class RemoveUserFromTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionDependenciesFinder dependenciesFinder;
  private final UserSession userSession;

  public RemoveUserFromTemplateAction(DbClient dbClient, PermissionDependenciesFinder dependenciesFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.dependenciesFinder = dependenciesFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction("remove_user_from_template")
      .setPost(true)
      .setSince("5.2")
      .setDescription("Remove a user from a permission template.<br /> " +
        "It requires administration permissions to access.")
      .setHandler(this);

    createTemplateParameters(action);
    createProjectPermissionParameter(action);
    createUserLoginParameter(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    checkGlobalAdminUser(userSession);

    String permission = wsRequest.mandatoryParam(PARAM_PERMISSION);
    String userLogin = wsRequest.mandatoryParam(PARAM_USER_LOGIN);

    DbSession dbSession = dbClient.openSession(false);
    try {
      validateProjectPermission(permission);
      PermissionTemplateDto template = dependenciesFinder.getTemplate(dbSession, WsTemplateRef.fromRequest(wsRequest));
      UserDto user = dependenciesFinder.getUser(dbSession, userLogin);

      dbClient.permissionTemplateDao().deleteUserPermission(dbSession, template.getId(), user.getId(), permission);
      dbSession.commit();
    } finally {
      dbClient.closeSession(dbSession);
    }

    wsResponse.noContent();
  }
}
