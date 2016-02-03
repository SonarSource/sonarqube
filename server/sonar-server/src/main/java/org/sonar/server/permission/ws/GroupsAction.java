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
package org.sonar.server.permission.ws;

import com.google.common.base.Optional;
import com.google.common.io.Resources;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.GroupWithPermissionDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsPermissions.Group;
import org.sonarqube.ws.WsPermissions.WsGroupsResponse;
import org.sonarqube.ws.client.permission.GroupsWsRequest;

import static com.google.common.base.Objects.firstNonNull;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdminUserByComponentDto;
import static org.sonar.server.permission.ws.PermissionQueryParser.fromSelectionModeToMembership;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateGlobalPermission;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPermission;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createPermissionParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectParameters;
import static org.sonar.server.permission.ws.WsProjectRef.newOptionalWsProjectRef;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

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
    createProjectParameters(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    GroupsWsRequest groupsRequest = toGroupsWsRequest(wsRequest);
    WsGroupsResponse groupsResponse = doHandle(groupsRequest);
    writeProtobuf(groupsResponse, wsRequest, wsResponse);
  }

  private WsGroupsResponse doHandle(GroupsWsRequest request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<ComponentDto> project = dependenciesFinder.searchProject(dbSession, newOptionalWsProjectRef(request.getProjectId(), request.getProjectKey()));
      checkProjectAdminUserByComponentDto(userSession, project);

      PermissionQuery permissionQuery = buildPermissionQuery(request, project);
      Long projectIdIfPresent = project.isPresent() ? project.get().getId() : null;
      int total = dbClient.permissionDao().countGroups(dbSession, permissionQuery.permission(), projectIdIfPresent);
      List<GroupWithPermissionDto> groupsWithPermission = permissionFinder.findGroupsWithPermission(dbSession, permissionQuery);
      return buildResponse(groupsWithPermission, request, total);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static GroupsWsRequest toGroupsWsRequest(Request request) {
    String permission = request.mandatoryParam(PARAM_PERMISSION);
    String projectUuid = request.param(PARAM_PROJECT_ID);
    String projectKey = request.param(PARAM_PROJECT_KEY);
    if (newOptionalWsProjectRef(projectUuid, projectKey).isPresent()) {
      validateProjectPermission(permission);
    } else {
      validateGlobalPermission(permission);
    }

    return new GroupsWsRequest()
      .setPermission(permission)
      .setProjectId(projectUuid)
      .setProjectKey(projectKey)
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setQuery(request.param(Param.TEXT_QUERY))
      .setSelected(request.mandatoryParam(Param.SELECTED));
  }

  private static PermissionQuery buildPermissionQuery(GroupsWsRequest request, Optional<ComponentDto> project) {
    PermissionQuery.Builder permissionQuery = PermissionQuery.builder()
      .permission(request.getPermission())
      .pageIndex(request.getPage())
      .pageSize(request.getPageSize())
      .membership(fromSelectionModeToMembership(firstNonNull(request.getSelected(), SelectionMode.SELECTED.value())))
      .search(request.getQuery());
    if (project.isPresent()) {
      permissionQuery.component(project.get().getKey());
    }

    return permissionQuery.build();
  }

  private static WsGroupsResponse buildResponse(List<GroupWithPermissionDto> groupsWithPermission, GroupsWsRequest permissionRequest, int total) {
    WsGroupsResponse.Builder groupsResponse = WsGroupsResponse.newBuilder();
    Group.Builder group = Group.newBuilder();
    Common.Paging.Builder paging = Common.Paging.newBuilder();

    for (GroupWithPermissionDto groupWithPermission : groupsWithPermission) {
      group
        .clear()
        .setName(groupWithPermission.getName())
        .setSelected(groupWithPermission.getPermission() != null);
      // anyone group return with id = 0
      if (groupWithPermission.getId() != 0) {
        group.setId(String.valueOf(groupWithPermission.getId()));
      }
      if (groupWithPermission.getDescription() != null) {
        group.setDescription(groupWithPermission.getDescription());
      }

      groupsResponse.addGroups(group);
    }

    groupsResponse.setPaging(
      paging
        .setPageIndex(permissionRequest.getPage())
        .setPageSize(permissionRequest.getPageSize())
        .setTotal(total));

    return groupsResponse.build();
  }
}
