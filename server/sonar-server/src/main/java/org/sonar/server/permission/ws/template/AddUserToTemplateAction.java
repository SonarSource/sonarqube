/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.UserId;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectPermissionParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createTemplateParameters;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createUserLoginParameter;
import static org.sonar.server.permission.ws.template.WsTemplateRef.newTemplateRef;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class AddUserToTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionWsSupport wsSupport;
  private final UserSession userSession;

  public AddUserToTemplateAction(DbClient dbClient, PermissionWsSupport wsSupport, UserSession userSession) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.userSession = userSession;
  }

  private static AddUserToTemplateRequest toAddUserToTemplateWsRequest(Request request) {
    return new AddUserToTemplateRequest()
      .setLogin(request.mandatoryParam(PARAM_USER_LOGIN))
      .setPermission(request.mandatoryParam(PARAM_PERMISSION))
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME));
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction("add_user_to_template")
      .setPost(true)
      .setSince("5.2")
      .setDescription("Add a user to a permission template.<br /> " +
        "Requires the following permission: 'Administer System'.")
      .setHandler(this);

    createTemplateParameters(action);
    createProjectPermissionParameter(action);
    createUserLoginParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(toAddUserToTemplateWsRequest(request));
    response.noContent();
  }

  private void doHandle(AddUserToTemplateRequest request) {
    String permission = request.getPermission();
    String userLogin = request.getLogin();

    try (DbSession dbSession = dbClient.openSession(false)) {
      PermissionTemplateDto template = wsSupport.findTemplate(dbSession, newTemplateRef(
        request.getTemplateId(), request.getOrganization(), request.getTemplateName()));
      OrganizationDto organizationDto = wsSupport.findOrganization(dbSession, request.getOrganization());
      checkGlobalAdmin(userSession, organizationDto.getUuid());
      UserId user = wsSupport.findUser(dbSession, userLogin);
      wsSupport.checkMembership(dbSession, organizationDto, user);

      if (!isUserAlreadyAdded(dbSession, organizationDto, template.getId(), userLogin, permission)) {
        dbClient.permissionTemplateDao().insertUserPermission(dbSession, template.getId(), user.getId(), permission);
        dbSession.commit();
      }
    }
  }

  private boolean isUserAlreadyAdded(DbSession dbSession, OrganizationDto organizationDto, long templateId, String userLogin, String permission) {
    PermissionQuery permissionQuery = PermissionQuery.builder().setOrganizationUuid(organizationDto.getUuid()).setPermission(permission).build();
    List<String> usersWithPermission = dbClient.permissionTemplateDao().selectUserLoginsByQueryAndTemplate(dbSession, permissionQuery, templateId);
    return usersWithPermission.stream().anyMatch(s -> s.equals(userLogin));
  }

  private static class AddUserToTemplateRequest {
    private String login;
    private String permission;
    private String templateId;
    private String organization;
    private String templateName;

    public String getLogin() {
      return login;
    }

    public AddUserToTemplateRequest setLogin(String login) {
      this.login = requireNonNull(login);
      return this;
    }

    public String getPermission() {
      return permission;
    }

    public AddUserToTemplateRequest setPermission(String permission) {
      this.permission = requireNonNull(permission);
      return this;
    }

    @CheckForNull
    public String getTemplateId() {
      return templateId;
    }

    public AddUserToTemplateRequest setTemplateId(@Nullable String templateId) {
      this.templateId = templateId;
      return this;
    }

    @CheckForNull
    public String getTemplateName() {
      return templateName;
    }

    public AddUserToTemplateRequest setTemplateName(@Nullable String templateName) {
      this.templateName = templateName;
      return this;
    }

    @CheckForNull
    public String getOrganization() {
      return organization;
    }

    public AddUserToTemplateRequest setOrganization(@Nullable String s) {
      this.organization = s;
      return this;
    }
  }
}
