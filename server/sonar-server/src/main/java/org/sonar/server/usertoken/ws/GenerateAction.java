/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.base.Optional;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.user.UserSession;
import org.sonar.server.usertoken.TokenGenerator;
import org.sonarqube.ws.WsUserTokens;
import org.sonarqube.ws.WsUserTokens.GenerateWsResponse;
import org.sonarqube.ws.client.usertoken.GenerateWsRequest;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;
import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.ACTION_GENERATE;
import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.PARAM_NAME;

public class GenerateAction implements UserTokensWsAction {
  private final DbClient dbClient;
  private final UserSession userSession;
  private final System2 system;
  private final TokenGenerator tokenGenerator;

  public GenerateAction(DbClient dbClient, UserSession userSession, System2 system, TokenGenerator tokenGenerator) {
    this.userSession = userSession;
    this.dbClient = dbClient;
    this.system = system;
    this.tokenGenerator = tokenGenerator;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_GENERATE)
      .setSince("5.3")
      .setPost(true)
      .setDescription("Generate a user access token. <br />" +
        "Please keep your tokens secret. They enable to authenticate and analyze projects.<br />" +
        "If the login is set, it requires administration permissions. Otherwise, a token is generated for the authenticated user.")
      .setResponseExample(getClass().getResource("generate-example.json"))
      .setHandler(this);

    action.createParam(PARAM_LOGIN)
      .setDescription("User login. If not set, the token is generated for the authenticated user.")
      .setExampleValue("g.hopper");

    action.createParam(PARAM_NAME)
      .setRequired(true)
      .setDescription("Token name")
      .setExampleValue("Project scan on Travis");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    WsUserTokens.GenerateWsResponse generateWsResponse = doHandle(toCreateWsRequest(request));
    writeProtobuf(generateWsResponse, request, response);
  }

  private WsUserTokens.GenerateWsResponse doHandle(GenerateWsRequest request) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      checkWsRequest(dbSession, request);
      TokenPermissionsValidator.validate(userSession, request.getLogin());

      String token = tokenGenerator.generate();
      String tokenHash = hashToken(dbSession, token);

      UserTokenDto userTokenDto = insertTokenInDb(dbSession, request, tokenHash);

      return buildResponse(userTokenDto, token);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private String hashToken(DbSession dbSession, String token) {
    String tokenHash = tokenGenerator.hash(token);
    Optional<UserTokenDto> userToken = dbClient.userTokenDao().selectByTokenHash(dbSession, tokenHash);
    if (userToken.isPresent()) {
      throw new ServerException(HTTP_INTERNAL_ERROR, "Error while generating token. Please try again.");
    }

    return tokenHash;
  }

  private void checkWsRequest(DbSession dbSession, GenerateWsRequest request) {
    checkLoginExists(dbSession, request);

    Optional<UserTokenDto> userTokenDto = dbClient.userTokenDao().selectByLoginAndName(dbSession, request.getLogin(), request.getName());
    checkRequest(!userTokenDto.isPresent(), "A user token with login '%s' and name '%s' already exists", request.getLogin(), request.getName());
  }

  private void checkLoginExists(DbSession dbSession, GenerateWsRequest request) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, request.getLogin());
    if (user == null) {
      throw insufficientPrivilegesException();
    }
  }

  private UserTokenDto insertTokenInDb(DbSession dbSession, GenerateWsRequest request, String tokenHash) {
    UserTokenDto userTokenDto = new UserTokenDto()
      .setLogin(request.getLogin())
      .setName(request.getName())
      .setTokenHash(tokenHash)
      .setCreatedAt(system.now());

    dbClient.userTokenDao().insert(dbSession, userTokenDto);
    dbSession.commit();
    return userTokenDto;
  }

  private GenerateWsRequest toCreateWsRequest(Request request) {
    GenerateWsRequest generateWsRequest = new GenerateWsRequest()
      .setLogin(request.param(PARAM_LOGIN))
      .setName(request.mandatoryParam(PARAM_NAME).trim());
    if (generateWsRequest.getLogin() == null) {
      generateWsRequest.setLogin(userSession.getLogin());
    }

    checkRequest(!generateWsRequest.getName().isEmpty(), "The '%s' parameter must not be blank", PARAM_NAME);

    return generateWsRequest;
  }

  private static GenerateWsResponse buildResponse(UserTokenDto userTokenDto, String token) {
    return WsUserTokens.GenerateWsResponse.newBuilder()
      .setLogin(userTokenDto.getLogin())
      .setName(userTokenDto.getName())
      .setToken(token)
      .build();
  }
}
