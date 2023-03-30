/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.usergroups.ws;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.GroupQuery;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.user.UserSession;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.UserGroups.Group;
import static org.sonarqube.ws.UserGroups.SearchWsResponse;

public class SearchAction implements UserGroupsWsAction {

  private static final String FIELD_NAME = "name";
  private static final String FIELD_DESCRIPTION = "description";
  private static final String FIELD_MEMBERS_COUNT = "membersCount";
  private static final String FIELD_IS_MANAGED = "managed";
  private static final String MANAGED_PARAM = "managed";
  private static final List<String> ALL_FIELDS = Arrays.asList(FIELD_NAME, FIELD_DESCRIPTION, FIELD_MEMBERS_COUNT, FIELD_IS_MANAGED);

  private final DbClient dbClient;
  private final UserSession userSession;
  private final DefaultGroupFinder defaultGroupFinder;
  private final ManagedInstanceService managedInstanceService;

  public SearchAction(DbClient dbClient, UserSession userSession, DefaultGroupFinder defaultGroupFinder, ManagedInstanceService managedInstanceService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.defaultGroupFinder = defaultGroupFinder;
    this.managedInstanceService = managedInstanceService;
  }

  @Override
  public void define(NewController context) {
    WebService.NewAction action = context.createAction("search")
      .setDescription("Search for user groups.<br>" +
        "Requires the following permission: 'Administer System'.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("search-example.json"))
      .setSince("5.2")
      .addFieldsParam(ALL_FIELDS)
      .addPagingParams(100, MAX_PAGE_SIZE)
      .addSearchQuery("sonar-users", "names")
      .setChangelog(
        new Change("10.0", "Field 'id' in the response has been removed"),
        new Change("10.0", "New parameter 'managed' to optionally search by managed status"),
        new Change("10.0", "Response includes 'managed' field."),
        new Change("8.4", "Field 'id' in the response is deprecated. Format changes from integer to string."),
        new Change("6.4", "Paging response fields moved to a Paging object"),
        new Change("6.4", "'default' response field has been added"));

    action.createParam(MANAGED_PARAM)
      .setSince("10.0")
      .setDescription("Return managed or non-managed groups. Only available for managed instances, throws for non-managed instances.")
      .setRequired(false)
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    int page = request.mandatoryParamAsInt(Param.PAGE);
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    SearchOptions options = new SearchOptions()
      .setPage(page, pageSize);

    GroupQuery query = buildGroupQuery(request);
    Set<String> fields = neededFields(request);

    try (DbSession dbSession = dbClient.openSession(false)) {
      userSession.checkLoggedIn().checkPermission(ADMINISTER);
      GroupDto defaultGroup = defaultGroupFinder.findDefaultGroup(dbSession);

      int limit = dbClient.groupDao().countByQuery(dbSession, query);
      Paging paging = forPageIndex(page).withPageSize(pageSize).andTotal(limit);
      List<GroupDto> groups = dbClient.groupDao().selectByQuery(dbSession, query, options.getOffset(), pageSize);
      List<String> groupUuids = extractGroupUuids(groups);
      Map<String, Boolean> groupUuidToIsManaged = managedInstanceService.getGroupUuidToManaged(dbSession, new HashSet<>(groupUuids));
      Map<String, Integer> userCountByGroup = dbClient.groupMembershipDao().countUsersByGroups(dbSession, groupUuids);
      writeProtobuf(buildResponse(groups, userCountByGroup, groupUuidToIsManaged, fields, paging, defaultGroup), request, response);
    }
  }

  private GroupQuery buildGroupQuery(Request request) {
    String textQuery = request.param(Param.TEXT_QUERY);
    Optional<Boolean> managed = Optional.ofNullable(request.paramAsBoolean(MANAGED_PARAM));

    GroupQuery.GroupQueryBuilder queryBuilder = GroupQuery.builder()
      .searchText(textQuery);

    if (managedInstanceService.isInstanceExternallyManaged()) {
      String managedInstanceSql = getManagedInstanceSql(managed);
      queryBuilder.isManagedClause(managedInstanceSql);
    } else if (managed.isPresent()) {
      throw BadRequestException.create("The 'managed' parameter is only available for managed instances.");
    }
    return queryBuilder.build();

  }

  @Nullable
  private String getManagedInstanceSql(Optional<Boolean> managed) {
    return managed
      .map(managedInstanceService::getManagedGroupsSqlFilter)
      .orElse(null);
  }

  private static List<String> extractGroupUuids(List<GroupDto> groups) {
    return groups.stream().map(GroupDto::getUuid).toList();
  }

  private static Set<String> neededFields(Request request) {
    Set<String> fields = new HashSet<>();
    List<String> fieldsFromRequest = request.paramAsStrings(Param.FIELDS);
    if (fieldsFromRequest == null || fieldsFromRequest.isEmpty()) {
      fields.addAll(ALL_FIELDS);
    } else {
      fields.addAll(fieldsFromRequest);
    }
    return fields;
  }

  private static SearchWsResponse buildResponse(List<GroupDto> groups, Map<String, Integer> userCountByGroup,
    Map<String, Boolean> groupUuidToIsManaged, Set<String> fields, Paging paging, GroupDto defaultGroup) {
    SearchWsResponse.Builder responseBuilder = SearchWsResponse.newBuilder();
    groups.forEach(group -> responseBuilder
      .addGroups(toWsGroup(group, userCountByGroup.get(group.getName()), groupUuidToIsManaged.get(group.getUuid()), fields, defaultGroup.getUuid().equals(group.getUuid()))));
    responseBuilder.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();
    return responseBuilder.build();
  }

  private static Group toWsGroup(GroupDto group, Integer memberCount, Boolean isManaged, Set<String> fields, boolean isDefault) {
    Group.Builder groupBuilder = Group.newBuilder()
      .setDefault(isDefault);
    if (fields.contains(FIELD_NAME)) {
      groupBuilder.setName(group.getName());
    }
    if (fields.contains(FIELD_DESCRIPTION)) {
      ofNullable(group.getDescription()).ifPresent(groupBuilder::setDescription);
    }
    if (fields.contains(FIELD_MEMBERS_COUNT)) {
      groupBuilder.setMembersCount(memberCount);
    }
    if (fields.contains(FIELD_IS_MANAGED)) {
      groupBuilder.setManaged(TRUE.equals(isManaged));
    }
    return groupBuilder.build();
  }

}
