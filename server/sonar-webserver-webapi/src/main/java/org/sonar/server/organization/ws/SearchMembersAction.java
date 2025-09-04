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
package org.sonar.server.organization.ws;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.emptyToNull;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static org.sonar.api.server.ws.WebService.SelectionMode.SELECTED;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.es.SearchOptions.MAX_PAGE_SIZE;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

import com.google.common.collect.Multiset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationMemberDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserQuery;
import org.sonar.server.common.avatar.AvatarResolver;
import org.sonar.server.organization.ws.MemberUpdater.MemberType;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Organizations.SearchMembersWsResponse;
import org.sonarqube.ws.Organizations.User;

public class SearchMembersAction implements OrganizationsWsAction {

  private static final String PARAM_ORGANIZATION = "organization";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final AvatarResolver avatarResolver;

  public SearchMembersAction(DbClient dbClient, UserSession userSession, AvatarResolver avatarResolver) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.avatarResolver = avatarResolver;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction("search_members")
      .setDescription("Search members of an organization.<br/>" +
        "Require organization membership.")
      .setResponseExample(getClass().getResource("search_members-example.json"))
      .setSince("6.4")
      .setInternal(true)
      .setChangelog(new Change("7.3", "This action now requires organization membership"))
      .setHandler(this);

    action.createSearchQuery("orwe", "names", "logins")
      .setMinimumLength(2);
    action.addPagingParams(50, MAX_PAGE_SIZE);

    action.createParam(Param.SELECTED)
      .setDescription("Depending on the value, show only selected items (selected=selected) or deselected items (selected=deselected).")
      .setInternal(true)
      .setDefaultValue(SELECTED.value())
      .setPossibleValues(SELECTED.value(), SelectionMode.DESELECTED.value());

    action.createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setInternal(true)
      .setRequired(true);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = getOrganization(dbSession, request.mandatoryParam("organization"));
      userSession.checkMembership(organization);

      var userQuery = buildUserQuery(request, organization);
      var users = fetchUsersAndSortByLogin(request, dbSession, userQuery);
      int totalUsers = dbClient.userDao().countUsers(dbSession, userQuery);
      List<OrganizationMemberDto> organizationMemberDtoList = dbClient.organizationMemberDao().selectAllOrganizationMemberDtos(dbSession, organization.getUuid());

      Multiset<String> groupCountByLogin = null;
      if (userSession.hasPermission(ADMINISTER, organization)) {
        var orderedLogins = users.stream().map(UserDto::getLogin).toList();
        groupCountByLogin = dbClient.groupMembershipDao().countGroupByLoginsAndOrganization(dbSession, orderedLogins, organization.getUuid());
      }
      Map<UserDto,Boolean> adminPrivilegesMap= new HashMap<>();
      for(UserDto userDto: users){
        Set<String> set = dbClient.authorizationDao().selectOrganizationPermissions(dbSession, organization.getUuid(),userDto.getUuid());
        adminPrivilegesMap.put(userDto, set.contains("admin"));
      }
      Paging paging = forPageIndex(request.mandatoryParamAsInt(Param.PAGE)).withPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE)).andTotal(totalUsers);
      SearchMembersWsResponse wsResponse = buildResponse(users, paging, groupCountByLogin, organizationMemberDtoList, adminPrivilegesMap);

      writeProtobuf(wsResponse, request, response);
    }
  }

  private SearchMembersWsResponse buildResponse(List<UserDto> users, Paging wsPaging, @Nullable Multiset<String> groupCountByLogin, List<OrganizationMemberDto> organizationMemberDtoList, Map<UserDto, Boolean> adminPrivilegesMap) {
    SearchMembersWsResponse.Builder response = SearchMembersWsResponse.newBuilder();

    Map<String, String> userUuidTypeMap = organizationMemberDtoList.stream()
            .collect(Collectors.toMap(
                    OrganizationMemberDto::getUserUuid,  // Key: userUuid
                    OrganizationMemberDto::getType,      // Value: type
                    (existing, replacement) -> existing
            ));

    User.Builder wsUser = User.newBuilder();
    users.stream()
      .map(userDto -> {
        String login = userDto.getLogin();
        String userUuid = userDto.getUuid();
        wsUser
          .clear()
          .setLogin(login)
          .setUuid(userDto.getUuid())
          .setType(userUuidTypeMap.getOrDefault(userUuid, MemberType.STANDARD.name()))
          .setIsAdmin(adminPrivilegesMap.getOrDefault(userDto, false));
        ofNullable(emptyToNull(userDto.getEmail())).ifPresent(text -> wsUser.setAvatar(avatarResolver.create(userDto)));
        ofNullable(userDto.getName()).ifPresent(wsUser::setName);
        ofNullable(groupCountByLogin).ifPresent(count -> wsUser.setGroupCount(groupCountByLogin.count(login)));
        return wsUser;
      })
      .forEach(response::addUsers);
    formatPaging(wsPaging, response);

    return response.build();
  }

  private static void formatPaging(Paging paging, SearchMembersWsResponse.Builder response) {
    var wsPaging = Common.Paging.newBuilder()
        .setPageIndex(paging.pageIndex())
        .setPageSize(paging.pageSize())
        .setTotal(paging.total());

    response.setPaging(wsPaging);
  }

  private static UserQuery buildUserQuery(Request request, OrganizationDto organization) {
    String textQuery = request.param(Param.TEXT_QUERY);
    checkArgument(textQuery == null || textQuery.length() >= 2, "Query length must be greater than or equal to 2");

    var userQuery = UserQuery.builder();
    userQuery.searchText(textQuery);
    userQuery.isActive(true);

    var selectionMode = SelectionMode.fromParam(request.mandatoryParam(Param.SELECTED));
    if (SelectionMode.DESELECTED.equals(selectionMode)) {
      userQuery.addExcludedOrganizationUuids(organization.getUuid());
    } else {
      userQuery.addOrganizationUuids(List.of(organization.getUuid()));
    }

    return userQuery.build();
  }

  private List<UserDto> fetchUsersAndSortByLogin(Request request, DbSession dbSession, UserQuery userQuery) {
    return dbClient.userDao().selectUsers(dbSession, userQuery, request.mandatoryParamAsInt(Param.PAGE), request.mandatoryParamAsInt(Param.PAGE_SIZE))
        .stream()
        .sorted(comparing(UserDto::getLogin))
        .toList();
  }

  private OrganizationDto getOrganization(DbSession dbSession, @Nullable String organizationKey) {
    return checkFoundWithOptional(
      dbClient.organizationDao().selectByKey(dbSession, organizationKey),
      "No organization with key '%s'", organizationKey);
  }
}
