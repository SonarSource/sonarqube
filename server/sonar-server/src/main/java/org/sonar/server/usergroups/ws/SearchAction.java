/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Set;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.GroupDto;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.user.UserSession;
import org.sonar.server.usergroups.DefaultGroupFinder;

import static org.apache.commons.lang.StringUtils.defaultIfBlank;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_ORGANIZATION_KEY;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.UserGroups.Group;
import static org.sonarqube.ws.UserGroups.SearchWsResponse;

public class SearchAction implements UserGroupsWsAction {

  private static final String FIELD_NAME = "name";
  private static final String FIELD_DESCRIPTION = "description";
  private static final String FIELD_MEMBERS_COUNT = "membersCount";
  private static final List<String> ALL_FIELDS = Arrays.asList(FIELD_NAME, FIELD_DESCRIPTION, FIELD_MEMBERS_COUNT);

  private final DbClient dbClient;
  private final UserSession userSession;
  private final GroupWsSupport groupWsSupport;
  private final DefaultGroupFinder defaultGroupFinder;

  public SearchAction(DbClient dbClient, UserSession userSession, GroupWsSupport groupWsSupport, DefaultGroupFinder defaultGroupFinder) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.groupWsSupport = groupWsSupport;
    this.defaultGroupFinder = defaultGroupFinder;
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
      .addPagingParams(100, MAX_LIMIT)
      .addSearchQuery("sonar-users", "names")
      .setChangelog(new Change("6.4", "Paging response fields moved to a Paging object"),
        new Change("6.4", "'default' response field has been added"));

    action.createParam(PARAM_ORGANIZATION_KEY)
      .setDescription("Key of organization. If not set then groups are searched in default organization.")
      .setExampleValue("my-org")
      .setSince("6.2")
      .setInternal(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    int page = request.mandatoryParamAsInt(Param.PAGE);
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    SearchOptions options = new SearchOptions()
      .setPage(page, pageSize);

    String query = defaultIfBlank(request.param(Param.TEXT_QUERY), "");
    Set<String> fields = neededFields(request);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = groupWsSupport.findOrganizationByKey(dbSession, request.param(PARAM_ORGANIZATION_KEY));
      userSession.checkLoggedIn().checkPermission(ADMINISTER, organization);
      GroupDto defaultGroup = defaultGroupFinder.findDefaultGroup(dbSession, organization.getUuid());

      int limit = dbClient.groupDao().countByQuery(dbSession, organization.getUuid(), query);
      Paging paging = forPageIndex(page).withPageSize(pageSize).andTotal(limit);
      List<GroupDto> groups = dbClient.groupDao().selectByQuery(dbSession, organization.getUuid(), query, options.getOffset(), pageSize);
      List<Integer> groupIds = groups.stream().map(GroupDto::getId).collect(MoreCollectors.toList(groups.size()));
      Map<String, Integer> userCountByGroup = dbClient.groupMembershipDao().countUsersByGroups(dbSession, groupIds);
      writeProtobuf(buildResponse(groups, userCountByGroup, fields, paging, defaultGroup), request, response);
    }
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

  private static SearchWsResponse buildResponse(List<GroupDto> groups, Map<String, Integer> userCountByGroup, Set<String> fields, Paging paging, GroupDto defaultGroup) {
    SearchWsResponse.Builder responseBuilder = SearchWsResponse.newBuilder();
    groups.forEach(group -> responseBuilder
      .addGroups(toWsGroup(group, userCountByGroup.get(group.getName()), fields, defaultGroup.getId().equals(group.getId()))));
    responseBuilder.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();
    return responseBuilder.build();
  }

  private static Group toWsGroup(GroupDto group, Integer memberCount, Set<String> fields, boolean isDefault) {
    Group.Builder groupBuilder = Group.newBuilder()
      .setId(group.getId())
      .setDefault(isDefault);
    if (fields.contains(FIELD_NAME)) {
      groupBuilder.setName(group.getName());
    }
    if (fields.contains(FIELD_DESCRIPTION)) {
      setNullable(group.getDescription(), groupBuilder::setDescription);
    }
    if (fields.contains(FIELD_MEMBERS_COUNT)) {
      groupBuilder.setMembersCount(memberCount);
    }
    return groupBuilder.build();
  }

}
