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
package org.sonar.server.permission.ws.template;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.UserId;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.permission.ws.RequestValidator;
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.WsParameters.createTemplateParameters;
import static org.sonar.server.permission.ws.WsParameters.createUserLoginParameter;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class RemoveUserFromTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionWsSupport wsSupport;
  private final UserSession userSession;
  private final WsParameters wsParameters;
  private final RequestValidator requestValidator;

  public RemoveUserFromTemplateAction(DbClient dbClient, PermissionWsSupport wsSupport, UserSession userSession, WsParameters wsParameters, RequestValidator requestValidator) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.userSession = userSession;
    this.wsParameters = wsParameters;
    this.requestValidator = requestValidator;
  }

  private static RemoveUserFromTemplateRequest toRemoveUserFromTemplateWsRequest(Request request) {
    return new RemoveUserFromTemplateRequest()
      .setPermission(request.mandatoryParam(PARAM_PERMISSION))
      .setLogin(request.mandatoryParam(PARAM_USER_LOGIN))
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME));
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction("remove_user_from_template")
      .setPost(true)
      .setSince("5.2")
      .setDescription("Remove a user from a permission template.<br /> " +
        "Requires the following permission: 'Administer System'.")
      .setHandler(this);

    createTemplateParameters(action);
    wsParameters.createProjectPermissionParameter(action);
    createUserLoginParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(toRemoveUserFromTemplateWsRequest(request));
    response.noContent();
  }

  private void doHandle(RemoveUserFromTemplateRequest request) {
    String permission = request.getPermission();
    String userLogin = request.getLogin();

    try (DbSession dbSession = dbClient.openSession(false)) {
      requestValidator.validateProjectPermission(permission);
      PermissionTemplateDto template = wsSupport.findTemplate(dbSession, WsTemplateRef.newTemplateRef(
        request.getTemplateId(), request.getOrganization(), request.getTemplateName()));
      checkGlobalAdmin(userSession, template.getOrganizationUuid());

      UserId user = wsSupport.findUser(dbSession, userLogin);

      dbClient.permissionTemplateDao().deleteUserPermission(dbSession, template.getId(), user.getId(), permission);
      dbSession.commit();
    }
  }

  private static class RemoveUserFromTemplateRequest {
    private String login;
    private String permission;
    private String templateId;
    private String organization;
    private String templateName;

    public String getLogin() {
      return login;
    }

    public RemoveUserFromTemplateRequest setLogin(String login) {
      this.login = requireNonNull(login);
      return this;
    }

    public String getPermission() {
      return permission;
    }

    public RemoveUserFromTemplateRequest setPermission(String permission) {
      this.permission = requireNonNull(permission);
      return this;
    }

    @CheckForNull
    public String getTemplateId() {
      return templateId;
    }

    public RemoveUserFromTemplateRequest setTemplateId(@Nullable String templateId) {
      this.templateId = templateId;
      return this;
    }

    @CheckForNull
    public String getOrganization() {
      return organization;
    }

    public RemoveUserFromTemplateRequest setOrganization(@Nullable String s) {
      this.organization = s;
      return this;
    }

    @CheckForNull
    public String getTemplateName() {
      return templateName;
    }

    public RemoveUserFromTemplateRequest setTemplateName(@Nullable String templateName) {
      this.templateName = templateName;
      return this;
    }
  }
}
