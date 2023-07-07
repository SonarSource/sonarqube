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
package org.sonar.server.user.ws;

import com.google.common.collect.Multimap;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserQuery;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.issue.AvatarResolver;
import org.sonar.server.management.ManagedInstanceService;
import org.sonar.server.user.UserSession;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.Users.SearchWsResponse;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.emptyToNull;
import static java.lang.Boolean.TRUE;
import static java.util.Comparator.comparing;
import static java.util.Optional.ofNullable;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Users.SearchWsResponse.Groups;
import static org.sonarqube.ws.Users.SearchWsResponse.ScmAccounts;
import static org.sonarqube.ws.Users.SearchWsResponse.User;
import static org.sonarqube.ws.Users.SearchWsResponse.newBuilder;

public class SearchAction implements UsersWsAction {
  private static final String DEACTIVATED_PARAM = "deactivated";
  private static final String MANAGED_PARAM = "managed";


  private static final int MAX_PAGE_SIZE = 500;
  static final String LAST_CONNECTION_DATE_FROM = "lastConnectedAfter";
  static final String LAST_CONNECTION_DATE_TO = "lastConnectedBefore";
  static final String SONAR_LINT_LAST_CONNECTION_DATE_FROM = "slLastConnectedAfter";
  static final String SONAR_LINT_LAST_CONNECTION_DATE_TO = "slLastConnectedBefore";
  private final UserSession userSession;
  private final DbClient dbClient;
  private final AvatarResolver avatarResolver;
  private final ManagedInstanceService managedInstanceService;

  public SearchAction(UserSession userSession, DbClient dbClient, AvatarResolver avatarResolver,
    ManagedInstanceService managedInstanceService) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.avatarResolver = avatarResolver;
    this.managedInstanceService = managedInstanceService;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("search")
      .setDescription("Get a list of users. By default, only active users are returned.<br/>" +
        "The following fields are only returned when user has Administer System permission or for logged-in in user :" +
        "<ul>" +
        "   <li>'email'</li>" +
        "   <li>'externalIdentity'</li>" +
        "   <li>'externalProvider'</li>" +
        "   <li>'groups'</li>" +
        "   <li>'lastConnectionDate'</li>" +
        "   <li>'sonarLintLastConnectionDate'</li>" +
        "   <li>'tokensCount'</li>" +
        "</ul>" +
        "Field 'lastConnectionDate' is only updated every hour, so it may not be accurate, for instance when a user authenticates many times in less than one hour.")
      .setSince("3.6")
      .setChangelog(
        new Change("10.1", "New optional parameters " + SONAR_LINT_LAST_CONNECTION_DATE_FROM +
          " and " + SONAR_LINT_LAST_CONNECTION_DATE_TO + " to filter users by SonarLint last connection date. Only available with Administer System permission."),
        new Change("10.1", "New optional parameters " + LAST_CONNECTION_DATE_FROM +
          " and " + LAST_CONNECTION_DATE_TO + " to filter users by SonarQube last connection date. Only available with Administer System permission."),
        new Change("10.1", "New field 'sonarLintLastConnectionDate' is added to response"),
        new Change("10.0", "'q' parameter values is now always performing a case insensitive match"),
        new Change("10.0", "New parameter 'managed' to optionally search by managed status"),
        new Change("10.0", "Response includes 'managed' field."),
        new Change("9.7", "New parameter 'deactivated' to optionally search for deactivated users"),
        new Change("7.7", "New field 'lastConnectionDate' is added to response"),
        new Change("7.4", "External identity is only returned to system administrators"),
        new Change("6.4", "Paging response fields moved to a Paging object"),
        new Change("6.4", "Avatar has been added to the response"),
        new Change("6.4", "Email is only returned when user has Administer System permission"))
      .setHandler(this)
      .setResponseExample(getClass().getResource("search-example.json"));

    action.addPagingParams(50, SearchOptions.MAX_PAGE_SIZE);

    final String dateExample = "2020-01-01T00:00:00+0100";
    action.createParam(TEXT_QUERY)
      .setMinimumLength(2)
      .setDescription("Filter on login, name and email.<br />" +
        "This parameter can either perform an exact match, or a partial match (contains), it is case insensitive.");
    action.createParam(DEACTIVATED_PARAM)
      .setSince("9.7")
      .setDescription("Return deactivated users instead of active users")
      .setRequired(false)
      .setDefaultValue(false)
      .setBooleanPossibleValues();
    action.createParam(MANAGED_PARAM)
      .setSince("10.0")
      .setDescription("Return managed or non-managed users. Only available for managed instances, throws for non-managed instances.")
      .setRequired(false)
      .setDefaultValue(null)
      .setBooleanPossibleValues();
    action.createParam(LAST_CONNECTION_DATE_FROM)
      .setSince("10.1")
      .setDescription("""
        Filter the users based on the last connection date field.
        Only users who interacted with this instance at or after the date will be returned.
        The format must be ISO 8601 datetime format (YYYY-MM-DDThh:mm:ss±hhmm)""")
      .setRequired(false)
      .setDefaultValue(null)
      .setExampleValue(dateExample);
    action.createParam(LAST_CONNECTION_DATE_TO)
      .setSince("10.1")
      .setDescription("""
        Filter the users based on the last connection date field.
        Only users that never connected or who interacted with this instance at or before the date will be returned.
        The format must be ISO 8601 datetime format (YYYY-MM-DDThh:mm:ss±hhmm)""")
      .setRequired(false)
      .setDefaultValue(null)
      .setExampleValue(dateExample);
    action.createParam(SONAR_LINT_LAST_CONNECTION_DATE_FROM)
      .setSince("10.1")
      .setDescription("""
        Filter the users based on the sonar lint last connection date field 
        Only users who interacted with this instance using SonarLint at or after the date will be returned.
        The format must be ISO 8601 datetime format (YYYY-MM-DDThh:mm:ss±hhmm)""")
      .setRequired(false)
      .setDefaultValue(null)
      .setExampleValue(dateExample);
    action.createParam(SONAR_LINT_LAST_CONNECTION_DATE_TO)
      .setSince("10.1")
      .setDescription("""
        Filter the users based on the sonar lint last connection date field.
        Only users that never connected or who interacted with this instance using SonarLint at or before the date will be returned.
        The format must be ISO 8601 datetime format (YYYY-MM-DDThh:mm:ss±hhmm)""")
      .setRequired(false)
      .setDefaultValue(null)
      .setExampleValue(dateExample);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Users.SearchWsResponse wsResponse = doHandle(toSearchRequest(request));
    writeProtobuf(wsResponse, request, response);
  }

  private Users.SearchWsResponse doHandle(SearchRequest request) {
    UserQuery userQuery = buildUserQuery(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<UserDto> users = findUsersAndSortByLogin(request, dbSession, userQuery);
      int totalUsers = dbClient.userDao().countUsers(dbSession, userQuery);

      List<String> logins = users.stream().map(UserDto::getLogin).toList();
      Multimap<String, String> groupsByLogin = dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, logins);
      Map<String, Integer> tokenCountsByLogin = dbClient.userTokenDao().countTokensByUsers(dbSession, users);
      Map<String, Boolean> userUuidToIsManaged = managedInstanceService.getUserUuidToManaged(dbSession, getUserUuids(users));
      Paging paging = forPageIndex(request.getPage()).withPageSize(request.getPageSize()).andTotal(totalUsers);
      return buildResponse(users, groupsByLogin, tokenCountsByLogin, userUuidToIsManaged, paging);
    }
  }

  private static Set<String> getUserUuids(List<UserDto> users) {
    return users.stream().map(UserDto::getUuid).collect(Collectors.toSet());
  }

  private UserQuery buildUserQuery(SearchRequest request) {
    UserQuery.UserQueryBuilder builder = UserQuery.builder();
    if(!userSession.isSystemAdministrator()) {
      request.getLastConnectionDateFrom().ifPresent(v -> throwForbiddenFor(LAST_CONNECTION_DATE_FROM));
      request.getLastConnectionDateTo().ifPresent(v -> throwForbiddenFor(LAST_CONNECTION_DATE_TO));
      request.getSonarLintLastConnectionDateFrom().ifPresent(v -> throwForbiddenFor(SONAR_LINT_LAST_CONNECTION_DATE_FROM));
      request.getSonarLintLastConnectionDateTo().ifPresent(v -> throwForbiddenFor(SONAR_LINT_LAST_CONNECTION_DATE_TO));
    }
    request.getLastConnectionDateFrom().ifPresent(builder::lastConnectionDateFrom);
    request.getLastConnectionDateTo().ifPresent(builder::lastConnectionDateTo);
    request.getSonarLintLastConnectionDateFrom().ifPresent(builder::sonarLintLastConnectionDateFrom);
    request.getSonarLintLastConnectionDateTo().ifPresent(builder::sonarLintLastConnectionDateTo);

    if (managedInstanceService.isInstanceExternallyManaged()) {
      String managedInstanceSql = Optional.ofNullable(request.isManaged())
        .map(managedInstanceService::getManagedUsersSqlFilter)
        .orElse(null);
      builder.isManagedClause(managedInstanceSql);
    } else if (request.isManaged() != null) {
      throw BadRequestException.create("The 'managed' parameter is only available for managed instances.");
    }

    return builder
      .isActive(!request.isDeactivated())
      .searchText(request.getQuery())
      .build();
  }

  private static void throwForbiddenFor(String parameterName) {
    throw new ServerException(403, "parameter " + parameterName + " requires Administer System permission.");
  }

  private List<UserDto> findUsersAndSortByLogin(SearchRequest request, DbSession dbSession, UserQuery userQuery) {
    return dbClient.userDao().selectUsers(dbSession, userQuery, request.getPage(), request.getPageSize())
      .stream()
      .sorted(comparing(UserDto::getLogin))
      .toList();
  }

  private SearchWsResponse buildResponse(List<UserDto> users, Multimap<String, String> groupsByLogin, Map<String, Integer> tokenCountsByLogin,
    Map<String, Boolean> userUuidToIsManaged, Paging paging) {
    SearchWsResponse.Builder responseBuilder = newBuilder();
    users.forEach(user -> responseBuilder.addUsers(
      towsUser(user, firstNonNull(tokenCountsByLogin.get(user.getUuid()), 0), groupsByLogin.get(user.getLogin()), userUuidToIsManaged.get(user.getUuid()))
    ));
    responseBuilder.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();
    return responseBuilder.build();
  }

  private User towsUser(UserDto user, @Nullable Integer tokensCount, Collection<String> groups, Boolean managed) {
    User.Builder userBuilder = User.newBuilder().setLogin(user.getLogin());
    ofNullable(user.getName()).ifPresent(userBuilder::setName);
    if (userSession.isLoggedIn()) {
      ofNullable(emptyToNull(user.getEmail())).ifPresent(u -> userBuilder.setAvatar(avatarResolver.create(user)));
      userBuilder.setActive(user.isActive());
      userBuilder.setLocal(user.isLocal());
      ofNullable(user.getExternalIdentityProvider()).ifPresent(userBuilder::setExternalProvider);
      if (!user.getSortedScmAccounts().isEmpty()) {
        userBuilder.setScmAccounts(ScmAccounts.newBuilder().addAllScmAccounts(user.getSortedScmAccounts()));
      }
    }
    if (userSession.isSystemAdministrator() || Objects.equals(userSession.getUuid(), user.getUuid())) {
      ofNullable(user.getEmail()).ifPresent(userBuilder::setEmail);
      if (!groups.isEmpty()) {
        userBuilder.setGroups(Groups.newBuilder().addAllGroups(groups));
      }
      ofNullable(user.getExternalLogin()).ifPresent(userBuilder::setExternalIdentity);
      ofNullable(tokensCount).ifPresent(userBuilder::setTokensCount);
      ofNullable(user.getLastConnectionDate()).map(DateUtils::formatDateTime).ifPresent(userBuilder::setLastConnectionDate);
      ofNullable(user.getLastSonarlintConnectionDate())
        .map(DateUtils::formatDateTime).ifPresent(userBuilder::setSonarLintLastConnectionDate);
      userBuilder.setManaged(TRUE.equals(managed));
    }
    return userBuilder.build();
  }

  private static SearchRequest toSearchRequest(Request request) {
    int pageSize = request.mandatoryParamAsInt(PAGE_SIZE);
    checkArgument(pageSize <= MAX_PAGE_SIZE, "The '%s' parameter must be less than %s", PAGE_SIZE, MAX_PAGE_SIZE);
    return SearchRequest.builder()
      .setQuery(request.param(TEXT_QUERY))
      .setDeactivated(request.mandatoryParamAsBoolean(DEACTIVATED_PARAM))
      .setManaged(request.paramAsBoolean(MANAGED_PARAM))
      .setLastConnectionDateFrom(request.param(LAST_CONNECTION_DATE_FROM))
      .setLastConnectionDateTo(request.param(LAST_CONNECTION_DATE_TO))
      .setSonarLintLastConnectionDateFrom(request.param(SONAR_LINT_LAST_CONNECTION_DATE_FROM))
      .setSonarLintLastConnectionDateTo(request.param(SONAR_LINT_LAST_CONNECTION_DATE_TO))
      .setPage(request.mandatoryParamAsInt(PAGE))
      .setPageSize(pageSize)
      .build();
  }

  private static class SearchRequest {
    private final Integer page;
    private final Integer pageSize;
    private final String query;
    private final boolean deactivated;
    private final Boolean managed;
    private final OffsetDateTime lastConnectionDateFrom;
    private final OffsetDateTime lastConnectionDateTo;
    private final OffsetDateTime sonarLintLastConnectionDateFrom;
    private final OffsetDateTime sonarLintLastConnectionDateTo;

    private SearchRequest(Builder builder) {
      this.page = builder.page;
      this.pageSize = builder.pageSize;
      this.query = builder.query;
      this.deactivated = builder.deactivated;
      this.managed = builder.managed;
      try {
        this.lastConnectionDateFrom = Optional.ofNullable(builder.lastConnectionDateFrom).map(DateUtils::parseOffsetDateTime).orElse(null);
        this.lastConnectionDateTo = Optional.ofNullable(builder.lastConnectionDateTo).map(DateUtils::parseOffsetDateTime).orElse(null);
        this.sonarLintLastConnectionDateFrom = Optional.ofNullable(builder.sonarLintLastConnectionDateFrom).map(DateUtils::parseOffsetDateTime).orElse(null);
        this.sonarLintLastConnectionDateTo = Optional.ofNullable(builder.sonarLintLastConnectionDateTo).map(DateUtils::parseOffsetDateTime).orElse(null);
      } catch (MessageException me) {
        throw new ServerException(400, me.getMessage());
      }
    }

    public Integer getPage() {
      return page;
    }

    public Integer getPageSize() {
      return pageSize;
    }

    @CheckForNull
    public String getQuery() {
      return query;
    }

    public boolean isDeactivated() {
      return deactivated;
    }

    @CheckForNull
    private Boolean isManaged() {
      return managed;
    }

    public Optional<OffsetDateTime> getLastConnectionDateFrom() {
      return Optional.ofNullable(lastConnectionDateFrom);
    }

    public Optional<OffsetDateTime> getLastConnectionDateTo() {
      return Optional.ofNullable(lastConnectionDateTo);
    }

    public Optional<OffsetDateTime> getSonarLintLastConnectionDateFrom() {
      return Optional.ofNullable(sonarLintLastConnectionDateFrom);
    }

    public Optional<OffsetDateTime> getSonarLintLastConnectionDateTo() {
      return Optional.ofNullable(sonarLintLastConnectionDateTo);
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  private static class Builder {
    private Integer page;
    private Integer pageSize;
    private String query;
    private boolean deactivated;
    private Boolean managed;
    private String lastConnectionDateFrom;
    private String lastConnectionDateTo;
    private String sonarLintLastConnectionDateFrom;
    private String sonarLintLastConnectionDateTo;


    private Builder() {
      // enforce factory method use
    }

    public Builder setPage(Integer page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public Builder setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    public Builder setDeactivated(boolean deactivated) {
      this.deactivated = deactivated;
      return this;
    }

    public Builder setManaged(@Nullable Boolean managed) {
      this.managed = managed;
      return this;
    }

    public Builder setLastConnectionDateFrom(@Nullable String lastConnectionDateFrom) {
      this.lastConnectionDateFrom = lastConnectionDateFrom;
      return this;
    }

    public Builder setLastConnectionDateTo(@Nullable String lastConnectionDateTo) {
      this.lastConnectionDateTo = lastConnectionDateTo;
      return this;
    }

    public Builder setSonarLintLastConnectionDateFrom(@Nullable String sonarLintLastConnectionDateFrom) {
      this.sonarLintLastConnectionDateFrom = sonarLintLastConnectionDateFrom;
      return this;
    }

    public Builder setSonarLintLastConnectionDateTo(@Nullable String sonarLintLastConnectionDateTo) {
      this.sonarLintLastConnectionDateTo = sonarLintLastConnectionDateTo;
      return this;
    }

    public SearchRequest build() {
      return new SearchRequest(this);
    }
  }
}
