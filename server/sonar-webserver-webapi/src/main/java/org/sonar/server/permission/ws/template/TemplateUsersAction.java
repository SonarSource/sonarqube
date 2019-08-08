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
package org.sonar.server.permission.ws.template;

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
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateUserDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.AvatarResolver;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.permission.RequestValidator;
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Permissions;
import org.sonarqube.ws.Permissions.UsersWsResponse;

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Optional.ofNullable;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.db.permission.PermissionQuery.DEFAULT_PAGE_SIZE;
import static org.sonar.db.permission.PermissionQuery.RESULTS_MAX_SIZE;
import static org.sonar.db.permission.PermissionQuery.SEARCH_QUERY_MIN_LENGTH;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdmin;
import static org.sonar.server.permission.ws.WsParameters.createTemplateParameters;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;

public class TemplateUsersAction implements PermissionsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionWsSupport wsSupport;
  private final AvatarResolver avatarResolver;
  private final WsParameters wsParameters;
  private final RequestValidator requestValidator;

  public TemplateUsersAction(DbClient dbClient, UserSession userSession, PermissionWsSupport wsSupport, AvatarResolver avatarResolver,
    WsParameters wsParameters, RequestValidator requestValidator) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
    this.avatarResolver = avatarResolver;
    this.wsParameters = wsParameters;
    this.requestValidator = requestValidator;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction("template_users")
      .setSince("5.2")
      .setDescription("Lists the users with their permission as individual users rather than through group affiliation on the chosen template. <br />" +
        "This service defaults to all users, but can be limited to users with a specific permission by providing the desired permission.<br>" +
        "Requires the following permission: 'Administer System'.")
      .addPagingParams(DEFAULT_PAGE_SIZE, RESULTS_MAX_SIZE)
      .setInternal(true)
      .setResponseExample(getClass().getResource("template_users-example.json"))
      .setHandler(this);

    action.createParam(Param.TEXT_QUERY)
      .setMinimumLength(SEARCH_QUERY_MIN_LENGTH)
      .setDescription("Limit search to user names that contain the supplied string. <br/>" +
        "When this parameter is not set, only users having at least one permission are returned.")
      .setExampleValue("eri");
    wsParameters.createProjectPermissionParameter(action).setRequired(false);
    createTemplateParameters(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      WsTemplateRef templateRef = WsTemplateRef.fromRequest(wsRequest);
      PermissionTemplateDto template = wsSupport.findTemplate(dbSession, templateRef);
      checkGlobalAdmin(userSession, template.getOrganizationUuid());

      PermissionQuery query = buildQuery(wsRequest, template);
      int total = dbClient.permissionTemplateDao().countUserLoginsByQueryAndTemplate(dbSession, query, template.getId());
      Paging paging = Paging.forPageIndex(wsRequest.mandatoryParamAsInt(PAGE)).withPageSize(wsRequest.mandatoryParamAsInt(PAGE_SIZE)).andTotal(total);
      List<UserDto> users = findUsers(dbSession, query, template);
      List<PermissionTemplateUserDto> permissionTemplateUsers = dbClient.permissionTemplateDao().selectUserPermissionsByTemplateIdAndUserLogins(dbSession, template.getId(),
        users.stream().map(UserDto::getLogin).collect(Collectors.toList()));
      Permissions.UsersWsResponse templateUsersResponse = buildResponse(users, permissionTemplateUsers, paging);
      writeProtobuf(templateUsersResponse, wsRequest, wsResponse);
    }
  }

  private PermissionQuery buildQuery(Request wsRequest, PermissionTemplateDto template) {
    String textQuery = wsRequest.param(TEXT_QUERY);
    String permission = wsRequest.param(PARAM_PERMISSION);
    PermissionQuery.Builder query = PermissionQuery.builder()
      .setOrganizationUuid(template.getOrganizationUuid())
      .setTemplate(template.getUuid())
      .setPermission(permission != null ? requestValidator.validateProjectPermission(permission) : null)
      .setPageIndex(wsRequest.mandatoryParamAsInt(PAGE))
      .setPageSize(wsRequest.mandatoryParamAsInt(PAGE_SIZE))
      .setSearchQuery(textQuery);
    return query.build();
  }

  private Permissions.UsersWsResponse buildResponse(List<UserDto> users, List<PermissionTemplateUserDto> permissionTemplateUsers, Paging paging) {
    Multimap<Integer, String> permissionsByUserId = TreeMultimap.create();
    permissionTemplateUsers.forEach(userPermission -> permissionsByUserId.put(userPermission.getUserId(), userPermission.getPermission()));

    UsersWsResponse.Builder responseBuilder = UsersWsResponse.newBuilder();
    users.forEach(user -> {
      Permissions.User.Builder userResponse = responseBuilder.addUsersBuilder()
        .setLogin(user.getLogin())
        .addAllPermissions(permissionsByUserId.get(user.getId()));
      ofNullable(user.getEmail()).ifPresent(userResponse::setEmail);
      ofNullable(user.getName()).ifPresent(userResponse::setName);
      ofNullable(emptyToNull(user.getEmail())).ifPresent(u -> userResponse.setAvatar(avatarResolver.create(user)));
    });
    responseBuilder.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();
    return responseBuilder.build();
  }

  private List<UserDto> findUsers(DbSession dbSession, PermissionQuery query, PermissionTemplateDto template) {
    List<String> orderedLogins = dbClient.permissionTemplateDao().selectUserLoginsByQueryAndTemplate(dbSession, query, template.getId());
    return Ordering.explicit(orderedLogins).onResultOf(UserDto::getLogin).immutableSortedCopy(dbClient.userDao().selectByLogins(dbSession, orderedLogins));
  }

}
