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
package org.sonar.server.permission.ws;

import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import com.google.common.io.Resources;
import java.util.List;
import java.util.Optional;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.user.GroupDto;
import org.sonar.server.permission.ProjectId;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Permissions.Group;
import org.sonarqube.ws.Permissions.WsGroupsResponse;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.sonar.db.permission.PermissionQuery.DEFAULT_PAGE_SIZE;
import static org.sonar.db.permission.PermissionQuery.RESULTS_MAX_SIZE;
import static org.sonar.db.permission.PermissionQuery.SEARCH_QUERY_MIN_LENGTH;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdmin;
import static org.sonar.server.permission.ws.WsParameters.createOrganizationParameter;
import static org.sonar.server.permission.ws.WsParameters.createProjectParameters;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_ORGANIZATION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;

public class GroupsAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionWsSupport wsSupport;
  private final WsParameters wsParameters;

  public GroupsAction(DbClient dbClient, UserSession userSession, PermissionWsSupport wsSupport, WsParameters wsParameters) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
    this.wsParameters = wsParameters;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("groups")
      .setSince("5.2")
      .setInternal(true)
      .setDescription("Lists the groups with their permissions.<br>" +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br> " +
        "This service defaults to all groups, but can be limited to groups with a specific permission by providing the desired permission.<br>" +
        "Requires one of the following permissions:" +
        "<ul>" +
        "<li>'Administer System'</li>" +
        "<li>'Administer' rights on the specified project</li>" +
        "</ul>")
      .addPagingParams(DEFAULT_PAGE_SIZE, RESULTS_MAX_SIZE)
      .setChangelog(
        new Change("7.4", "The response list is returning all groups even those without permissions, the groups with permission are at the top of the list."))
      .setResponseExample(Resources.getResource(getClass(), "groups-example.json"))
      .setHandler(this);

    action.createSearchQuery("sonar", "names")
      .setDescription("Limit search to group names that contain the supplied string.")
      .setMinimumLength(SEARCH_QUERY_MIN_LENGTH);

    createOrganizationParameter(action).setSince("6.2");
    wsParameters.createPermissionParameter(action).setRequired(false);
    createProjectParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto org = wsSupport.findOrganization(dbSession, request.param(PARAM_ORGANIZATION));
      Optional<ProjectId> projectId = wsSupport.findProjectId(dbSession, request);
      checkProjectAdmin(userSession, org.getUuid(), projectId);

      PermissionQuery query = buildPermissionQuery(request, org, projectId);
      // TODO validatePermission(groupsRequest.getPermission(), wsProjectRef);
      List<GroupDto> groups = findGroups(dbSession, org, query);
      int total = dbClient.groupPermissionDao().countGroupsByQuery(dbSession, query);
      List<GroupPermissionDto> groupsWithPermission = findGroupPermissions(dbSession, org, groups, projectId);
      Paging paging = Paging.forPageIndex(request.mandatoryParamAsInt(Param.PAGE)).withPageSize(query.getPageSize()).andTotal(total);
      WsGroupsResponse groupsResponse = buildResponse(groups, groupsWithPermission, paging);
      writeProtobuf(groupsResponse, request, response);
    }
  }

  private static PermissionQuery buildPermissionQuery(Request request, OrganizationDto org, Optional<ProjectId> project) {
    String textQuery = request.param(Param.TEXT_QUERY);
    PermissionQuery.Builder permissionQuery = PermissionQuery.builder()
      .setOrganizationUuid(org.getUuid())
      .setPermission(request.param(PARAM_PERMISSION))
      .setPageIndex(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setSearchQuery(textQuery);
    if (project.isPresent()) {
      permissionQuery.setComponentUuid(project.get().getUuid());
    }

    return permissionQuery.build();
  }

  private static WsGroupsResponse buildResponse(List<GroupDto> groups, List<GroupPermissionDto> groupPermissions, Paging paging) {
    Multimap<Integer, String> permissionsByGroupId = TreeMultimap.create();
    groupPermissions.forEach(groupPermission -> permissionsByGroupId.put(groupPermission.getGroupId(), groupPermission.getRole()));
    WsGroupsResponse.Builder response = WsGroupsResponse.newBuilder();

    groups.forEach(group -> {
      Group.Builder wsGroup = response.addGroupsBuilder()
        .setName(group.getName());
      if (group.getId() != 0) {
        wsGroup.setId(String.valueOf(group.getId()));
      }
      ofNullable(group.getDescription()).ifPresent(wsGroup::setDescription);
      wsGroup.addAllPermissions(permissionsByGroupId.get(group.getId()));
    });

    response.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total());

    return response.build();
  }

  private List<GroupDto> findGroups(DbSession dbSession, OrganizationDto org, PermissionQuery dbQuery) {
    List<String> orderedNames = dbClient.groupPermissionDao().selectGroupNamesByQuery(dbSession, dbQuery);
    List<GroupDto> groups = dbClient.groupDao().selectByNames(dbSession, org.getUuid(), orderedNames);
    if (orderedNames.contains(DefaultGroups.ANYONE)) {
      groups.add(0, new GroupDto().setId(0).setName(DefaultGroups.ANYONE).setOrganizationUuid(org.getUuid()));
    }
    return Ordering.explicit(orderedNames).onResultOf(GroupDto::getName).immutableSortedCopy(groups);
  }

  private List<GroupPermissionDto> findGroupPermissions(DbSession dbSession, OrganizationDto org, List<GroupDto> groups, Optional<ProjectId> project) {
    if (groups.isEmpty()) {
      return emptyList();
    }
    List<Integer> ids = groups.stream().map(GroupDto::getId).collect(MoreCollectors.toList(groups.size()));
    return dbClient.groupPermissionDao().selectByGroupIds(dbSession, org.getUuid(), ids, project.isPresent() ? project.get().getId() : null);
  }
}
