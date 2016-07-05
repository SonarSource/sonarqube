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
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.ApplyPermissionTemplateQuery;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.permission.ws.PermissionDependenciesFinder;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonarqube.ws.client.permission.ApplyTemplateWsRequest;

import static java.util.Collections.singletonList;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectParameters;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createTemplateParameters;
import static org.sonar.server.permission.ws.WsProjectRef.newWsProjectRef;
import static org.sonar.server.permission.ws.WsTemplateRef.newTemplateRef;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class ApplyTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionService permissionService;
  private final PermissionDependenciesFinder finder;

  public ApplyTemplateAction(DbClient dbClient, PermissionService permissionService, PermissionDependenciesFinder finder) {
    this.dbClient = dbClient;
    this.permissionService = permissionService;
    this.finder = finder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("apply_template")
      .setDescription("Apply a permission template to one project.<br />" +
        "The project id or project key must be provided.<br />" +
        "The template id or name must be provided.<br />" +
        "It requires administration permissions to access.")
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

  private void doHandle(ApplyTemplateWsRequest request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      PermissionTemplateDto template = finder.getTemplate(dbSession, newTemplateRef(request.getTemplateId(), request.getTemplateName()));
      ComponentDto project = finder.getRootComponentOrModule(dbSession, newWsProjectRef(request.getProjectId(), request.getProjectKey()));

      ApplyPermissionTemplateQuery query = ApplyPermissionTemplateQuery.create(
        template.getUuid(),
        singletonList(project.key()));
      permissionService.applyPermissionTemplate(query);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static ApplyTemplateWsRequest toApplyTemplateWsRequest(Request request) {
    return new ApplyTemplateWsRequest()
      .setProjectId(request.param(PARAM_PROJECT_ID))
      .setProjectKey(request.param(PARAM_PROJECT_KEY))
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME));
  }
}
