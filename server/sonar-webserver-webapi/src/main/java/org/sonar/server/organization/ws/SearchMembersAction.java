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
package org.sonar.server.organization.ws;

import com.google.common.collect.Multiset;
import com.google.common.collect.Ordering;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.Param;
import org.sonar.api.server.ws.WebService.SelectionMode;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.issue.AvatarResolver;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserQuery;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.Organizations.SearchMembersWsResponse;
import org.sonarqube.ws.Organizations.User;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.emptyToNull;
import static java.util.Optional.ofNullable;
import static org.sonar.api.server.ws.WebService.SelectionMode.SELECTED;
import static org.sonar.db.permission.OrganizationPermission.ADMINISTER;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;
import static org.sonar.server.organization.ws.OrganizationsWsSupport.PARAM_ORGANIZATION;
import static org.sonar.server.exceptions.NotFoundException.checkFoundWithOptional;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class SearchMembersAction implements OrganizationsWsAction {

  private final DbClient dbClient;
  private final UserIndex userIndex;
  private final DefaultOrganizationProvider organizationProvider;
  private final UserSession userSession;
  private final AvatarResolver avatarResolver;

  public SearchMembersAction(DbClient dbClient, UserIndex userIndex, DefaultOrganizationProvider organizationProvider, UserSession userSession, AvatarResolver avatarResolver) {
    this.dbClient = dbClient;
    this.userIndex = userIndex;
    this.organizationProvider = organizationProvider;
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
    action.addPagingParams(50, MAX_LIMIT);

    action.createParam(Param.SELECTED)
      .setDescription("Depending on the value, show only selected items (selected=selected) or deselected items (selected=deselected).")
      .setInternal(true)
      .setDefaultValue(SELECTED.value())
      .setPossibleValues(SELECTED.value(), SelectionMode.DESELECTED.value());

    action.createParam(PARAM_ORGANIZATION)
      .setDescription("Organization key")
      .setInternal(true)
      .setRequired(false);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    try (DbSession dbSession = dbClient.openSession(false)) {
      OrganizationDto organization = getOrganization(dbSession, request.param("organization"));
      userSession.checkMembership(organization);

      UserQuery.Builder userQuery = buildUserQuery(request, organization);
      SearchOptions searchOptions = buildSearchOptions(request);

      SearchResult<UserDoc> searchResults = userIndex.search(userQuery.build(), searchOptions);
      List<String> orderedLogins = searchResults.getDocs().stream().map(UserDoc::login).collect(MoreCollectors.toList());

      List<UserDto> users = dbClient.userDao().selectByLogins(dbSession, orderedLogins).stream()
        .sorted(Ordering.explicit(orderedLogins).onResultOf(UserDto::getLogin))
        .collect(MoreCollectors.toList());

      Multiset<String> groupCountByLogin = null;
      if (userSession.hasPermission(ADMINISTER, organization)) {
        groupCountByLogin = dbClient.groupMembershipDao().countGroupByLoginsAndOrganization(dbSession, orderedLogins, organization.getUuid());
      }

      Common.Paging wsPaging = buildWsPaging(request, searchResults);
      SearchMembersWsResponse wsResponse = buildResponse(users, wsPaging, groupCountByLogin);

      writeProtobuf(wsResponse, request, response);
    }
  }

  private SearchMembersWsResponse buildResponse(List<UserDto> users, Common.Paging wsPaging, @Nullable Multiset<String> groupCountByLogin) {
    SearchMembersWsResponse.Builder response = SearchMembersWsResponse.newBuilder();

    User.Builder wsUser = User.newBuilder();
    users.stream()
      .map(userDto -> {
        String login = userDto.getLogin();
        wsUser
          .clear()
          .setLogin(login);
        ofNullable(emptyToNull(userDto.getEmail())).ifPresent(text -> wsUser.setAvatar(avatarResolver.create(userDto)));
        ofNullable(userDto.getName()).ifPresent(wsUser::setName);
        ofNullable(groupCountByLogin).ifPresent(count -> wsUser.setGroupCount(groupCountByLogin.count(login)));
        return wsUser;
      })
      .forEach(response::addUsers);
    response.setPaging(wsPaging);

    return response.build();
  }

  private static UserQuery.Builder buildUserQuery(Request request, OrganizationDto organization) {
    UserQuery.Builder userQuery = UserQuery.builder();
    String textQuery = request.param(Param.TEXT_QUERY);
    checkArgument(textQuery == null || textQuery.length() >= 2, "Query length must be greater than or equal to 2");
    userQuery.setTextQuery(textQuery);

    SelectionMode selectionMode = SelectionMode.fromParam(request.mandatoryParam(Param.SELECTED));
    if (SelectionMode.DESELECTED.equals(selectionMode)) {
      userQuery.setExcludedOrganizationUuid(organization.getUuid());
    } else {
      userQuery.setOrganizationUuid(organization.getUuid());
    }
    return userQuery;
  }

  private static SearchOptions buildSearchOptions(Request request) {
    int pageSize = request.mandatoryParamAsInt(Param.PAGE_SIZE);
    return new SearchOptions().setPage(request.mandatoryParamAsInt(Param.PAGE), pageSize);
  }

  private static Common.Paging buildWsPaging(Request request, SearchResult<UserDoc> searchResults) {
    return Common.Paging.newBuilder()
      .setPageIndex(request.mandatoryParamAsInt(Param.PAGE))
      .setPageSize(request.mandatoryParamAsInt(Param.PAGE_SIZE))
      .setTotal((int) searchResults.getTotal())
      .build();
  }

  private OrganizationDto getOrganization(DbSession dbSession, @Nullable String organizationParam) {
    String organizationKey = Optional.ofNullable(organizationParam)
      .orElseGet(organizationProvider.get()::getKey);
    return checkFoundWithOptional(
      dbClient.organizationDao().selectByKey(dbSession, organizationKey),
      "No organization with key '%s'", organizationKey);
  }
}
