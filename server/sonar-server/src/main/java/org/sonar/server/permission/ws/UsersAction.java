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
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import java.util.List;
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserPermissionDto;
import org.sonar.server.permission.PermissionFinder;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsPermissions;
import org.sonarqube.ws.WsPermissions.UsersWsResponse;
import org.sonarqube.ws.client.permission.UsersWsRequest;

import static java.util.Collections.emptyList;
import static org.sonar.db.permission.PermissionQuery.DEFAULT_PAGE_SIZE;
import static org.sonar.db.permission.PermissionQuery.RESULTS_MAX_SIZE;
import static org.sonar.db.permission.PermissionQuery.SEARCH_QUERY_MIN_LENGTH;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkProjectAdminUserByComponentDto;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validatePermission;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createPermissionParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectParameters;
import static org.sonar.server.permission.ws.WsProjectRef.newOptionalWsProjectRef;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_ID;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PROJECT_KEY;

public class UsersAction implements PermissionsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionFinder permissionFinder;
  private final PermissionDependenciesFinder dependenciesFinder;

  public UsersAction(DbClient dbClient, UserSession userSession, PermissionFinder permissionFinder, PermissionDependenciesFinder dependenciesFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.permissionFinder = permissionFinder;
    this.dependenciesFinder = dependenciesFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("users")
      .setSince("5.2")
      .setDescription("Lists the users with their permissions as individual users rather than through group affiliation.<br>" +
        "This service defaults to global permissions, but can be limited to project permissions by providing project id or project key.<br> " +
        "This service defaults to all users, but can be limited to users with a specific permission by providing the desired permission.<br>" +
        "It requires administration permissions to access.")
      .addPagingParams(DEFAULT_PAGE_SIZE, RESULTS_MAX_SIZE)
      .setInternal(true)
      .setResponseExample(getClass().getResource("users-example.json"))
      .setHandler(this);

    action.createParam(Param.TEXT_QUERY)
      .setDescription("Limit search to user names that contain the supplied string. Must have at least %d characters.<br/>" +
        "When this parameter is not set, only users having at least one permission are returned.", SEARCH_QUERY_MIN_LENGTH)
      .setExampleValue("eri");
    createPermissionParameter(action).setRequired(false);
    createProjectParameters(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    UsersWsResponse usersWsResponse = doHandle(toUsersWsRequest(wsRequest));
    writeProtobuf(usersWsResponse, wsRequest, wsResponse);
  }

  private UsersWsResponse doHandle(UsersWsRequest request) {
    Optional<WsProjectRef> wsProjectRef = newOptionalWsProjectRef(request.getProjectId(), request.getProjectKey());
    validatePermission(request.getPermission(), wsProjectRef);
    DbSession dbSession = dbClient.openSession(false);
    try {
      Optional<ComponentDto> project = dependenciesFinder.searchProject(dbSession, wsProjectRef);
      checkProjectAdminUserByComponentDto(userSession, project);
      PermissionQuery dbQuery = buildPermissionQuery(request, project);
      List<UserDto> users = findUsers(dbSession, dbQuery);
      int total = dbClient.permissionDao().countUsersByQuery(dbSession, dbQuery);
      List<UserPermissionDto> userPermissions = findUserPermissions(dbSession, users);
      Paging paging = Paging.forPageIndex(request.getPage()).withPageSize(request.getPageSize()).andTotal(total);
      return buildResponse(users, userPermissions, paging);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static UsersWsRequest toUsersWsRequest(Request request) {
    UsersWsRequest usersRequest = new UsersWsRequest()
      .setPermission(request.param(PARAM_PERMISSION))
      .setProjectId(request.param(PARAM_PROJECT_ID))
      .setProjectKey(request.param(PARAM_PROJECT_KEY))
      .setQuery(request.param(Param.TEXT_QUERY))
      .setPage(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE));

    String searchQuery = usersRequest.getQuery();
    checkRequest(searchQuery == null || searchQuery.length() >= SEARCH_QUERY_MIN_LENGTH,
      "The '%s' parameter must have at least %d characters", Param.TEXT_QUERY, SEARCH_QUERY_MIN_LENGTH);
    return usersRequest;
  }

  private static UsersWsResponse buildResponse(List<UserDto> users, List<UserPermissionDto> userPermissions, Paging paging) {
    Multimap<Long, String> permissionsByUserId = TreeMultimap.create();
    userPermissions.forEach(userPermission -> permissionsByUserId.put(userPermission.getUserId(), userPermission.getPermission()));

    UsersWsResponse.Builder response = UsersWsResponse.newBuilder();
    users.forEach(user -> {
      WsPermissions.User.Builder userResponse = response.addUsersBuilder()
        .setLogin(user.getLogin())
        .addAllPermissions(permissionsByUserId.get(user.getId()));

      if (user.getEmail() != null) {
        userResponse.setEmail(user.getEmail());
      }
      if (user.getName() != null) {
        userResponse.setName(user.getName());
      }
    });

    response.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();

    return response.build();
  }

  private static PermissionQuery buildPermissionQuery(UsersWsRequest request, Optional<ComponentDto> project) {
    PermissionQuery.Builder dbQuery = PermissionQuery.builder()
      .setPermission(request.getPermission())
      .setPageIndex(request.getPage())
      .setPageSize(request.getPageSize())
      .setSearchQuery(request.getQuery());
    if (project.isPresent()) {
      dbQuery.setComponentUuid(project.get().uuid());
    }
    if (request.getQuery() == null) {
      dbQuery.withPermissionOnly();
    }

    return dbQuery.build();
  }

  private List<UserDto> findUsers(DbSession dbSession, PermissionQuery dbQuery) {
    List<String> orderedLogins = dbClient.permissionDao().selectLoginsByPermissionQuery(dbSession, dbQuery);
    return Ordering.explicit(orderedLogins).onResultOf(UserDto::getLogin).immutableSortedCopy(dbClient.userDao().selectByLogins(dbSession, orderedLogins));
  }

  private List<UserPermissionDto> findUserPermissions(DbSession dbSession, List<UserDto> users) {
    if (users.isEmpty()) {
      return emptyList();
    }
    List<String> logins = users.stream().map(UserDto::getLogin).collect(Collectors.toList());
    return dbClient.permissionDao().selectUserPermissionsByLogins(dbSession, logins);
  }
}
