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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.DefaultTemplates;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.user.UserSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createTemplateParameters;
import static org.sonar.server.permission.ws.template.WsTemplateRef.newTemplateRef;
import static org.sonar.server.ws.WsUtils.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class DeleteTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionWsSupport finder;
  private final DefaultTemplatesResolver defaultTemplatesResolver;

  public DeleteTemplateAction(DbClient dbClient, UserSession userSession, PermissionWsSupport support, DefaultTemplatesResolver defaultTemplatesResolver) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.finder = support;
    this.defaultTemplatesResolver = defaultTemplatesResolver;
  }

  private static DeleteTemplateRequest toDeleteTemplateWsRequest(Request request) {
    return new DeleteTemplateRequest()
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setOrganization(request.param(PARAM_ORGANIZATION))
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

    createTemplateParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();
    doHandle(toDeleteTemplateWsRequest(request));
    response.noContent();
  }

  private void doHandle(DeleteTemplateRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PermissionTemplateDto template = finder.findTemplate(dbSession, newTemplateRef(
        request.getTemplateId(), request.getOrganization(), request.getTemplateName()));
      checkGlobalAdmin(userSession, template.getOrganizationUuid());

      DefaultTemplates defaultTemplates = retrieveDefaultTemplates(dbSession, template);

      checkTemplateUuidIsNotDefault(template, defaultTemplates);
      dbClient.permissionTemplateDao().deleteById(dbSession, template.getId());
      updateViewDefaultTemplateWhenGovernanceIsNotInstalled(dbSession, template, defaultTemplates);

      dbSession.commit();
    }
  }

  /**
   * The default template for view can be removed when Governance is not installed. To avoid keeping a reference
   * to a non existing template, we update the default templates.
   */
  private void updateViewDefaultTemplateWhenGovernanceIsNotInstalled(DbSession dbSession, PermissionTemplateDto template, DefaultTemplates defaultTemplates) {
    String viewDefaultTemplateUuid = defaultTemplates.getViewUuid();
    if (viewDefaultTemplateUuid != null && viewDefaultTemplateUuid.equals(template.getUuid())) {
      defaultTemplates.setViewUuid(null);
      dbClient.organizationDao().setDefaultTemplates(dbSession, template.getOrganizationUuid(), defaultTemplates);
    }
  }

  private DefaultTemplates retrieveDefaultTemplates(DbSession dbSession, PermissionTemplateDto template) {
    return checkFoundWithOptional(
      dbClient.organizationDao().getDefaultTemplates(dbSession, template.getOrganizationUuid()),
      "Can't find default templates of Organization with uuid '%s' to which template with uuid '%s' belongs",
      template.getOrganizationUuid(), template.getUuid());
  }

  private void checkTemplateUuidIsNotDefault(PermissionTemplateDto template, DefaultTemplates defaultTemplates) {
    DefaultTemplatesResolverImpl.ResolvedDefaultTemplates resolvedDefaultTemplates = defaultTemplatesResolver.resolve(defaultTemplates);
    checkRequest(!resolvedDefaultTemplates.getProject().equals(template.getUuid()),
      "It is not possible to delete the default permission template for projects");
    resolvedDefaultTemplates.getView()
      .ifPresent(viewDefaultTemplateUuid -> checkRequest(
        !viewDefaultTemplateUuid.equals(template.getUuid()),
        "It is not possible to delete the default permission template for views"));
  }

  private static class DeleteTemplateRequest {
    private String templateId;
    private String organization;
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
    public String getOrganization() {
      return organization;
    }

    public DeleteTemplateRequest setOrganization(@Nullable String s) {
      this.organization = s;
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
