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

import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.OldPermissionQuery;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.permission.ws.PermissionDependenciesFinder;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.user.UserSession;
import org.sonar.server.usergroups.ws.WsGroupRef;
import org.sonarqube.ws.client.permission.AddGroupToTemplateWsRequest;

import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.db.user.GroupMembershipQuery.IN;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateNotAnyoneAndAdminPermission;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPermission;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_TEMPLATE_ID;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createGroupIdParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createGroupNameParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectPermissionParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createTemplateParameters;
import static org.sonar.server.permission.ws.WsTemplateRef.newTemplateRef;
import static org.sonar.server.usergroups.ws.WsGroupRef.newWsGroupRef;

public class AddGroupToTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionDependenciesFinder dependenciesFinder;
  private final UserSession userSession;

  public AddGroupToTemplateAction(DbClient dbClient, PermissionDependenciesFinder dependenciesFinder, UserSession userSession) {
    this.dbClient = dbClient;
    this.dependenciesFinder = dependenciesFinder;
    this.userSession = userSession;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction("add_group_to_template")
      .setPost(true)
      .setSince("5.2")
      .setDescription("Add a group to a permission template.<br /> " +
        "The group id or group name must be provided. <br />" +
        "It requires administration permissions to access.")
      .setHandler(this);

    createTemplateParameters(action);
    createProjectPermissionParameter(action);
    createGroupIdParameter(action);
    createGroupNameParameter(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) {
    checkGlobalAdminUser(userSession);
    doHandle(toAddGroupToTemplateWsRequest(wsRequest));
    wsResponse.noContent();
  }

  private void doHandle(AddGroupToTemplateWsRequest wsRequest) {
    String permission = wsRequest.getPermission();
    Long requestGroupId = wsRequest.getGroupId() == null ? null : Long.valueOf(wsRequest.getGroupId());
    WsGroupRef group = newWsGroupRef(requestGroupId, wsRequest.getGroupName());

    DbSession dbSession = dbClient.openSession(false);
    try {
      validateProjectPermission(permission);
      validateNotAnyoneAndAdminPermission(permission, group.name());

      PermissionTemplateDto template = dependenciesFinder.getTemplate(dbSession, newTemplateRef(wsRequest.getTemplateId(), wsRequest.getTemplateName()));
      GroupDto groupDto = dependenciesFinder.getGroup(dbSession, group);

      if (!groupAlreadyAdded(dbSession, template.getId(), groupDto, permission)) {
        Long groupId = groupDto == null ? null : groupDto.getId();
        dbClient.permissionTemplateDao().insertGroupPermission(dbSession, template.getId(), groupId, permission);
      }
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private boolean groupAlreadyAdded(DbSession dbSession, long templateId, @Nullable GroupDto group, String permission) {
    String groupName = group == null ? ANYONE : group.getName();
    OldPermissionQuery permissionQuery = OldPermissionQuery.builder().membership(IN).permission(permission).build();
    return dbClient.permissionTemplateDao().hasGroup(dbSession, permissionQuery, templateId, groupName);
  }

  private static AddGroupToTemplateWsRequest toAddGroupToTemplateWsRequest(Request request) {
    return new AddGroupToTemplateWsRequest()
      .setPermission(request.mandatoryParam(PARAM_PERMISSION))
      .setGroupId(request.param(PARAM_GROUP_ID))
      .setGroupName(request.param(PARAM_GROUP_NAME))
      .setTemplateId(request.param(PARAM_TEMPLATE_ID))
      .setTemplateName(request.param(PARAM_TEMPLATE_NAME));
  }
}
