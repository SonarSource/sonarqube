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

import java.util.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.permission.AddProjectCreatorToTemplateWsRequest;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPermission;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectPermissionParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createTemplateParameters;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION_KEY;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class AddProjectCreatorToTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionWsSupport wsSupport;
  private final UserSession userSession;
  private final System2 system;

  public AddProjectCreatorToTemplateAction(DbClient dbClient, PermissionWsSupport wsSupport, UserSession userSession, System2 system) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.userSession = userSession;
    this.system = system;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("add_project_creator_to_template")
      .setDescription("Add a project creator to a permission template.<br>" +
        "Requires the 'Administer' permission.")
      .setSince("6.0")
      .setPost(true)
      .setHandler(this);

    createTemplateParameters(action);
    createProjectPermissionParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(toWsRequest(request));
    response.noContent();
  }

  private void doHandle(AddProjectCreatorToTemplateWsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PermissionTemplateDto template = wsSupport.findTemplate(dbSession, WsTemplateRef.newTemplateRef(
        request.getTemplateId(), request.getOrganization(), request.getTemplateName()));
      checkGlobalAdmin(userSession, template.getOrganizationUuid());

      Optional<PermissionTemplateCharacteristicDto> templatePermission = dbClient.permissionTemplateCharacteristicDao()
        .selectByPermissionAndTemplateId(dbSession, request.getPermission(), template.getId());
      if (templatePermission.isPresent()) {
        updateTemplatePermission(dbSession, templatePermission.get());
      } else {
        addTemplatePermission(dbSession, request, template);
      }
    }
  }

  private void addTemplatePermission(DbSession dbSession, AddProjectCreatorToTemplateWsRequest request, PermissionTemplateDto template) {
    long now = system.now();
    dbClient.permissionTemplateCharacteristicDao().insert(dbSession, new PermissionTemplateCharacteristicDto()
      .setPermission(request.getPermission())
      .setTemplateId(template.getId())
      .setWithProjectCreator(true)
      .setCreatedAt(now)
      .setUpdatedAt(now));
    dbSession.commit();
  }

  private void updateTemplatePermission(DbSession dbSession, PermissionTemplateCharacteristicDto templatePermission) {
    PermissionTemplateCharacteristicDto targetTemplatePermission = templatePermission
      .setUpdatedAt(system.now())
      .setWithProjectCreator(true);
    dbClient.permissionTemplateCharacteristicDao().update(dbSession, targetTemplatePermission);
    dbSession.commit();
  }

  private static AddProjectCreatorToTemplateWsRequest toWsRequest(Request request) {
    AddProjectCreatorToTemplateWsRequest wsRequest = AddProjectCreatorToTemplateWsRequest.builder()
      .setPermission(request.mandatoryParam(PARAM_PERMISSION))
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setOrganization(request.param(PARAM_ORGANIZATION_KEY))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME))
      .build();
    validateProjectPermission(wsRequest.getPermission());
    return wsRequest;
  }
}
