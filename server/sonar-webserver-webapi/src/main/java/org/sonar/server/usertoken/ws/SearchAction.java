/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.usertoken.ws;

import java.util.List;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonarqube.ws.UserTokens.SearchWsResponse;
import org.sonarqube.ws.UserTokens.SearchWsResponse.UserToken;

import static java.util.Optional.ofNullable;
import static org.elasticsearch.common.Strings.isNullOrEmpty;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.usertoken.ws.UserTokenSupport.ACTION_SEARCH;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_LOGIN;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.UserTokens.SearchWsResponse.UserToken.Project;
import static org.sonarqube.ws.UserTokens.SearchWsResponse.UserToken.newBuilder;

public class SearchAction implements UserTokensWsAction {

  private final DbClient dbClient;
  private final UserTokenSupport userTokenSupport;

  public SearchAction(DbClient dbClient, UserTokenSupport userTokenSupport) {
    this.dbClient = dbClient;
    this.userTokenSupport = userTokenSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_SEARCH)
      .setDescription("List the access tokens of a user.<br>" +
        "The login must exist and active.<br>" +
        "Field 'lastConnectionDate' is only updated every hour, so it may not be accurate, for instance when a user is using a token many times in less than one hour.<br> " +
        "It requires administration permissions to specify a 'login' and list the tokens of another user. Otherwise, tokens for the current user are listed. <br> " +
        "Authentication is required for this API endpoint")
      .setChangelog(new Change("9.6", "New field 'expirationDate' is added to response"))
      .setChangelog(new Change("7.7", "New field 'lastConnectionDate' is added to response"))
      .setResponseExample(getClass().getResource("search-example.json"))
      .setSince("5.3")
      .setHandler(this);

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setExampleValue("g.hopper");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    SearchWsResponse searchWsResponse = doHandle(request);
    writeProtobuf(searchWsResponse, request, response);
  }

  private SearchWsResponse doHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = userTokenSupport.getUser(dbSession, request);
      List<UserTokenDto> userTokens = dbClient.userTokenDao().selectByUser(dbSession, user);
      return buildResponse(user, userTokens);
    }
  }

  private static SearchWsResponse buildResponse(UserDto user, List<UserTokenDto> userTokensDto) {
    SearchWsResponse.Builder searchWsResponse = SearchWsResponse.newBuilder();
    UserToken.Builder userTokenBuilder = newBuilder();
    searchWsResponse.setLogin(user.getLogin());
    for (UserTokenDto userTokenDto : userTokensDto) {
      userTokenBuilder
        .clear()
        .setName(userTokenDto.getName())
        .setCreatedAt(formatDateTime(userTokenDto.getCreatedAt()))
        .setType(userTokenDto.getType());
      ofNullable(userTokenDto.getLastConnectionDate()).ifPresent(date -> userTokenBuilder.setLastConnectionDate(formatDateTime(date)));
      ofNullable(userTokenDto.getExpirationDate()).ifPresent(expirationDate -> {
        userTokenBuilder.setExpirationDate(formatDateTime(expirationDate));
        userTokenBuilder.setIsExpired(userTokenDto.isExpired());
      });

      if (!isNullOrEmpty(userTokenDto.getProjectKey()) && !isNullOrEmpty(userTokenDto.getProjectName())) {
        Project.Builder projectBuilder = newBuilder().getProjectBuilder()
          .setKey(userTokenDto.getProjectKey())
          .setName(userTokenDto.getProjectName());
        userTokenBuilder.setProject(projectBuilder.build());
      }
      searchWsResponse.addUserTokens(userTokenBuilder);
    }

    return searchWsResponse.build();
  }

}
