/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDao;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.RequestValidator;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.WsParameters.createTemplateParameters;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class RemoveProjectCreatorFromTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionWsSupport wsSupport;
  private final UserSession userSession;
  private final System2 system;
  private final WsParameters wsParameters;
  private final RequestValidator requestValidator;

  public RemoveProjectCreatorFromTemplateAction(DbClient dbClient, PermissionWsSupport wsSupport, UserSession userSession, System2 system,
    WsParameters wsParameters, RequestValidator requestValidator) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.userSession = userSession;
    this.system = system;
    this.wsParameters = wsParameters;
    this.requestValidator = requestValidator;
  }

  private RemoveProjectCreatorFromTemplateRequest toWsRequest(Request request) {
    RemoveProjectCreatorFromTemplateRequest wsRequest = RemoveProjectCreatorFromTemplateRequest.builder()
      .setPermission(request.mandatoryParam(PARAM_PERMISSION))
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME))
      .build();
    requestValidator.validateProjectPermission(wsRequest.getPermission());
    return wsRequest;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("remove_project_creator_from_template")
      .setDescription("Remove a project creator from a permission template.<br>" +
        "Requires the following permission: 'Administer System'.")
      .setSince("6.0")
      .setPost(true)
      .setHandler(this);

    createTemplateParameters(action);
    wsParameters.createProjectPermissionParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(toWsRequest(request));
    response.noContent();
  }

  private void doHandle(RemoveProjectCreatorFromTemplateRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PermissionTemplateDto template = wsSupport.findTemplate(dbSession, WsTemplateRef.newTemplateRef(
        request.getTemplateId(), request.getOrganization(), request.getTemplateName()));
      checkGlobalAdmin(userSession, template.getOrganizationUuid());

      PermissionTemplateCharacteristicDao dao = dbClient.permissionTemplateCharacteristicDao();
      dao.selectByPermissionAndTemplateId(dbSession, request.getPermission(), template.getUuid())
        .ifPresent(permissionTemplateCharacteristicDto -> updateTemplateCharacteristic(dbSession, permissionTemplateCharacteristicDto,
          template.getName()));
    }
  }

  private void updateTemplateCharacteristic(DbSession dbSession, PermissionTemplateCharacteristicDto templatePermission, String templateName) {
    PermissionTemplateCharacteristicDto targetTemplatePermission = templatePermission
      .setUpdatedAt(system.now())
      .setWithProjectCreator(false);
    dbClient.permissionTemplateCharacteristicDao().update(dbSession, targetTemplatePermission, templateName);
    dbSession.commit();
  }

  private static class RemoveProjectCreatorFromTemplateRequest {
    private final String templateId;
    private final String templateName;
    private final String permission;
    private final String organization;

    private RemoveProjectCreatorFromTemplateRequest(Builder builder) {
      this.templateId = builder.templateId;
      this.templateName = builder.templateName;
      this.permission = requireNonNull(builder.permission);
      this.organization = requireNonNull(builder.organization);
    }

    @CheckForNull
    public String getTemplateId() {
      return templateId;
    }

    @CheckForNull
    public String getTemplateName() {
      return templateName;
    }

    public String getPermission() {
      return permission;
    }

    public String getOrganization() {
      return organization;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  public static class Builder {
    private String templateId;
    private String templateName;
    private String permission;
    private String organization;

    private Builder() {
      // enforce method constructor
    }

    public Builder setTemplateId(@Nullable String templateId) {
      this.templateId = templateId;
      return this;
    }

    public Builder setTemplateName(@Nullable String templateName) {
      this.templateName = templateName;
      return this;
    }

    public Builder setPermission(@Nullable String permission) {
      this.permission = permission;
      return this;
    }

    public Builder setOrganization(@Nullable String s) {
      this.organization = s;
      return this;
    }

    public RemoveProjectCreatorFromTemplateRequest build() {
      return new RemoveProjectCreatorFromTemplateRequest(this);
    }
  }
}
