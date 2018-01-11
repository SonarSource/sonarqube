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
import org.sonarqube.ws.UserTokens;
import org.sonarqube.ws.UserTokens.GenerateWsResponse;

import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.usertoken.ws.UserTokensWsParameters.ACTION_GENERATE;
import static org.sonar.server.usertoken.ws.UserTokensWsParameters.PARAM_LOGIN;
import static org.sonar.server.usertoken.ws.UserTokensWsParameters.PARAM_NAME;
import static org.sonar.server.ws.WsUtils.checkRequest;
import static org.sonar.server.ws.WsUtils.writeProtobuf;

public class GenerateAction implements UserTokensWsAction {
  private static final int MAX_TOKEN_NAME_LENGTH = 100;
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
      .setMaximumLength(MAX_TOKEN_NAME_LENGTH)
      .setDescription("Token name")
      .setExampleValue("Project scan on Travis");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserTokens.GenerateWsResponse generateWsResponse = doHandle(toCreateWsRequest(request));
    writeProtobuf(generateWsResponse, request, response);
  }

  private UserTokens.GenerateWsResponse doHandle(GenerateRequest request) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      checkWsRequest(dbSession, request);
      TokenPermissionsValidator.validate(userSession, request.getLogin());

      String token = tokenGenerator.generate();
      String tokenHash = hashToken(dbSession, token);

      UserTokenDto userTokenDto = insertTokenInDb(dbSession, request, tokenHash);

      return buildResponse(userTokenDto, token);
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

  private void checkWsRequest(DbSession dbSession, GenerateRequest request) {
    checkLoginExists(dbSession, request);

    Optional<UserTokenDto> userTokenDto = dbClient.userTokenDao().selectByLoginAndName(dbSession, request.getLogin(), request.getName());
    checkRequest(!userTokenDto.isPresent(), "A user token with login '%s' and name '%s' already exists", request.getLogin(), request.getName());
  }

  private void checkLoginExists(DbSession dbSession, GenerateRequest request) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, request.getLogin());
    if (user == null) {
      throw insufficientPrivilegesException();
    }
  }

  private UserTokenDto insertTokenInDb(DbSession dbSession, GenerateRequest request, String tokenHash) {
    UserTokenDto userTokenDto = new UserTokenDto()
      .setLogin(request.getLogin())
      .setName(request.getName())
      .setTokenHash(tokenHash)
      .setCreatedAt(system.now());

    dbClient.userTokenDao().insert(dbSession, userTokenDto);
    dbSession.commit();
    return userTokenDto;
  }

  private GenerateRequest toCreateWsRequest(Request request) {
    GenerateRequest generateWsRequest = new GenerateRequest()
      .setLogin(request.param(PARAM_LOGIN))
      .setName(request.mandatoryParam(PARAM_NAME).trim());
    if (generateWsRequest.getLogin() == null) {
      generateWsRequest.setLogin(userSession.getLogin());
    }

    checkRequest(!generateWsRequest.getName().isEmpty(), "The '%s' parameter must not be blank", PARAM_NAME);

    return generateWsRequest;
  }

  private static GenerateWsResponse buildResponse(UserTokenDto userTokenDto, String token) {
    return UserTokens.GenerateWsResponse.newBuilder()
      .setLogin(userTokenDto.getLogin())
      .setName(userTokenDto.getName())
      .setCreatedAt(formatDateTime(userTokenDto.getCreatedAt()))
      .setToken(token)
      .build();
  }

  private static class GenerateRequest {

    private String login;
    private String name;

    public GenerateRequest setLogin(String login) {
      this.login = login;
      return this;
    }

    public String getLogin() {
      return login;
    }

    public GenerateRequest setName(String name) {
      this.name = name;
      return this;
    }

    public String getName() {
      return name;
    }
  }
}
