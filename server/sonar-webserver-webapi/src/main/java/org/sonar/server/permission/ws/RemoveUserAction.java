/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.permission.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserId;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.common.permission.Operation;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.common.permission.UserPermissionChange;
import org.sonar.server.user.UserSession;

import static java.util.Collections.singletonList;
import static org.sonar.server.permission.ws.WsParameters.createProjectParameters;
import static org.sonar.server.permission.ws.WsParameters.createUserLoginParameter;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class RemoveUserAction implements PermissionsWsAction {

  public static final String ACTION = "remove_user";

  private static final Logger logger = LoggerFactory.getLogger(RemoveUserAction.class);

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionUpdater<UserPermissionChange> permissionUpdater;
  private final PermissionWsSupport wsSupport;
  private final WsParameters wsParameters;
  private final PermissionService permissionService;
  private final ManagedInstanceChecker managedInstanceChecker;

  public RemoveUserAction(DbClient dbClient, UserSession userSession, PermissionUpdater<UserPermissionChange> permissionUpdater, PermissionWsSupport wsSupport,
    WsParameters wsParameters, PermissionService permissionService, ManagedInstanceChecker managedInstanceChecker) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.permissionUpdater = permissionUpdater;
    this.wsSupport = wsSupport;
    this.wsParameters = wsParameters;
    this.permissionService = permissionService;
    this.managedInstanceChecker = managedInstanceChecker;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setDescription("Remove permission from a user.<br /> " +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br /> " +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "</ul>")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    wsParameters.createPermissionParameter(action, "The permission you would like to revoke from the user.");
    wsParameters.createOrganizationParameter(action).setSince("6.2");
    createUserLoginParameter(action);
    createProjectParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserId userIdDto = wsSupport.findUser(dbSession, request.mandatoryParam(PARAM_USER_LOGIN));
      String permission = request.mandatoryParam(PARAM_PERMISSION);
      wsSupport.checkRemovingOwnAdminRight(userSession, userIdDto, permission);

      OrganizationDto org = dbClient.organizationDao().selectByKey(dbSession, request.mandatoryParam(PARAM_ORGANIZATION))
          .orElseThrow(() -> new NotFoundException("No organization found with key: " + request.param(PARAM_ORGANIZATION)));
      EntityDto entityDto = wsSupport.findEntity(dbSession, request);
      wsSupport.checkRemovingOwnBrowsePermissionOnPrivateProject(userSession, entityDto, permission, userIdDto);
      wsSupport.checkPermissionManagementAccess(userSession, entityDto, org.getUuid());
      if (entityDto != null && entityDto.isProject()) {
        managedInstanceChecker.throwIfUserAndProjectAreManaged(dbSession, userIdDto.getUuid(), entityDto.getUuid());
      }
      UserPermissionChange change = new UserPermissionChange(
        Operation.REMOVE,
        org.getUuid(),
        permission,
        entityDto,
        userIdDto,
        permissionService);
      logger.info("Removing permissions for user: {} and permission type: {}, organization: {}, orgId: {}", userIdDto.getLogin(),
          change.getPermission(), org.getKey(), org.getUuid());
      permissionUpdater.apply(dbSession, singletonList(change));
      response.noContent();
    }
  }
}
