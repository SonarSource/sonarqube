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
package org.sonar.server.user.ws;

import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.Paging;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.es.SearchResult;
import org.sonar.server.issue.ws.AvatarResolver;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;
import org.sonar.server.user.index.UserQuery;
import org.sonarqube.ws.Users;
import org.sonarqube.ws.Users.SearchWsResponse;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.emptyToNull;
import static org.sonar.api.server.ws.WebService.Param.FIELDS;
import static org.sonar.api.server.ws.WebService.Param.PAGE;
import static org.sonar.api.server.ws.WebService.Param.PAGE_SIZE;
import static org.sonar.api.server.ws.WebService.Param.TEXT_QUERY;
import static org.sonar.api.utils.Paging.forPageIndex;
import static org.sonar.core.util.stream.MoreCollectors.toList;
import static org.sonar.server.es.SearchOptions.MAX_LIMIT;
import static org.sonar.server.user.ws.UserJsonWriter.FIELD_ACTIVE;
import static org.sonar.server.user.ws.UserJsonWriter.FIELD_AVATAR;
import static org.sonar.server.user.ws.UserJsonWriter.FIELD_EMAIL;
import static org.sonar.server.user.ws.UserJsonWriter.FIELD_EXTERNAL_IDENTITY;
import static org.sonar.server.user.ws.UserJsonWriter.FIELD_EXTERNAL_PROVIDER;
import static org.sonar.server.user.ws.UserJsonWriter.FIELD_GROUPS;
import static org.sonar.server.user.ws.UserJsonWriter.FIELD_LOCAL;
import static org.sonar.server.user.ws.UserJsonWriter.FIELD_NAME;
import static org.sonar.server.user.ws.UserJsonWriter.FIELD_SCM_ACCOUNTS;
import static org.sonar.server.user.ws.UserJsonWriter.FIELD_TOKENS_COUNT;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.Users.SearchWsResponse.Groups;
import static org.sonarqube.ws.Users.SearchWsResponse.ScmAccounts;
import static org.sonarqube.ws.Users.SearchWsResponse.User;
import static org.sonarqube.ws.Users.SearchWsResponse.newBuilder;

public class SearchAction implements UsersWsAction {

  private static final int MAX_PAGE_SIZE = 500;

  private final UserSession userSession;
  private final UserIndex userIndex;
  private final DbClient dbClient;
  private final AvatarResolver avatarResolver;

  public SearchAction(UserSession userSession, UserIndex userIndex, DbClient dbClient, AvatarResolver avatarResolver) {
    this.userSession = userSession;
    this.userIndex = userIndex;
    this.dbClient = dbClient;
    this.avatarResolver = avatarResolver;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("search")
      .setDescription("Get a list of active users. <br/>" +
        "Administer System permission is required to show the 'groups' field.<br/>" +
        "When accessed anonymously, only logins and names are returned.")
      .setSince("3.6")
      .setChangelog(
        new Change("6.4", "Paging response fields moved to a Paging object"),
        new Change("6.4", "Avatar has been added to the response"),
        new Change("6.4", "Email is only returned when user has Administer System permission"))
      .setHandler(this)
      .setResponseExample(getClass().getResource("search-example.json"));

    action.createFieldsParam(UserJsonWriter.FIELDS)
      .setDeprecatedSince("5.4");
    action.addPagingParams(50, MAX_LIMIT);

    action.createParam(TEXT_QUERY)
      .setMinimumLength(2)
      .setDescription("Filter on login or name.");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    Users.SearchWsResponse wsResponse = doHandle(toSearchRequest(request));
    writeProtobuf(wsResponse, request, response);
  }

  private Users.SearchWsResponse doHandle(SearchRequest request) {
    SearchOptions options = new SearchOptions().setPage(request.getPage(), request.getPageSize());
    List<String> fields = request.getPossibleFields();
    SearchResult<UserDoc> result = userIndex.search(UserQuery.builder().setTextQuery(request.getQuery()).build(), options);
    try (DbSession dbSession = dbClient.openSession(false)) {
      List<String> logins = result.getDocs().stream().map(UserDoc::login).collect(toList());
      Multimap<String, String> groupsByLogin = dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, logins);
      Map<String, Integer> tokenCountsByLogin = dbClient.userTokenDao().countTokensByLogins(dbSession, logins);
      List<UserDto> users = dbClient.userDao().selectByOrderedLogins(dbSession, logins);
      Paging paging = forPageIndex(request.getPage()).withPageSize(request.getPageSize()).andTotal((int) result.getTotal());
      return buildResponse(users, groupsByLogin, tokenCountsByLogin, fields, paging);
    }
  }

  private SearchWsResponse buildResponse(List<UserDto> users, Multimap<String, String> groupsByLogin, Map<String, Integer> tokenCountsByLogin,
    @Nullable List<String> fields, Paging paging) {
    SearchWsResponse.Builder responseBuilder = newBuilder();
    users.forEach(user -> responseBuilder.addUsers(towsUser(user, firstNonNull(tokenCountsByLogin.get(user.getLogin()), 0), groupsByLogin.get(user.getLogin()), fields)));
    responseBuilder.getPagingBuilder()
      .setPageIndex(paging.pageIndex())
      .setPageSize(paging.pageSize())
      .setTotal(paging.total())
      .build();
    return responseBuilder.build();
  }

  private User towsUser(UserDto user, @Nullable Integer tokensCount, Collection<String> groups, @Nullable Collection<String> fields) {
    User.Builder userBuilder = User.newBuilder()
      .setLogin(user.getLogin());
    setIfNeeded(FIELD_NAME, fields, user.getName(), userBuilder::setName);
    if (userSession.isLoggedIn()) {
      setIfNeeded(FIELD_AVATAR, fields, emptyToNull(user.getEmail()), u -> userBuilder.setAvatar(avatarResolver.create(user)));
      setIfNeeded(FIELD_ACTIVE, fields, user.isActive(), userBuilder::setActive);
      setIfNeeded(FIELD_LOCAL, fields, user.isLocal(), userBuilder::setLocal);
      setIfNeeded(FIELD_EXTERNAL_IDENTITY, fields, user.getExternalIdentity(), userBuilder::setExternalIdentity);
      setIfNeeded(FIELD_EXTERNAL_PROVIDER, fields, user.getExternalIdentityProvider(), userBuilder::setExternalProvider);
      setIfNeeded(FIELD_TOKENS_COUNT, fields, tokensCount, userBuilder::setTokensCount);
      setIfNeeded(isNeeded(FIELD_SCM_ACCOUNTS, fields) && !user.getScmAccountsAsList().isEmpty(), user.getScmAccountsAsList(),
        scm -> userBuilder.setScmAccounts(ScmAccounts.newBuilder().addAllScmAccounts(scm)));
    }
    if (userSession.isSystemAdministrator()) {
      setIfNeeded(FIELD_EMAIL, fields, user.getEmail(), userBuilder::setEmail);
      setIfNeeded(isNeeded(FIELD_GROUPS, fields) && !groups.isEmpty(), groups,
        g -> userBuilder.setGroups(Groups.newBuilder().addAllGroups(g)));
    }
    return userBuilder.build();
  }

  private static <PARAM> void setIfNeeded(String field, @Nullable Collection<String> fields, @Nullable PARAM parameter, Function<PARAM, ?> setter) {
    setIfNeeded(isNeeded(field, fields), parameter, setter);
  }

  private static <PARAM> void setIfNeeded(boolean condition, @Nullable PARAM parameter, Function<PARAM, ?> setter) {
    if (parameter != null && condition) {
      setter.apply(parameter);
    }
  }

  private static boolean isNeeded(String field, @Nullable Collection<String> fields) {
    return fields == null || fields.isEmpty() || fields.contains(field);
  }

  private static SearchRequest toSearchRequest(Request request) {
    int pageSize = request.mandatoryParamAsInt(PAGE_SIZE);
    checkArgument(pageSize <= MAX_PAGE_SIZE, "The '%s' parameter must be less than %s", PAGE_SIZE, MAX_PAGE_SIZE);
    return SearchRequest.builder()
      .setQuery(request.param(TEXT_QUERY))
      .setPage(request.mandatoryParamAsInt(PAGE))
      .setPageSize(pageSize)
      .setPossibleFields(request.paramAsStrings(FIELDS))
      .build();
  }

  private static class SearchRequest {

    private final Integer page;
    private final Integer pageSize;
    private final String query;
    private final List<String> possibleFields;

    private SearchRequest(Builder builder) {
      this.page = builder.page;
      this.pageSize = builder.pageSize;
      this.query = builder.query;
      this.possibleFields = builder.additionalFields;
    }

    @CheckForNull
    public Integer getPage() {
      return page;
    }

    @CheckForNull
    public Integer getPageSize() {
      return pageSize;
    }

    @CheckForNull
    public String getQuery() {
      return query;
    }

    public List<String> getPossibleFields() {
      return possibleFields;
    }

    public static Builder builder() {
      return new Builder();
    }
  }

  private static class Builder {
    private Integer page;
    private Integer pageSize;
    private String query;
    private List<String> additionalFields = new ArrayList<>();

    private Builder() {
      // enforce factory method use
    }

    public Builder setPage(@Nullable Integer page) {
      this.page = page;
      return this;
    }

    public Builder setPageSize(@Nullable Integer pageSize) {
      this.pageSize = pageSize;
      return this;
    }

    public Builder setQuery(@Nullable String query) {
      this.query = query;
      return this;
    }

    public Builder setPossibleFields(List<String> possibleFields) {
      this.additionalFields = possibleFields;
      return this;
    }

    public SearchRequest build() {
      return new SearchRequest(this);
    }
  }
}
