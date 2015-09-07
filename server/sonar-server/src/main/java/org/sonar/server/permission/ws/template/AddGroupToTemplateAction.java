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

import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.permission.ws.PermissionDependenciesFinder;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.permission.ws.WsGroupRef;
import org.sonar.server.permission.ws.WsTemplateRef;
import org.sonar.server.user.UserSession;

import static org.sonar.api.security.DefaultGroups.ANYONE;
import static org.sonar.db.user.GroupMembershipQuery.IN;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateNotAnyoneAndAdminPermission;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPermission;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.WsPermissionParameters.createGroupIdParameter;
import static org.sonar.server.permission.ws.WsPermissionParameters.createGroupNameParameter;
import static org.sonar.server.permission.ws.WsPermissionParameters.createProjectPermissionParameter;
import static org.sonar.server.permission.ws.WsPermissionParameters.createTemplateParameters;

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
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    checkGlobalAdminUser(userSession);

    String permission = wsRequest.mandatoryParam(PARAM_PERMISSION);
    WsGroupRef group = WsGroupRef.fromRequest(wsRequest);

    DbSession dbSession = dbClient.openSession(false);
    try {
      validateProjectPermission(permission);
      validateNotAnyoneAndAdminPermission(permission, group.name());

      PermissionTemplateDto template = dependenciesFinder.getTemplate(dbSession, WsTemplateRef.fromRequest(wsRequest));
      GroupDto groupDto = dependenciesFinder.getGroup(dbSession, group);

      if (!groupAlreadyAdded(dbSession, template.getId(), groupDto, permission)) {
        Long groupId = groupDto == null ? null : groupDto.getId();
        dbClient.permissionTemplateDao().insertGroupPermission(dbSession, template.getId(), groupId, permission);
      }
    } finally {
      dbClient.closeSession(dbSession);
    }

    wsResponse.noContent();
  }

  private boolean groupAlreadyAdded(DbSession dbSession, long templateId, @Nullable GroupDto group, String permission) {
    String groupName = group == null ? ANYONE : group.getName();
    PermissionQuery permissionQuery = PermissionQuery.builder().membership(IN).permission(permission).build();
    return dbClient.permissionTemplateDao().hasGroup(dbSession, permissionQuery, templateId, groupName);
  }
}
