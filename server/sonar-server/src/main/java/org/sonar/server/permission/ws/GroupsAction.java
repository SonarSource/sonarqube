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

import com.google.common.io.Resources;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.GroupWithPermission;
import org.sonar.core.util.ProtobufJsonFormat;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.server.permission.GroupWithPermissionQueryResult;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Permissions;

import static com.google.common.base.Objects.firstNonNull;
import static org.sonar.server.permission.PermissionQueryParser.toMembership;

public class GroupsAction implements PermissionsWsAction {
  private final UserSession userSession;
  private final PermissionFinder permissionFinder;

  public GroupsAction(UserSession userSession, PermissionFinder permissionFinder) {
    this.userSession = userSession;
    this.permissionFinder = permissionFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("groups")
      .setSince("5.2")
      .setInternal(true)
      .setDescription(String.format("List permission's groups.<br /> " +
        "If the query parameter '%s' is specified, the '%s' parameter is '%s'.",
        WebService.Param.TEXT_QUERY, WebService.Param.SELECTED, WebService.SelectionMode.ALL.value()))
      .addPagingParams(100)
      .addSearchQuery("sonar", "names")
      .addSelectionModeParam()
      .setResponseExample(Resources.getResource(getClass(), "groups-example.json"))
      .setHandler(this);

    action.createParam("permission")
      .setExampleValue("scan")
      .setRequired(true)
      .setPossibleValues(GlobalPermissions.ALL);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String permission = request.mandatoryParam("permission");
    String selected = request.param(WebService.Param.SELECTED);
    int page = request.mandatoryParamAsInt(WebService.Param.PAGE);
    int pageSize = request.mandatoryParamAsInt(WebService.Param.PAGE_SIZE);
    String query = request.param(WebService.Param.TEXT_QUERY);
    if (query != null) {
      selected = WebService.SelectionMode.ALL.value();
    }

    userSession
      .checkLoggedIn()
      .checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    PermissionQuery.Builder permissionQuery = PermissionQuery.builder()
      .permission(permission)
      .pageIndex(page)
      .pageSize(pageSize)
      .membership(toMembership(firstNonNull(selected, WebService.SelectionMode.SELECTED.value())));
    if (query != null) {
      permissionQuery.search(query);
    }

    GroupWithPermissionQueryResult groupsResult = permissionFinder.findGroupsWithPermission(permissionQuery.build());
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

    response.stream().setMediaType(MimeTypes.JSON);
    JsonWriter json = response.newJsonWriter();
    ProtobufJsonFormat.write(groupsResponse.build(), json);
    json.close();
  }
}
