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

import java.util.Set;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.permission.DeleteTemplateWsRequest;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createTemplateParameters;
import static org.sonar.server.permission.ws.template.WsTemplateRef.newTemplateRef;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class DeleteTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionWsSupport finder;
  private final DefaultPermissionTemplateFinder defaultPermissionTemplateFinder;

  public DeleteTemplateAction(DbClient dbClient, UserSession userSession, PermissionWsSupport support, DefaultPermissionTemplateFinder defaultPermissionTemplateFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.finder = support;
    this.defaultPermissionTemplateFinder = defaultPermissionTemplateFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("delete_template")
      .setDescription("Delete a permission template.<br />" +
        "It requires administration permissions to access.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    createTemplateParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(toDeleteTemplateWsRequest(request));
    response.noContent();
  }

  private void doHandle(DeleteTemplateWsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PermissionTemplateDto template = finder.findTemplate(dbSession, newTemplateRef(
        request.getTemplateId(), request.getOrganization(), request.getTemplateName()));
      checkGlobalAdmin(userSession, template.getOrganizationUuid());

      checkTemplateUuidIsNotDefault(template.getUuid());
      dbClient.permissionTemplateDao().deleteById(dbSession, template.getId());
      dbSession.commit();
    }
  }

  private static DeleteTemplateWsRequest toDeleteTemplateWsRequest(Request request) {
    return new DeleteTemplateWsRequest()
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setOrganization(request.param(PARAM_ORGANIZATION_KEY))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME));
  }

  private void checkTemplateUuidIsNotDefault(String key) {
    Set<String> defaultTemplateUuids = defaultPermissionTemplateFinder.getDefaultTemplateUuids();
    checkRequest(!defaultTemplateUuids.contains(key), "It is not possible to delete a default template");
  }
}
