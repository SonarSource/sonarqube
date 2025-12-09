/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.entity.EntityDto;
import org.sonar.db.permission.GroupPermissionDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.user.GroupDto;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Permissions.Group;
import org.sonarqube.ws.Permissions.WsGroupsResponse;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.sonar.db.permission.PermissionQuery.DEFAULT_PAGE_SIZE;
import static org.sonar.db.permission.PermissionQuery.RESULTS_MAX_SIZE;
import static org.sonar.db.permission.PermissionQuery.SEARCH_QUERY_MIN_LENGTH;
import static org.sonar.server.permission.ws.WsParameters.createProjectParameters;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;

public class GroupsAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionWsSupport wsSupport;
  private final WsParameters wsParameters;
  private final ManagedInstanceService managedInstanceService;

  public GroupsAction(DbClient dbClient, UserSession userSession, PermissionWsSupport wsSupport, WsParameters wsParameters,
    ManagedInstanceService managedInstanceService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
    this.wsParameters = wsParameters;
    this.managedInstanceService = managedInstanceService;
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
        new Change("10.0", "Response includes 'managed' field."),
        new Change("8.4", "Field 'id' in the response is deprecated. Format changes from integer to string."),
        new Change("7.4", "The response list is returning all groups even those without permissions, the groups with permission are at the top of the list."))
      .setResponseExample(Resources.getResource(getClass(), "groups-example.json"))
      .setHandler(this);

    action.createSearchQuery("sonar", "names")
      .setDescription("Limit search to group names that contain the supplied string.")
      .setMinimumLength(SEARCH_QUERY_MIN_LENGTH);

    wsParameters.createPermissionParameter(action).setRequired(false);
    createProjectParameters(action);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      EntityDto project = wsSupport.findEntity(dbSession, request);
      wsSupport.checkPermissionManagementAccess(userSession, project);

      PermissionQuery query = buildPermissionQuery(request, project);
      List<GroupDto> groups = findGroups(dbSession, query);
      int total = dbClient.groupPermissionDao().countGroupsByQuery(dbSession, query);
      List<GroupPermissionDto> groupsWithPermission = findGroupPermissions(dbSession, groups, project);
      Map<String, Boolean> groupUuidToIsManaged = managedInstanceService.getGroupUuidToManaged(dbSession, getUserUuids(groups));
      Paging paging = Paging.forPageIndex(request.mandatoryParamAsInt(Param.PAGE)).withPageSize(query.getPageSize()).andTotal(total);
      WsGroupsResponse groupsResponse = buildResponse(groups, groupsWithPermission, groupUuidToIsManaged, paging);
      writeProtobuf(groupsResponse, request, response);
    }
  }

  private static Set<String> getUserUuids(List<GroupDto> groups) {
    return groups.stream().map(GroupDto::getUuid).collect(toSet());
  }

  private static PermissionQuery buildPermissionQuery(Request request, @Nullable EntityDto entity) {
    String textQuery = request.param(Param.TEXT_QUERY);
    PermissionQuery.Builder permissionQuery = PermissionQuery.builder()
      .setPermission(request.param(PARAM_PERMISSION))
      .setPageIndex(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setSearchQuery(textQuery);
    if (entity != null) {
      permissionQuery.setEntityUuid(entity.getUuid());
    }
    return permissionQuery.build();
  }

  private static WsGroupsResponse buildResponse(List<GroupDto> groups, List<GroupPermissionDto> groupPermissions,
    Map<String, Boolean> groupUuidToIsManaged, Paging paging) {
    Multimap<String, String> permissionsByGroupUuid = TreeMultimap.create();
    groupPermissions.forEach(groupPermission -> permissionsByGroupUuid.put(groupPermission.getGroupUuid(), groupPermission.getRole()));
    WsGroupsResponse.Builder response = WsGroupsResponse.newBuilder();

    groups.forEach(group -> {
      Group.Builder wsGroup = response.addGroupsBuilder()
        .setName(group.getName());
      if (group.getUuid() != null) {
        wsGroup.setId(String.valueOf(group.getUuid()));
      }
      ofNullable(group.getDescription()).ifPresent(wsGroup::setDescription);
      wsGroup.addAllPermissions(permissionsByGroupUuid.get(group.getUuid()));
      wsGroup.setManaged(groupUuidToIsManaged.getOrDefault(group.getUuid(), false));
    });

    response.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total());

    return response.build();
  }

  private List<GroupDto> findGroups(DbSession dbSession, PermissionQuery dbQuery) {
    List<String> orderedNames = dbClient.groupPermissionDao().selectGroupNamesByQuery(dbSession, dbQuery);
    List<GroupDto> groups = dbClient.groupDao().selectByNames(dbSession, orderedNames);
    if (orderedNames.contains(DefaultGroups.ANYONE)) {
      groups.add(0, new GroupDto().setUuid(DefaultGroups.ANYONE).setName(DefaultGroups.ANYONE));
    }
    return Ordering.explicit(orderedNames).onResultOf(GroupDto::getName).immutableSortedCopy(groups);
  }

  private List<GroupPermissionDto> findGroupPermissions(DbSession dbSession, List<GroupDto> groups, @Nullable EntityDto entity) {
    if (groups.isEmpty()) {
      return emptyList();
    }
    List<String> uuids = groups.stream().map(GroupDto::getUuid).toList();
    return dbClient.groupPermissionDao().selectByGroupUuids(dbSession, uuids, entity != null ? entity.getUuid() : null);
  }
}
