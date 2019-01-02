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

import java.util.Collections;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.PermissionTemplateService;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.user.UserSession;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.ProjectWsRef.newWsProjectRef;
import static org.sonar.server.permission.ws.WsParameters.createProjectParameters;
import static org.sonar.server.permission.ws.WsParameters.createTemplateParameters;
import static org.sonar.server.permission.ws.template.WsTemplateRef.newTemplateRef;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class ApplyTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionTemplateService permissionTemplateService;
  private final PermissionWsSupport wsSupport;

  public ApplyTemplateAction(DbClient dbClient, UserSession userSession, PermissionTemplateService permissionTemplateService,
    PermissionWsSupport wsSupport) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.permissionTemplateService = permissionTemplateService;
    this.wsSupport = wsSupport;
  }

  private static ApplyTemplateRequest toApplyTemplateWsRequest(Request request) {
    return new ApplyTemplateRequest()
      .setProjectId(request.param(PARAM_PROJECT_ID))
      .setProjectKey(request.param(PARAM_PROJECT_KEY))
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME))
      .setOrganization(request.param(PARAM_ORGANIZATION));
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("apply_template")
      .setDescription("Apply a permission template to one project.<br>" +
        "The project id or project key must be provided.<br>" +
        "The template id or name must be provided.<br>" +
        "Requires the following permission: 'Administer System'.")
      .setPost(true)
      .setSince("5.2")
      .setHandler(this);

    createTemplateParameters(action);
    createProjectParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(toApplyTemplateWsRequest(request));
    response.noContent();
  }

  private void doHandle(ApplyTemplateRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PermissionTemplateDto template = wsSupport.findTemplate(dbSession, newTemplateRef(
        request.getTemplateId(), request.getOrganization(), request.getTemplateName()));

      ComponentDto project = wsSupport.getRootComponentOrModule(dbSession, newWsProjectRef(request.getProjectId(), request.getProjectKey()));
      checkGlobalAdmin(userSession, template.getOrganizationUuid());

      permissionTemplateService.applyAndCommit(dbSession, template, Collections.singletonList(project));
    }
  }

  private static class ApplyTemplateRequest {
    private String projectId;
    private String projectKey;
    private String templateId;
    private String organization;
    private String templateName;

    @CheckForNull
    public String getProjectId() {
      return projectId;
    }

    public ApplyTemplateRequest setProjectId(@Nullable String projectId) {
      this.projectId = projectId;
      return this;
    }

    @CheckForNull
    public String getProjectKey() {
      return projectKey;
    }

    public ApplyTemplateRequest setProjectKey(@Nullable String projectKey) {
      this.projectKey = projectKey;
      return this;
    }

    @CheckForNull
    public String getTemplateId() {
      return templateId;
    }

    public ApplyTemplateRequest setTemplateId(@Nullable String templateId) {
      this.templateId = templateId;
      return this;
    }

    @CheckForNull
    public String getOrganization() {
      return organization;
    }

    public ApplyTemplateRequest setOrganization(@Nullable String s) {
      this.organization = s;
      return this;
    }
    @CheckForNull
    public String getTemplateName() {
      return templateName;
    }

    public ApplyTemplateRequest setTemplateName(@Nullable String templateName) {
      this.templateName = templateName;
      return this;
    }
  }
}
