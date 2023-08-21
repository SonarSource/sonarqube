/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.server.permission.GroupUuidOrAnyone;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static org.sonar.core.permission.GlobalPermissions.SYSTEM_ADMIN;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.WsParameters.createGroupIdParameter;
import static org.sonar.server.permission.ws.WsParameters.createGroupNameParameter;
import static org.sonar.server.permission.ws.WsParameters.createTemplateParameters;
import static org.sonar.server.permission.ws.template.WsTemplateRef.fromRequest;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_GROUP_NAME;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;

public class AddGroupToTemplateAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final PermissionWsSupport support;
  private final UserSession userSession;
  private final WsParameters wsParameters;
  private static final Logger logger = Loggers.get(AddGroupToTemplateAction.class);

  public AddGroupToTemplateAction(DbClient dbClient, PermissionWsSupport support, UserSession userSession, WsParameters wsParameters) {
    this.dbClient = dbClient;
    this.support = support;
    this.userSession = userSession;
    this.wsParameters = wsParameters;
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
      .setChangelog(
        new Change("8.4", "Parameter 'groupId' is deprecated. Format changes from integer to string. Use 'groupName' instead."))
      .setHandler(this);

    createTemplateParameters(action);
    wsParameters.createProjectPermissionParameter(action);
    createGroupIdParameter(action);
    createGroupNameParameter(action);
  }

  @Override
  public void handle(Request request, Response response) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String permission = request.mandatoryParam(PARAM_PERMISSION);
      GroupUuidOrAnyone group = support.findGroup(dbSession, request);
      checkRequest(!SYSTEM_ADMIN.equals(permission) || !group.isAnyone(),
        format("It is not possible to add the '%s' permission to the group 'Anyone'.", permission));

      PermissionTemplateDto template = support.findTemplate(dbSession, fromRequest(request));
      checkGlobalAdmin(userSession, template.getOrganizationUuid());
      logger.info("Add Group to Permission Template Request :: organizationUuid : {}, templateName: {} and permissionType: {}, user: {}",
              template.getOrganizationUuid(), template.getName(), permission, userSession.getLogin());
      if (!groupAlreadyAdded(dbSession, template.getUuid(), permission, group)) {
        dbClient.permissionTemplateDao().insertGroupPermission(dbSession, template.getUuid(), group.getUuid(), permission,
          template.getName(), request.param(PARAM_GROUP_NAME));
        dbSession.commit();
      }
    }
    response.noContent();
  }

  private boolean groupAlreadyAdded(DbSession dbSession, String templateUuid, String permission, GroupUuidOrAnyone group) {
    return dbClient.permissionTemplateDao().hasGroupsWithPermission(dbSession, templateUuid, permission, group.getUuid());
  }
}
