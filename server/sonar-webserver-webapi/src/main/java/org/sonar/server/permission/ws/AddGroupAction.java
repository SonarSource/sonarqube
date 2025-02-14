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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.db.entity.EntityDto;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.common.permission.GroupPermissionChange;
import org.sonar.server.common.permission.Operation;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.user.UserSession;

import static org.sonar.server.permission.ws.WsParameters.createGroupNameParameter;
import static org.sonar.server.permission.ws.WsParameters.createProjectParameters;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.*;

public class AddGroupAction implements PermissionsWsAction {
  public static final String ACTION = "add_group";

  private final Logger logger = LoggerFactory.getLogger(AddGroupAction.class);

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionUpdater<GroupPermissionChange> permissionUpdater;
  private final PermissionWsSupport wsSupport;
  private final WsParameters wsParameters;
  private final PermissionService permissionService;
  private final ManagedInstanceChecker managedInstanceChecker;

  public AddGroupAction(DbClient dbClient, UserSession userSession, PermissionUpdater<GroupPermissionChange> permissionUpdater, PermissionWsSupport wsSupport,
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
      .setDescription("Add a permission to a group.<br /> " +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br /> " +
        "The group name must be provided. <br />" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "</ul>")
      .setSince("5.2")
      .setChangelog(
        new Change("10.0", "Parameter 'groupId' is removed. Use 'groupName' instead."),
        new Change("8.4", "Parameter 'groupId' is deprecated. Format changes from integer to string. Use 'groupName' instead."))
      .setPost(true)
      .setHandler(this);

    wsParameters.createPermissionParameter(action, "The permission you would like to grant to the group.");
    wsParameters.createOrganizationParameter(action);
    createGroupNameParameter(action);
    createProjectParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto org = dbClient.organizationDao().selectByKey(dbSession, request.mandatoryParam(PARAM_ORGANIZATION))
              .orElseThrow(() -> new NotFoundException("No organization found with key: " + request.param(PARAM_ORGANIZATION)));
      GroupDto groupDto = wsSupport.findGroupDtoOrNullIfAnyone(dbSession, request);
      EntityDto entityDto = wsSupport.findEntity(dbSession, request);
      if (entityDto != null && entityDto.isProject()) {
        managedInstanceChecker.throwIfProjectIsManaged(dbSession, entityDto.getUuid());
      }

      String groupName = request.mandatoryParam(PARAM_GROUP_NAME);
      wsSupport.checkPermissionManagementAccess(userSession, entityDto, groupDto.getOrganizationUuid());

      logger.info("Grant Permission to a group: {} :: permission type: {}, organization: {}, orgId: {}, groupId: {}, user: {}",
          groupName, request.mandatoryParam(PARAM_PERMISSION), org.getKey(), org.getUuid(),
          groupDto.getUuid(), userSession.getLogin());

      GroupPermissionChange change = new GroupPermissionChange(
        Operation.ADD,
        org.getUuid(),
        request.mandatoryParam(PARAM_PERMISSION),
        entityDto,
        groupDto,
        permissionService);
      permissionUpdater.apply(dbSession, List.of(change));
    }
    response.noContent();
  }
}
