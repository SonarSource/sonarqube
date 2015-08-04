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
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.permission.UserWithPermission;
import org.sonar.core.util.ProtobufJsonFormat;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.permission.UserWithPermissionQueryResult;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Permissions;

import static com.google.common.base.Objects.firstNonNull;
import static org.sonar.server.permission.PermissionQueryParser.toMembership;

public class UsersAction implements PermissionsWsAction {

  private final UserSession userSession;
  private final PermissionFinder permissionFinder;

  public UsersAction(UserSession userSession, PermissionFinder permissionFinder) {
    this.userSession = userSession;
    this.permissionFinder = permissionFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("users")
      .setSince("5.2")
      .setDescription(String.format("List permission's users.<br /> " +
        "If the query parameter '%s' is specified, the '%s' parameter is '%s'.",
        Param.TEXT_QUERY, Param.SELECTED, SelectionMode.ALL.value()))
      .addPagingParams(100)
      .addSearchQuery("stas", "names")
      .addSelectionModeParam()
      .setInternal(true)
      .setResponseExample(Resources.getResource(getClass(), "users-example.json"))
      .setHandler(this);

    action.createParam("permission")
      .setExampleValue("scan")
      .setRequired(true)
      .setPossibleValues(GlobalPermissions.ALL);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String permission = request.mandatoryParam("permission");
    String selected = request.param(Param.SELECTED);
    int page = request.mandatoryParamAsInt(Param.PAGE);
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    String query = request.param(Param.TEXT_QUERY);
    if (query != null) {
      selected = SelectionMode.ALL.value();
    }

    userSession
      .checkLoggedIn()
      .checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    PermissionQuery.Builder permissionQuery = PermissionQuery.builder()
      .permission(permission)
      .pageIndex(page)
      .pageSize(pageSize)
      .membership(toMembership(firstNonNull(selected, SelectionMode.SELECTED.value())));
    if (query != null) {
      permissionQuery.search(query);
    }

    UserWithPermissionQueryResult usersResult = permissionFinder.findUsersWithPermission(permissionQuery.build());
    List<UserWithPermission> usersWithPermission = usersResult.users();

    Permissions.UsersResponse.Builder userResponse = Permissions.UsersResponse.newBuilder();
    Permissions.UsersResponse.User.Builder user = Permissions.UsersResponse.User.newBuilder();
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
          .setPages(page)
          .setPageSize(pageSize)
          .setTotal(usersResult.total())
        );
    }

    response.stream().setMediaType(MimeTypes.JSON);
    JsonWriter json = response.newJsonWriter();
    ProtobufJsonFormat.write(userResponse.build(), json);
    json.close();
  }
}
