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

package org.sonar.server.permission.ws;

import com.google.common.base.Optional;
import com.google.common.io.Resources;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.core.permission.GroupWithPermission;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.server.permission.GroupWithPermissionQueryResult;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.permission.ws.PermissionRequest.Builder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsPermissions.WsGroupsResponse;

import static com.google.common.base.Objects.firstNonNull;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdminUserByComponentDto;
import static org.sonar.server.permission.ws.PermissionQueryParser.fromSelectionModeToMembership;
import static org.sonar.server.permission.ws.WsPermissionParameters.createPermissionParameter;
import static org.sonar.server.permission.ws.WsPermissionParameters.createProjectParameter;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class GroupsAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionFinder permissionFinder;
  private final PermissionDependenciesFinder dependenciesFinder;

  public GroupsAction(DbClient dbClient, UserSession userSession, PermissionFinder permissionFinder, PermissionDependenciesFinder dependenciesFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.permissionFinder = permissionFinder;
    this.dependenciesFinder = dependenciesFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("groups")
      .setSince("5.2")
      .setInternal(true)
      .setDescription(String.format("Lists the groups that have been explicitly granted the specified permission. <br />" +
        "This service defaults to global permissions, but can be limited to project permissions by providing a project id or project key. <br />" +
        "If the query parameter '%s' is specified, the '%s' parameter is forced to '%s'. <br />" +
        "It requires administration permissions to access.",
        Param.TEXT_QUERY, Param.SELECTED, SelectionMode.ALL.value()))
      .addPagingParams(100)
      .addSearchQuery("sonar", "names")
      .addSelectionModeParam()
      .setResponseExample(Resources.getResource(getClass(), "groups-example.json"))
      .setHandler(this);

    createPermissionParameter(action);
    createProjectParameter(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    DbSession dbSession = dbClient.openSession(false);
    try {
      PermissionRequest request = new Builder(wsRequest).withPagination().build();
      Optional<ComponentDto> project = dependenciesFinder.searchProject(dbSession, request);
      checkProjectAdminUserByComponentDto(userSession, project);

      PermissionQuery permissionQuery = buildPermissionQuery(request, project);
      WsGroupsResponse groupsResponse = buildResponse(permissionQuery, request);

      writeProtobuf(groupsResponse, wsRequest, wsResponse);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private WsGroupsResponse buildResponse(PermissionQuery permissionQuery, PermissionRequest permissionRequest) {
    GroupWithPermissionQueryResult groupsResult = permissionFinder.findGroupsWithPermission(permissionQuery);
    List<GroupWithPermission> groupsWithPermission = groupsResult.groups();

    WsGroupsResponse.Builder groupsResponse = WsGroupsResponse.newBuilder();
    WsGroupsResponse.Group.Builder group = WsGroupsResponse.Group.newBuilder();
    Common.Paging.Builder paging = Common.Paging.newBuilder();

    for (GroupWithPermission groupWithPermission : groupsWithPermission) {
      group
        .clear()
        .setName(groupWithPermission.name())
        .setSelected(groupWithPermission.hasPermission());
      // anyone group return with id = 0
      if (groupWithPermission.id() != 0) {
        group.setId(String.valueOf(groupWithPermission.id()));
      }
      if (groupWithPermission.description() != null) {
        group.setDescription(groupWithPermission.description());
      }

      groupsResponse.addGroups(group);
    }

    groupsResponse.setPaging(
      paging
        .setPageIndex(permissionRequest.page())
        .setPageSize(permissionRequest.pageSize())
        .setTotal(groupsResult.total())
      );

    return groupsResponse.build();
  }

  private static PermissionQuery buildPermissionQuery(PermissionRequest request, Optional<ComponentDto> project) {
    PermissionQuery.Builder permissionQuery = PermissionQuery.builder()
      .permission(request.permission())
      .pageIndex(request.page())
      .pageSize(request.pageSize())
      .membership(fromSelectionModeToMembership(firstNonNull(request.selected(), SelectionMode.SELECTED.value())))
      .search(request.query());
    if (project.isPresent()) {
      permissionQuery.component(project.get().getKey());
    }

    return permissionQuery.build();
  }
}
