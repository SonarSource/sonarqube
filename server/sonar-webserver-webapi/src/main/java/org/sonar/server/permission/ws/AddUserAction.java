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
import org.sonar.api.config.Configuration;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserId;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.common.permission.Operation;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.common.permission.UserPermissionChange;
import org.sonar.server.user.UserSession;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singletonList;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdmin;
import static org.sonar.server.permission.ws.WsParameters.createProjectParameters;
import static org.sonar.server.permission.ws.WsParameters.createUserLoginParameter;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_USER_LOGIN;

public class AddUserAction implements PermissionsWsAction {

  public static final String ACTION = "add_user";

  private static final Logger logger = LoggerFactory.getLogger(AddUserAction.class);

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionUpdater<UserPermissionChange> permissionUpdater;
  private final PermissionWsSupport wsSupport;
  private final WsParameters wsParameters;
  private final PermissionService permissionService;
  private final Configuration configuration;
  private final ManagedInstanceChecker managedInstanceChecker;

  public AddUserAction(DbClient dbClient, UserSession userSession, PermissionUpdater<UserPermissionChange> permissionUpdater, PermissionWsSupport wsSupport,
    WsParameters wsParameters, PermissionService permissionService, Configuration configuration, ManagedInstanceChecker managedInstanceChecker) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.permissionUpdater = permissionUpdater;
    this.wsSupport = wsSupport;
    this.wsParameters = wsParameters;
    this.permissionService = permissionService;
    this.configuration = configuration;
    this.managedInstanceChecker = managedInstanceChecker;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION)
      .setDescription("Add permission to a user.<br /> " +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br />" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "</ul>")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

    wsParameters.createPermissionParameter(action, "The permission you would like to grant to the user");
    createUserLoginParameter(action);
    createProjectParameters(action);
    wsParameters.createOrganizationParameter(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String userLogin = request.mandatoryParam(PARAM_USER_LOGIN);
      EntityDto entityDto = wsSupport.findEntity(dbSession, request);

      String organizationKey = request.mandatoryParam(PARAM_ORGANIZATION);
      OrganizationDto org = dbClient.organizationDao().selectByKey(dbSession, organizationKey)
              .orElseThrow(() -> new NotFoundException(String.format("Organization with key '%s' not found", organizationKey)));
      if (entityDto != null) {
        checkArgument(org.getUuid().equals(entityDto.getOrganizationUuid()), "Organization key is incorrect.");
      }

      checkProjectAdmin(userSession, configuration, entityDto, org.getUuid());
      if (!userSession.isSystemAdministrator() && entityDto != null && entityDto.isProject()) {
        managedInstanceChecker.throwIfProjectIsManaged(dbSession, entityDto.getUuid());
      }


      UserId user = wsSupport.findUser(dbSession, userLogin);
      checkProjectAdmin(userSession, configuration, entityDto, org.getUuid());
      wsSupport.checkMembership(dbSession, org, user);

      UserPermissionChange change = new UserPermissionChange(
        Operation.ADD,
        org.getUuid(),
        request.mandatoryParam(PARAM_PERMISSION),
        entityDto,
        user,
        permissionService);
      logger.info("Granting permissions for user: {} and permission type: {}, organization: {}, orgId: {}", userLogin,
          change.getPermission(), org.getKey(), org.getUuid());
      permissionUpdater.apply(dbSession, singletonList(change));
    }
    response.noContent();
  }
}
