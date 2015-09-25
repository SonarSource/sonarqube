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

import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.db.permission.UserWithPermissionDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsPermissions.WsTemplateUsersResponse;
import org.sonarqube.ws.WsPermissions.WsTemplateUsersResponse.User;

import static java.lang.String.format;
import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.ws.PermissionQueryParser.fromSelectionModeToMembership;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPermission;
import static org.sonar.server.permission.ws.WsPermissionParameters.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.WsPermissionParameters.createProjectPermissionParameter;
import static org.sonar.server.permission.ws.WsPermissionParameters.createTemplateParameters;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class TemplateUsersAction implements PermissionsWsAction {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionDependenciesFinder dependenciesFinder;

  public TemplateUsersAction(DbClient dbClient, UserSession userSession, PermissionDependenciesFinder dependenciesFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.dependenciesFinder = dependenciesFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context
      .createAction("template_users")
      .setSince("5.2")
      .setDescription(
        format("Lists the users that have been granted the specified permission as individual users rather than through group affiliation on the chosen template. <br />" +
          "If the query parameter '%s' is specified, the '%s' parameter is forced to '%s'.<br />" +
          "It requires administration permissions to access.<br />",
          Param.TEXT_QUERY, Param.SELECTED, SelectionMode.ALL.value()))
      .addPagingParams(100)
      .addSearchQuery("stas", "names")
      .addSelectionModeParam()
      .setInternal(true)
      .setResponseExample(getClass().getResource("template-users-example.json"))
      .setHandler(this);

    createProjectPermissionParameter(action);
    createTemplateParameters(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    checkGlobalAdminUser(userSession);
    DbSession dbSession = dbClient.openSession(false);
    try {
      WsTemplateRef templateRef = WsTemplateRef.fromRequest(wsRequest);
      PermissionTemplateDto template = dependenciesFinder.getTemplate(dbSession, templateRef);

      PermissionQuery query = buildQuery(wsRequest, template);
      WsTemplateUsersResponse templateUsersResponse = buildResponse(dbSession, query, template);
      writeProtobuf(templateUsersResponse, wsRequest, wsResponse);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static PermissionQuery buildQuery(Request wsRequest, PermissionTemplateDto template) {
    String permission = validateProjectPermission(wsRequest.mandatoryParam(PARAM_PERMISSION));

    return PermissionQuery.builder()
      .template(template.getUuid())
      .permission(permission)
      .membership(fromSelectionModeToMembership(wsRequest.mandatoryParam(Param.SELECTED)))
      .pageIndex(wsRequest.mandatoryParamAsInt(Param.PAGE))
      .pageSize(wsRequest.mandatoryParamAsInt(Param.PAGE_SIZE))
      .search(wsRequest.param(Param.TEXT_QUERY))
      .build();
  }

  private WsTemplateUsersResponse buildResponse(DbSession dbSession, PermissionQuery query, PermissionTemplateDto template) {
    List<UserWithPermissionDto> usersWithPermission = dbClient.permissionTemplateDao().selectUsers(dbSession, query, template.getId(), offset(query), query.pageSize());
    int total = dbClient.permissionTemplateDao().countUsers(dbSession, query, template.getId());
    WsTemplateUsersResponse.Builder responseBuilder = WsTemplateUsersResponse.newBuilder();
    for (UserWithPermissionDto userWithPermission : usersWithPermission) {
      responseBuilder.addUsers(userDtoToUserResponse(userWithPermission));
    }

    responseBuilder.getPagingBuilder()
      .setPageIndex(query.pageIndex())
      .setPageSize(query.pageSize())
      .setTotal(total)
      .build();

    return responseBuilder.build();
  }

  private static int offset(PermissionQuery query) {
    int pageSize = query.pageSize();
    int pageIndex = query.pageIndex();
    return (pageIndex - 1) * pageSize;
  }

  private static User userDtoToUserResponse(UserWithPermissionDto userWithPermission) {
    User.Builder userBuilder = User.newBuilder();
    userBuilder.setLogin(userWithPermission.getLogin());
    String email = userWithPermission.getEmail();
    if (email != null) {
      userBuilder.setEmail(email);
    }
    String name = userWithPermission.getName();
    if (name != null) {
      userBuilder.setName(name);
    }
    userBuilder.setSelected(userWithPermission.getPermission() != null);

    return userBuilder.build();
  }
}
