/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
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
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.permission.ws.PermissionDependenciesFinder;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.user.UserSession;
import org.sonar.server.usergroups.ws.WsGroupRef;
import org.sonarqube.ws.client.permission.RemoveGroupFromTemplateWsRequest;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPermission;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createGroupIdParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createGroupNameParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectPermissionParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createTemplateParameters;
import static org.sonar.server.permission.ws.WsTemplateRef.newTemplateRef;
import static org.sonar.server.usergroups.ws.WsGroupRef.newWsGroupRef;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;

public class RemoveGroupFromTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionDependenciesFinder dependenciesFinder;
  private final UserSession userSession;

  public RemoveGroupFromTemplateAction(DbClient dbClient, PermissionDependenciesFinder dependenciesFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.dependenciesFinder = dependenciesFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction("remove_group_from_template")
      .setPost(true)
      .setSince("5.2")
      .setDescription("Remove a group from a permission template.<br /> " +
        "The group id or group name must be provided. <br />" +
        "It requires administration permissions to access.")
      .setHandler(this);

    createTemplateParameters(action);
    createProjectPermissionParameter(action);
    createGroupIdParameter(action);
    createGroupNameParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    checkGlobalAdminUser(userSession);
    doHandle(toRemoveGroupFromTemplateWsRequest(request));
    response.noContent();
  }

  private void doHandle(RemoveGroupFromTemplateWsRequest request) {
    String permission = request.getPermission();
    Long groupIdInRequest = request.getGroupId() == null ? null : Long.valueOf(request.getGroupId());
    WsGroupRef group = newWsGroupRef(groupIdInRequest, request.getGroupName());

    DbSession dbSession = dbClient.openSession(false);
    try {
      validateProjectPermission(permission);
      PermissionTemplateDto template = dependenciesFinder.getTemplate(dbSession, newTemplateRef(request.getTemplateId(), request.getTemplateName()));
      GroupDto groupDto = dependenciesFinder.getGroup(dbSession, group);

      Long groupId = groupDto == null ? null : groupDto.getId();
      dbClient.permissionTemplateDao().deleteGroupPermission(dbSession, template.getId(), groupId, permission);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static RemoveGroupFromTemplateWsRequest toRemoveGroupFromTemplateWsRequest(Request request) {
    return new RemoveGroupFromTemplateWsRequest()
      .setPermission(request.mandatoryParam(PARAM_PERMISSION))
      .setGroupId(request.param(PARAM_GROUP_ID))
      .setGroupName(request.param(PARAM_GROUP_NAME))
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME));
  }
}
