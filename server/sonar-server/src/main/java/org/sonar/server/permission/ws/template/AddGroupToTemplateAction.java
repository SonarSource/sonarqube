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
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.user.UserSession;
import org.sonar.server.usergroups.ws.GroupIdOrAnyone;

import static java.lang.String.format;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createGroupIdParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createGroupNameParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectPermissionParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createTemplateParameters;
import static org.sonar.server.permission.ws.template.WsTemplateRef.fromRequest;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;

public class AddGroupToTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionWsSupport support;
  private final UserSession userSession;

  public AddGroupToTemplateAction(DbClient dbClient, PermissionWsSupport support, UserSession userSession) {
    this.dbClient = dbClient;
    this.support = support;
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
        "Requires the following permission: 'Administer System'.")
      .setHandler(this);

    createTemplateParameters(action);
    createProjectPermissionParameter(action);
    createGroupIdParameter(action);
    createGroupNameParameter(action);
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String permission = request.mandatoryParam(PARAM_PERMISSION);
      GroupIdOrAnyone groupId = support.findGroup(dbSession, request);
      checkRequest(!SYSTEM_ADMIN.equals(permission) || !groupId.isAnyone(),
        format("It is not possible to add the '%s' permission to the group 'Anyone'.", permission));

      PermissionTemplateDto template = support.findTemplate(dbSession, fromRequest(request));
      checkGlobalAdmin(userSession, template.getOrganizationUuid());

      if (!groupAlreadyAdded(dbSession, template.getId(), permission, groupId)) {
        dbClient.permissionTemplateDao().insertGroupPermission(dbSession, template.getId(), groupId.getId(), permission);
        dbSession.commit();
      }
    }
    response.noContent();
  }

  private boolean groupAlreadyAdded(DbSession dbSession, long templateId, String permission, GroupIdOrAnyone group) {
    return dbClient.permissionTemplateDao().hasGroupsWithPermission(dbSession, templateId, permission, group.getId());
  }
}
