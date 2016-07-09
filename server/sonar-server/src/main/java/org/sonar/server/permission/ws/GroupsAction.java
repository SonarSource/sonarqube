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
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.google.common.io.Resources;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupRoleDto;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsPermissions.Group;
import org.sonarqube.ws.WsPermissions.WsGroupsResponse;
import org.sonarqube.ws.client.permission.GroupsWsRequest;

import static org.sonar.db.permission.PermissionQuery.DEFAULT_PAGE_SIZE;
import static org.sonar.db.permission.PermissionQuery.RESULTS_MAX_SIZE;
import static org.sonar.db.permission.PermissionQuery.SEARCH_QUERY_MIN_LENGTH;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdminUserByComponentDto;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validatePermission;
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
      .setDescription("Lists the groups with their permissions.<br>" +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br> " +
        "This service defaults to all groups, but can be limited to groups with a specific permission by providing the desired permission.<br>" +
        "It requires administration permissions to access.")
      .addPagingParams(DEFAULT_PAGE_SIZE, RESULTS_MAX_SIZE)
      .addSearchQuery("sonar", "names").setDescription("Limit search to group names that contain the supplied string. Must have at least %d characters.<br/>" +
        "When this parameter is not set, only groups having at least one permission are returned.", SEARCH_QUERY_MIN_LENGTH)
      .setResponseExample(Resources.getResource(getClass(), "groups-example.json"))
      .setHandler(this);

    createPermissionParameter(action)
      .setRequired(false);
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

      PermissionQuery.Builder dbQuery = buildPermissionQuery(request, project);
      List<GroupDto> groups = permissionFinder.findGroups(dbSession, dbQuery);
      int total = dbClient.permissionDao().countGroupsByPermissionQuery(dbSession, dbQuery.build());
      List<GroupRoleDto> groupsWithPermission = permissionFinder.findGroupPermissions(dbSession, dbQuery, groups);
      return buildResponse(groups, groupsWithPermission, Paging.forPageIndex(request.getPage()).withPageSize(request.getPageSize()).andTotal(total));
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static GroupsWsRequest toGroupsWsRequest(Request request) {
    GroupsWsRequest groupsRequest = new GroupsWsRequest()
      .setPermission(request.param(PARAM_PERMISSION))
      .setProjectId(request.param(PARAM_PROJECT_ID))
      .setProjectKey(request.param(PARAM_PROJECT_KEY))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setQuery(request.param(Param.TEXT_QUERY));

    Optional<WsProjectRef> wsProjectRef = newOptionalWsProjectRef(groupsRequest.getProjectId(), groupsRequest.getProjectKey());
    validatePermission(groupsRequest.getPermission(), wsProjectRef);
    return groupsRequest;
  }

  private static PermissionQuery.Builder buildPermissionQuery(GroupsWsRequest request, Optional<ComponentDto> project) {
    PermissionQuery.Builder permissionQuery = PermissionQuery.builder()
      .setPermission(request.getPermission())
      .setPageIndex(request.getPage())
      .setPageSize(request.getPageSize())
      .setSearchQuery(request.getQuery());
    if (project.isPresent()) {
      permissionQuery.setComponentUuid(project.get().uuid());
    }
    if (request.getQuery() == null) {
      permissionQuery.withPermissionOnly();
    }
    return permissionQuery;
  }

  private static WsGroupsResponse buildResponse(List<GroupDto> groups, List<GroupRoleDto> groupPermissions, Paging paging) {
    Multimap<Long, String> permissionsByGroupId = TreeMultimap.create();
    groupPermissions.forEach(groupPermission -> permissionsByGroupId.put(groupPermission.getGroupId(), groupPermission.getRole()));
    WsGroupsResponse.Builder response = WsGroupsResponse.newBuilder();

    groups.forEach(group -> {
      Group.Builder wsGroup = response.addGroupsBuilder()
        .setName(group.getName());
      if (group.getId() != 0L) {
        wsGroup.setId(String.valueOf(group.getId()));
      }
      if (group.getDescription() != null) {
        wsGroup.setDescription(group.getDescription());
      }
      wsGroup.addAllPermissions(permissionsByGroupId.get(group.getId()));
    });

    response.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total());

    return response.build();
  }
}
