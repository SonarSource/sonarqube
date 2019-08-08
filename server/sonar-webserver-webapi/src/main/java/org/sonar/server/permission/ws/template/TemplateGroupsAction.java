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
import org.sonar.api.security.DefaultGroups;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.permission.PermissionQuery;
import org.sonar.db.permission.template.PermissionTemplateDto;
import org.sonar.db.permission.template.PermissionTemplateGroupDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.permission.ws.PermissionWsSupport;
import org.sonar.server.permission.ws.PermissionsWsAction;
import org.sonar.server.permission.RequestValidator;
import org.sonar.server.permission.ws.WsParameters;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Permissions;

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

public class TemplateGroupsAction implements PermissionsWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final PermissionWsSupport wsSupport;
  private final WsParameters wsParameters;
  private final RequestValidator requestValidator;

  public TemplateGroupsAction(DbClient dbClient, UserSession userSession, PermissionWsSupport wsSupport, WsParameters wsParameters, RequestValidator requestValidator) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.wsSupport = wsSupport;
    this.wsParameters = wsParameters;
    this.requestValidator = requestValidator;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("template_groups")
      .setSince("5.2")
      .setInternal(true)
      .setDescription("Lists the groups with their permission as individual groups rather than through user affiliation on the chosen template.<br />" +
        "This service defaults to all groups, but can be limited to groups with a specific permission by providing the desired permission.<br>" +
        "Requires the following permission: 'Administer System'.")
      .addPagingParams(DEFAULT_PAGE_SIZE, RESULTS_MAX_SIZE)
      .setResponseExample(getClass().getResource("template_groups-example.json"))
      .setHandler(this);

    action.createParam(TEXT_QUERY)
      .setMinimumLength(SEARCH_QUERY_MIN_LENGTH)
      .setDescription("Limit search to group names that contain the supplied string. <br/>" +
        "When this parameter is not set, only group having at least one permission are returned.")
      .setExampleValue("eri");

    wsParameters.createProjectPermissionParameter(action, false);
    createTemplateParameters(action);
  }

  @Override
  public void handle(Request wsRequest, Response wsResponse) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      WsTemplateRef templateRef = WsTemplateRef.fromRequest(wsRequest);
      PermissionTemplateDto template = wsSupport.findTemplate(dbSession, templateRef);
      checkGlobalAdmin(userSession, template.getOrganizationUuid());

      PermissionQuery query = buildPermissionQuery(wsRequest, template);
      int total = dbClient.permissionTemplateDao().countGroupNamesByQueryAndTemplate(dbSession, query, template.getOrganizationUuid(), template.getId());
      Paging paging = Paging.forPageIndex(wsRequest.mandatoryParamAsInt(PAGE)).withPageSize(wsRequest.mandatoryParamAsInt(PAGE_SIZE)).andTotal(total);
      List<GroupDto> groups = findGroups(dbSession, query, template);
      List<PermissionTemplateGroupDto> groupPermissions = findGroupPermissions(dbSession, groups, template);
      Permissions.WsGroupsResponse groupsResponse = buildResponse(groups, groupPermissions, paging);
      writeProtobuf(groupsResponse, wsRequest, wsResponse);
    }
  }

  private PermissionQuery buildPermissionQuery(Request request, PermissionTemplateDto template) {
    String textQuery = request.param(TEXT_QUERY);
    String permission = request.param(PARAM_PERMISSION);
    PermissionQuery.Builder permissionQuery = PermissionQuery.builder()
      .setOrganizationUuid(template.getOrganizationUuid())
      .setPermission(permission != null ? requestValidator.validateProjectPermission(permission) : null)
      .setPageIndex(request.mandatoryParamAsInt(PAGE))
      .setPageSize(request.mandatoryParamAsInt(PAGE_SIZE))
      .setSearchQuery(textQuery);
    return permissionQuery.build();
  }

  private static Permissions.WsGroupsResponse buildResponse(List<GroupDto> groups, List<PermissionTemplateGroupDto> groupPermissions, Paging paging) {
    Multimap<Integer, String> permissionsByGroupId = TreeMultimap.create();
    groupPermissions.forEach(groupPermission -> permissionsByGroupId.put(groupPermission.getGroupId(), groupPermission.getPermission()));
    Permissions.WsGroupsResponse.Builder response = Permissions.WsGroupsResponse.newBuilder();

    groups.forEach(group -> {
      Permissions.Group.Builder wsGroup = response.addGroupsBuilder()
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

  private List<GroupDto> findGroups(DbSession dbSession, PermissionQuery dbQuery, PermissionTemplateDto template) {
    List<String> orderedNames = dbClient.permissionTemplateDao().selectGroupNamesByQueryAndTemplate(dbSession, dbQuery, template.getId());
    List<GroupDto> groups = dbClient.groupDao().selectByNames(dbSession, template.getOrganizationUuid(), orderedNames);
    if (orderedNames.contains(DefaultGroups.ANYONE)) {
      groups.add(0, new GroupDto().setId(0).setName(DefaultGroups.ANYONE));
    }
    return Ordering.explicit(orderedNames).onResultOf(GroupDto::getName).immutableSortedCopy(groups);
  }

  private List<PermissionTemplateGroupDto> findGroupPermissions(DbSession dbSession, List<GroupDto> groups, PermissionTemplateDto template) {
    List<String> names = groups.stream().map(GroupDto::getName).collect(Collectors.toList());
    return dbClient.permissionTemplateDao().selectGroupPermissionsByTemplateIdAndGroupNames(dbSession, template.getId(), names);
  }
}
