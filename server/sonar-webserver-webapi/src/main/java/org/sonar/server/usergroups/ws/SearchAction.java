/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.stream.Collectors;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.permission.OrganizationPermission;
import org.sonar.db.user.GroupDto;
import org.sonar.server.common.SearchResults;
import org.sonar.server.common.group.service.GroupInformation;
import org.sonar.server.common.group.service.GroupSearchRequest;
import org.sonar.server.common.group.service.GroupService;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.usergroups.ws.GroupWsSupport.PARAM_ORGANIZATION_KEY;
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

  private final GroupService groupService;

  public SearchAction(DbClient dbClient, UserSession userSession, GroupService groupService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.groupService = groupService;
  }

  @Override
  public void define(NewController context) {
    WebService.NewAction action = context.createAction("search")
      .setDescription("Search for user groups.<br>" +
        "Requires the following permission: 'Administer System'.")
      .setHandler(this)
      .setResponseExample(getClass().getResource("search-example.json"))
      .setSince("5.2")
      .setDeprecatedSince("10.4")
      .addFieldsParam(ALL_FIELDS)
      .addPagingParams(100, MAX_PAGE_SIZE)
      .addSearchQuery("sonar-users", "names")
      .setChangelog(
        new Change("10.4", "Deprecated. Use GET /api/v2/authorizations/groups instead"),
        new Change("10.0", "Field 'id' in the response has been removed"),
        new Change("10.0", "New parameter 'managed' to optionally search by managed status"),
        new Change("10.0", "Response includes 'managed' field."),
        new Change("8.4", "Field 'id' in the response is deprecated. Format changes from integer to string."),
        new Change("6.4", "Paging response fields moved to a Paging object"),
        new Change("6.4", "'default' response field has been added"));

    action.createParam(PARAM_ORGANIZATION_KEY)
            .setDescription("Key of organization. If not set then groups are searched in default organization.")
            .setExampleValue("my-org")
            .setRequired(true)
            .setSince("6.2")
            .setInternal(true);

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

    Set<String> fields = neededFields(request);

    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = dbClient.organizationDao().selectByKey(dbSession, request.mandatoryParam(PARAM_ORGANIZATION_KEY))
          .orElseThrow(() -> new NotFoundException("No organization found with key: " + request.mandatoryParam(PARAM_ORGANIZATION_KEY)));
      userSession.checkPermission(OrganizationPermission.ADMINISTER, organization);

      userSession.checkLoggedIn().checkPermission(OrganizationPermission.ADMINISTER, organization);

      GroupSearchRequest groupSearchRequest = new GroupSearchRequest(organization, request.param(Param.TEXT_QUERY), request.paramAsBoolean(MANAGED_PARAM), page, pageSize);
      SearchResults<GroupInformation> searchResults = groupService.search(dbSession, groupSearchRequest);

      Set<String> groupUuids = extractGroupUuids(searchResults.searchResults());

      Map<String, Integer> userCountByGroup = dbClient.groupMembershipDao().countUsersByGroups(dbSession, groupUuids);
      Paging paging = forPageIndex(page).withPageSize(pageSize).andTotal(searchResults.total());
      writeProtobuf(buildResponse(searchResults.searchResults(), userCountByGroup, fields, paging), request, response);
    }
  }

  private static Set<String> extractGroupUuids(List<GroupInformation> groupInformations) {
    return groupInformations.stream()
      .map(groupInformation -> groupInformation.groupDto().getUuid())
      .collect(Collectors.toSet());
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

  private static SearchWsResponse buildResponse(List<GroupInformation> groups, Map<String, Integer> userCountByGroup,
   Set<String> fields, Paging paging) {
    SearchWsResponse.Builder responseBuilder = SearchWsResponse.newBuilder();
    groups.forEach(group -> responseBuilder
      .addGroups(toWsGroup(group.groupDto(), userCountByGroup.get(group.groupDto().getName()), group.isManaged(), fields, group.isDefault())));
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
