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
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.core.permission.UserWithPermission;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.permission.UserWithPermissionQueryResult;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Permissions.UsersResponse;

import static com.google.common.base.Objects.firstNonNull;
import static org.sonar.server.permission.PermissionQueryParser.toMembership;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_KEY;
import static org.sonar.server.permission.ws.PermissionWsCommons.PARAM_PROJECT_UUID;
import static org.sonar.server.permission.ws.PermissionWsCommons.createPermissionParam;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class UsersAction implements PermissionsWsAction {

  private final UserSession userSession;
  private final PermissionFinder permissionFinder;
  private final PermissionWsCommons permissionWsCommons;

  public UsersAction(UserSession userSession, PermissionFinder permissionFinder, PermissionWsCommons permissionWsCommons) {
    this.permissionWsCommons = permissionWsCommons;
    this.userSession = userSession;
    this.permissionFinder = permissionFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("users")
      .setSince("5.2")
      .setDescription(String.format("List permission's users.<br /> " +
        "If the project id or project key is provided, users with project permissions are returned.<br />" +
        "If the query parameter '%s' is specified, the '%s' parameter is '%s'.",
        Param.TEXT_QUERY, Param.SELECTED, SelectionMode.ALL.value()))
      .addPagingParams(100)
      .addSearchQuery("stas", "names")
      .addSelectionModeParam()
      .setInternal(true)
      .setResponseExample(getClass().getResource("users-example.json"))
      .setHandler(this);

    createPermissionParam(action);

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
    permissionWsCommons.checkPermissions(project, wsRequest.mandatoryParam(PARAM_PERMISSION));
    PermissionQuery permissionQuery = buildPermissionQuery(wsRequest, project);
    UsersResponse usersResponse = usersResponse(permissionQuery, page, pageSize);

    writeProtobuf(usersResponse, wsRequest, wsResponse);
  }

  private UsersResponse usersResponse(PermissionQuery permissionQuery, int page, int pageSize) {
    UserWithPermissionQueryResult usersResult = permissionFinder.findUsersWithPermission(permissionQuery);
    List<UserWithPermission> usersWithPermission = usersResult.users();

    UsersResponse.Builder userResponse = UsersResponse.newBuilder();
    UsersResponse.User.Builder user = UsersResponse.User.newBuilder();
    Common.Paging.Builder paging = Common.Paging.newBuilder();
    for (UserWithPermission userWithPermission : usersWithPermission) {
      userResponse.addUsers(
        user
          .clear()
          .setLogin(userWithPermission.login())
          .setName(userWithPermission.name())
          .setSelected(userWithPermission.hasPermission()));
      userResponse.setPaging(
        paging
          .clear()
          .setPageIndex(page)
          .setPageSize(pageSize)
          .setTotal(usersResult.total())
        );
    }

    return userResponse.build();
  }

  private static PermissionQuery buildPermissionQuery(Request request, Optional<ComponentDto> project) {
    String permission = request.mandatoryParam(PARAM_PERMISSION);
    String selected = request.param(Param.SELECTED);
    int page = request.mandatoryParamAsInt(Param.PAGE);
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    String query = request.param(Param.TEXT_QUERY);
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
