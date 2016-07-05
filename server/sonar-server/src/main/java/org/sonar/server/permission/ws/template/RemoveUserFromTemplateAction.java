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
package org.sonar.server.permission.ws.template;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.permission.ws.PermissionDependenciesFinder;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.permission.ws.WsTemplateRef;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.permission.RemoveUserFromTemplateWsRequest;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPermission;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectPermissionParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createTemplateParameters;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createUserLoginParameter;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

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
  public void handle(Request request, Response response) throws Exception {
    doHandle(toRemoveUserFromTemplateWsRequest(request));
    response.noContent();
  }

  private void doHandle(RemoveUserFromTemplateWsRequest request) {
    checkGlobalAdminUser(userSession);

    String permission = request.getPermission();
    String userLogin = request.getLogin();

    DbSession dbSession = dbClient.openSession(false);
    try {
      validateProjectPermission(permission);
      PermissionTemplateDto template = dependenciesFinder.getTemplate(dbSession, WsTemplateRef.newTemplateRef(request.getTemplateId(), request.getTemplateName()));
      UserDto user = dependenciesFinder.getUser(dbSession, userLogin);

      dbClient.permissionTemplateDao().deleteUserPermission(dbSession, template.getId(), user.getId(), permission);
      dbSession.commit();
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static RemoveUserFromTemplateWsRequest toRemoveUserFromTemplateWsRequest(Request request) {
    return new RemoveUserFromTemplateWsRequest()
      .setPermission(request.mandatoryParam(PARAM_PERMISSION))
      .setLogin(request.mandatoryParam(PARAM_USER_LOGIN))
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME));
  }
}
