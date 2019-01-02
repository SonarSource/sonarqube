/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.user.UserSession;
import org.sonar.server.usergroups.ws.GroupIdOrAnyone;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.WsParameters.createGroupIdParameter;
import static org.sonar.server.permission.ws.WsParameters.createGroupNameParameter;
import static org.sonar.server.permission.ws.WsParameters.createTemplateParameters;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;

public class RemoveGroupFromTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionWsSupport wsSupport;
  private final UserSession userSession;
  private final WsParameters wsParameters;

  public RemoveGroupFromTemplateAction(DbClient dbClient, PermissionWsSupport wsSupport, UserSession userSession, WsParameters wsParameters) {
    this.dbClient = dbClient;
    this.wsSupport = wsSupport;
    this.userSession = userSession;
    this.wsParameters = wsParameters;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction("remove_group_from_template")
      .setPost(true)
      .setSince("5.2")
      .setDescription("Remove a group from a permission template.<br /> " +
        "The group id or group name must be provided. <br />" +
        "Requires the following permission: 'Administer System'.")
      .setHandler(this);

    createTemplateParameters(action);
    wsParameters.createProjectPermissionParameter(action);
    createGroupIdParameter(action);
    createGroupNameParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String permission = request.mandatoryParam(PARAM_PERMISSION);
      PermissionTemplateDto template = wsSupport.findTemplate(dbSession, WsTemplateRef.fromRequest(request));
      checkGlobalAdmin(userSession, template.getOrganizationUuid());
      GroupIdOrAnyone groupId = wsSupport.findGroup(dbSession, request);
      checkArgument(groupId.getOrganizationUuid().equals(template.getOrganizationUuid()), "Group and template are on different organizations");

      dbClient.permissionTemplateDao().deleteGroupPermission(dbSession, template.getId(), groupId.getId(), permission);
      dbSession.commit();
    }
    response.noContent();
  }
}
