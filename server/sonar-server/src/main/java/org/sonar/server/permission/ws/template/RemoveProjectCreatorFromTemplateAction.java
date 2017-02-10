/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDao;
import org.sonar.db.permission.template.PermissionTemplateCharacteristicDto;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.client.permission.RemoveProjectCreatorFromTemplateWsRequest;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPermission;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectPermissionParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createTemplateParameters;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class RemoveProjectCreatorFromTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionWsSupport wsSupport;
  private final UserSession userSession;
  private final System2 system;

  public RemoveProjectCreatorFromTemplateAction(DbClient dbClient, PermissionWsSupport wsSupport, UserSession userSession, System2 system) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.userSession = userSession;
    this.system = system;
  }

  private static RemoveProjectCreatorFromTemplateWsRequest toWsRequest(Request request) {
    RemoveProjectCreatorFromTemplateWsRequest wsRequest = RemoveProjectCreatorFromTemplateWsRequest.builder()
      .setPermission(request.mandatoryParam(PARAM_PERMISSION))
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setOrganization(request.param(PARAM_ORGANIZATION))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME))
      .build();
    validateProjectPermission(wsRequest.getPermission());
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
    createProjectPermissionParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    doHandle(toWsRequest(request));
    response.noContent();
  }

  private void doHandle(RemoveProjectCreatorFromTemplateWsRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PermissionTemplateDto template = wsSupport.findTemplate(dbSession, WsTemplateRef.newTemplateRef(
        request.getTemplateId(), request.getOrganization(), request.getTemplateName()));
      checkGlobalAdmin(userSession, template.getOrganizationUuid());

      PermissionTemplateCharacteristicDao dao = dbClient.permissionTemplateCharacteristicDao();
      dao.selectByPermissionAndTemplateId(dbSession, request.getPermission(), template.getId())
        .ifPresent(permissionTemplateCharacteristicDto -> updateTemplateCharacteristic(dbSession, permissionTemplateCharacteristicDto));
    }
  }

  private void updateTemplateCharacteristic(DbSession dbSession, PermissionTemplateCharacteristicDto templatePermission) {
    PermissionTemplateCharacteristicDto targetTemplatePermission = templatePermission
      .setUpdatedAt(system.now())
      .setWithProjectCreator(false);
    dbClient.permissionTemplateCharacteristicDao().update(dbSession, targetTemplatePermission);
    dbSession.commit();
  }
}
