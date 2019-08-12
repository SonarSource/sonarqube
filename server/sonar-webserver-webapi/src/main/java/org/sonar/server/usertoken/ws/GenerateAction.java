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
package org.sonar.server.usertoken.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.usertoken.TokenGenerator;
import org.sonarqube.ws.UserTokens;
import org.sonarqube.ws.UserTokens.GenerateWsResponse;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.usertoken.ws.UserTokenSupport.ACTION_GENERATE;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_LOGIN;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_NAME;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class GenerateAction implements UserTokensWsAction {

  private static final int MAX_TOKEN_NAME_LENGTH = 100;

  private final DbClient dbClient;
  private final System2 system;
  private final TokenGenerator tokenGenerator;
  private final UserTokenSupport userTokenSupport;

  public GenerateAction(DbClient dbClient, System2 system, TokenGenerator tokenGenerator, UserTokenSupport userTokenSupport) {
    this.dbClient = dbClient;
    this.system = system;
    this.tokenGenerator = tokenGenerator;
    this.userTokenSupport = userTokenSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_GENERATE)
      .setSince("5.3")
      .setPost(true)
      .setDescription("Generate a user access token. <br />" +
        "Please keep your tokens secret. They enable to authenticate and analyze projects.<br />" +
        "It requires administration permissions to specify a 'login' and generate a token for another user. Otherwise, a token is generated for the current user.")
      .setResponseExample(getClass().getResource("generate-example.json"))
      .setHandler(this);

    action.createParam(PARAM_LOGIN)
      .setDescription("User login. If not set, the token is generated for the authenticated user.")
      .setExampleValue("g.hopper");

    action.createParam(PARAM_NAME)
      .setRequired(true)
      .setMaximumLength(MAX_TOKEN_NAME_LENGTH)
      .setDescription("Token name")
      .setExampleValue("Project scan on Travis");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserTokens.GenerateWsResponse generateWsResponse = doHandle(request);
    writeProtobuf(generateWsResponse, request, response);
  }

  private UserTokens.GenerateWsResponse doHandle(Request request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      String name = request.mandatoryParam(PARAM_NAME).trim();
      UserDto user = userTokenSupport.getUser(dbSession, request);
      checkTokenDoesNotAlreadyExists(dbSession, user, name);

      String token = tokenGenerator.generate();
      String tokenHash = hashToken(dbSession, token);
      UserTokenDto userTokenDto = insertTokenInDb(dbSession, user, name, tokenHash);
      return buildResponse(userTokenDto, token, user);
    }
  }

  private String hashToken(DbSession dbSession, String token) {
    String tokenHash = tokenGenerator.hash(token);
    UserTokenDto userToken = dbClient.userTokenDao().selectByTokenHash(dbSession, tokenHash);
    if (userToken == null) {
      return tokenHash;
    }
    throw new ServerException(HTTP_INTERNAL_ERROR, "Error while generating token. Please try again.");
  }

  private void checkTokenDoesNotAlreadyExists(DbSession dbSession, UserDto user, String name) {
    UserTokenDto userTokenDto = dbClient.userTokenDao().selectByUserAndName(dbSession, user, name);
    checkRequest(userTokenDto == null, "A user token for login '%s' and name '%s' already exists", user.getLogin(), name);
  }

  private UserTokenDto insertTokenInDb(DbSession dbSession, UserDto user, String name, String tokenHash) {
    UserTokenDto userTokenDto = new UserTokenDto()
      .setUserUuid(user.getUuid())
      .setName(name)
      .setTokenHash(tokenHash)
      .setCreatedAt(system.now());
    dbClient.userTokenDao().insert(dbSession, userTokenDto);
    dbSession.commit();
    return userTokenDto;
  }

  private static GenerateWsResponse buildResponse(UserTokenDto userTokenDto, String token, UserDto user) {
    return UserTokens.GenerateWsResponse.newBuilder()
      .setLogin(user.getLogin())
      .setName(userTokenDto.getName())
      .setCreatedAt(formatDateTime(userTokenDto.getCreatedAt()))
      .setToken(token)
      .build();
  }

}
