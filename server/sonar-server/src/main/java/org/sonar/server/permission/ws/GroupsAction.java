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
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.io.Resources;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.core.permission.ComponentPermissions;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.GroupWithPermission;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.server.permission.GroupWithPermissionQueryResult;
import org.sonar.server.permission.PermissionFinder;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Permissions;

import static com.google.common.base.Objects.firstNonNull;
import static org.sonar.core.permission.GlobalPermissions.DASHBOARD_SHARING;
import static org.sonar.server.permission.PermissionQueryParser.toMembership;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_KEY;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_UUID;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class GroupsAction implements PermissionsWsAction {
  private final PermissionFinder permissionFinder;
  private final PermissionWsCommons permissionWsCommons;

  public GroupsAction(PermissionFinder permissionFinder, PermissionWsCommons permissionWsCommons) {
    this.permissionFinder = permissionFinder;
    this.permissionWsCommons = permissionWsCommons;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("groups")
      .setSince("5.2")
      .setInternal(true)
      .setDescription(String.format("List permission's groups.<br /> " +
        "If the project id or project key is provided, groups with project permissions are returned.<br />" +
        "If the query parameter '%s' is specified, the '%s' parameter is '%s'.",
        Param.TEXT_QUERY, Param.SELECTED, SelectionMode.ALL.value()))
      .addPagingParams(100)
      .addSearchQuery("sonar", "names")
      .addSelectionModeParam()
      .setResponseExample(Resources.getResource(getClass(), "groups-example.json"))
      .setHandler(this);

    action.createParam(PARAM_PERMISSION)
      .setExampleValue(DASHBOARD_SHARING)
      .setRequired(true)
      .setPossibleValues(ImmutableSortedSet.naturalOrder()
        .addAll(GlobalPermissions.ALL)
        .addAll(ComponentPermissions.ALL)
        .build());

    action.createParam(PARAM_PROJECT_UUID)
      .setExampleValue("ce4c03d6-430f-40a9-b777-ad877c00aa4d")
      .setDescription("Project id");

    action.createParam(PARAM_PROJECT_KEY)
      .setExampleValue("org.apache.hbas:hbase")
      .setDescription("Project key");
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    int page = wsRequest.mandatoryParamAsInt(Param.PAGE);
    int pageSize = wsRequest.mandatoryParamAsInt(Param.PAGE_SIZE);

    Optional<ComponentDto> project = permissionWsCommons.searchProject(wsRequest);
    permissionWsCommons.checkPermissions(project);

    PermissionQuery permissionQuery = buildPermissionQuery(wsRequest, project);
    Permissions.GroupsResponse groupsResponse = groupsResponse(permissionQuery, page, pageSize);

    writeProtobuf(groupsResponse, wsRequest, wsResponse);
  }

  private Permissions.GroupsResponse groupsResponse(PermissionQuery permissionQuery, int page, int pageSize) {
    GroupWithPermissionQueryResult groupsResult = permissionFinder.findGroupsWithPermission(permissionQuery);
    List<GroupWithPermission> groupsWithPermission = groupsResult.groups();

    Permissions.GroupsResponse.Builder groupsResponse = Permissions.GroupsResponse.newBuilder();
    Permissions.GroupsResponse.Group.Builder group = Permissions.GroupsResponse.Group.newBuilder();
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
        .setPageIndex(page)
        .setPageSize(pageSize)
        .setTotal(groupsResult.total())
      );

    return groupsResponse.build();
  }

  private static PermissionQuery buildPermissionQuery(Request wsRequest, Optional<ComponentDto> project) {
    String permission = wsRequest.mandatoryParam("permission");
    String selected = wsRequest.param(Param.SELECTED);
    int page = wsRequest.mandatoryParamAsInt(Param.PAGE);
    int pageSize = wsRequest.mandatoryParamAsInt(Param.PAGE_SIZE);
    String query = wsRequest.param(Param.TEXT_QUERY);
    if (query != null) {
      selected = SelectionMode.ALL.value();
    }

    PermissionQuery.Builder permissionQuery = PermissionQuery.builder()
      .permission(permission)
      .pageIndex(page)
      .pageSize(pageSize)
      .membership(toMembership(firstNonNull(selected, SelectionMode.SELECTED.value())));
    if (query != null) {
      permissionQuery.search(query);
    }
    if (project.isPresent()) {
      permissionQuery.component(project.get().getKey());
    }

    return permissionQuery.build();
  }
}
