/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import org.sonar.server.common.permission.DefaultTemplatesResolver;
import org.sonar.server.common.permission.DefaultTemplatesResolver.ResolvedDefaultTemplates;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.user.UserSession;

import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.template.WsTemplateRef.newTemplateRef;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class DeleteTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionWsSupport wsSupport;
  private final DefaultTemplatesResolver defaultTemplatesResolver;

  public DeleteTemplateAction(DbClient dbClient, UserSession userSession, PermissionWsSupport support,
    DefaultTemplatesResolver defaultTemplatesResolver) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.wsSupport = support;
    this.defaultTemplatesResolver = defaultTemplatesResolver;
  }

  private static DeleteTemplateRequest toDeleteTemplateWsRequest(Request request) {
    return new DeleteTemplateRequest()
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME));
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("delete_template")
      .setDescription("Delete a permission template.<br />" +
        "Requires the following permission: 'Administer System'.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    WsParameters.createTemplateParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    doHandle(toDeleteTemplateWsRequest(request));
    response.noContent();
  }

  private void doHandle(DeleteTemplateRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PermissionTemplateDto template = wsSupport.findTemplate(dbSession, newTemplateRef(request.getTemplateId(), request.getTemplateName()));
      checkGlobalAdmin(userSession);

      checkTemplateUuidIsNotDefault(dbSession, template);
      dbClient.permissionTemplateDao().deleteByUuid(dbSession, template.getUuid(), template.getName());
      dbSession.commit();
    }
  }

  private void checkTemplateUuidIsNotDefault(DbSession dbSession, PermissionTemplateDto template) {
    ResolvedDefaultTemplates resolvedDefaultTemplates = defaultTemplatesResolver.resolve(dbSession);
    checkRequest(!resolvedDefaultTemplates.getProject().equals(template.getUuid()),
      "It is not possible to delete the default permission template for projects");
    resolvedDefaultTemplates.getApplication()
      .ifPresent(defaultApplicationTemplate -> checkRequest(
        !defaultApplicationTemplate.equals(template.getUuid()),
        "It is not possible to delete the default permission template for applications"));
    resolvedDefaultTemplates.getPortfolio()
      .ifPresent(defaultPortfolioTemplate -> checkRequest(
        !defaultPortfolioTemplate.equals(template.getUuid()),
        "It is not possible to delete the default permission template for portfolios"));
  }

  private static class DeleteTemplateRequest {
    private String templateId;
    private String templateName;

    @CheckForNull
    public String getTemplateId() {
      return templateId;
    }

    public DeleteTemplateRequest setTemplateId(@Nullable String templateId) {
      this.templateId = templateId;
      return this;
    }

    @CheckForNull
    public String getTemplateName() {
      return templateName;
    }

    public DeleteTemplateRequest setTemplateName(@Nullable String templateName) {
      this.templateName = templateName;
      return this;
    }
  }
}
