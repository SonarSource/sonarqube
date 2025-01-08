/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.common.permission.GroupPermissionChange;
import org.sonar.server.permission.GroupUuidOrAnyone;
import org.sonar.server.common.permission.Operation;
import org.sonar.server.permission.PermissionService;
import org.sonar.server.common.permission.PermissionUpdater;
import org.sonar.server.user.UserSession;

import static java.util.Collections.singletonList;
import static org.sonar.server.permission.ws.WsParameters.createGroupNameParameter;
import static org.sonar.server.permission.ws.WsParameters.createProjectParameters;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;

public class RemoveGroupAction implements PermissionsWsAction {

  public static final String ACTION = "remove_group";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionUpdater<GroupPermissionChange> permissionUpdater;
  private final PermissionWsSupport wsSupport;
  private final WsParameters wsParameters;
  private final PermissionService permissionService;
  private final ManagedInstanceChecker managedInstanceChecker;

  public RemoveGroupAction(DbClient dbClient, UserSession userSession, PermissionUpdater<GroupPermissionChange> permissionUpdater, PermissionWsSupport wsSupport,
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
      .setDescription("Remove a permission from a group.<br /> " +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br /> " +
        "The group name must be provided.<br />" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "</ul>")
      .setSince("5.2")
      .setPost(true)
      .setChangelog(
        new Change("10.0", "Parameter 'groupId' is removed. Use 'groupName' instead."),
        new Change("8.4", "Parameter 'groupId' is deprecated. Format changes from integer to string. Use 'groupName' instead."))
      .setHandler(this);

    wsParameters.createPermissionParameter(action, "The permission you would like to revoke from the group.");
    createGroupNameParameter(action);
    createProjectParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      EntityDto entityDto = wsSupport.findEntity(dbSession, request);
      GroupDto groupDto = wsSupport.findGroupDtoOrNullIfAnyone(dbSession, request);
      if (entityDto != null && entityDto.isProject() && groupDto != null) {
        managedInstanceChecker.throwIfGroupAndProjectAreManaged(dbSession, groupDto.getUuid(), entityDto.getUuid());
      }
      wsSupport.checkPermissionManagementAccess(userSession, entityDto);

      String permission = request.mandatoryParam(PARAM_PERMISSION);
      wsSupport.checkRemovingOwnBrowsePermissionOnPrivateProject(dbSession, userSession, entityDto, permission, GroupUuidOrAnyone.from(groupDto));

      GroupPermissionChange change = new GroupPermissionChange(
        Operation.REMOVE,
        permission,
        entityDto,
        groupDto,
        permissionService);
      permissionUpdater.apply(dbSession, singletonList(change));
    }
    response.noContent();
  }

}
