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
import org.sonar.db.permission.GroupWithPermissionDto;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.PermissionTemplateDto;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.WsPermissions.Group;
import org.sonarqube.ws.WsPermissions.WsGroupsResponse;

import static org.sonar.server.permission.PermissionPrivilegeChecker.checkGlobalAdminUser;
import static org.sonar.server.permission.ws.PermissionQueryParser.fromSelectionModeToMembership;
import static org.sonar.server.permission.ws.PermissionRequestValidator.validateProjectPermission;
import static org.sonarqube.ws.client.permission.PermissionsWsParameters.PARAM_PERMISSION;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createProjectPermissionParameter;
import static org.sonar.server.permission.ws.PermissionsWsParametersBuilder.createTemplateParameters;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class TemplateGroupsAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionDependenciesFinder dependenciesFinder;

  public TemplateGroupsAction(DbClient dbClient, UserSession userSession, PermissionDependenciesFinder dependenciesFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.dependenciesFinder = dependenciesFinder;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("template_groups")
      .setSince("5.2")
      .setInternal(true)
      .setDescription(String.format("Lists the groups that have been explicitly granted the specified project permission on a permission template. <br />" +
        "If the query parameter '%s' is specified, the '%s' parameter is forced to '%s'. <br />" +
        "It requires administration permissions to access.",
        Param.TEXT_QUERY, Param.SELECTED, SelectionMode.ALL.value()))
      .addPagingParams(100)
      .addSearchQuery("sonar", "names")
      .addSelectionModeParam()
      .setResponseExample(getClass().getResource("template_groups-example.json"))
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
      WsGroupsResponse groupsResponse = buildResponse(dbSession, query, template);

      writeProtobuf(groupsResponse, wsRequest, wsResponse);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private WsGroupsResponse buildResponse(DbSession dbSession, PermissionQuery query, PermissionTemplateDto template) {
    int total = dbClient.permissionTemplateDao().countGroups(dbSession, query, template.getId());
    List<GroupWithPermissionDto> groupsWithPermission = dbClient.permissionTemplateDao().selectGroups(dbSession, query, template.getId(), query.pageOffset(), query.pageSize());

    WsGroupsResponse.Builder groupsResponse = WsGroupsResponse.newBuilder();

    for (GroupWithPermissionDto groupWithPermission : groupsWithPermission) {
      groupsResponse.addGroups(groupDtoToGroupResponse(groupWithPermission));
    }

    groupsResponse.getPagingBuilder()
      .setPageIndex(query.pageIndex())
      .setPageSize(query.pageSize())
      .setTotal(total)
      .build();

    return groupsResponse.build();
  }

  private static PermissionQuery buildQuery(Request request, PermissionTemplateDto template) {
    String permission = validateProjectPermission(request.mandatoryParam(PARAM_PERMISSION));

    PermissionQuery.Builder permissionQuery = PermissionQuery.builder()
      .permission(permission)
      .pageIndex(request.mandatoryParamAsInt(Param.PAGE))
      .pageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .membership(fromSelectionModeToMembership(request.mandatoryParam(Param.SELECTED)))
      .template(template.getUuid())
      .search(request.param(Param.TEXT_QUERY));

    return permissionQuery.build();
  }

  private static Group groupDtoToGroupResponse(GroupWithPermissionDto groupDto) {
    Group.Builder groupBuilder = Group.newBuilder();
    groupBuilder
      .setName(groupDto.getName())
      .setSelected(groupDto.getPermission() != null);
    // anyone group return with id = 0
    if (groupDto.getId() != 0) {
      groupBuilder.setId(String.valueOf(groupDto.getId()));
    }
    if (groupDto.getDescription() != null) {
      groupBuilder.setDescription(groupDto.getDescription());
    }

    return groupBuilder.build();
  }
}
