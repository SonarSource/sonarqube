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
package org.sonar.server.user.ws;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.ServletFilter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.authentication.JwtHttpHandler;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.ws.ServletFilterHandler;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static org.sonarqube.ws.WsUtils.checkArgument;
import static org.sonarqube.ws.WsUtils.isNullOrEmpty;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PASSWORD;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PREVIOUS_PASSWORD;

public class ChangePasswordAction extends ServletFilter implements BaseUsersWsAction {

  private static final String CHANGE_PASSWORD = "change_password";
  private static final String CHANGE_PASSWORD_URL = "/" + UsersWs.API_USERS + "/" + CHANGE_PASSWORD;
  private static final String MSG_PARAMETER_MISSING = "The '%s' parameter is missing";

  private final DbClient dbClient;
  private final UserUpdater userUpdater;
  private final UserSession userSession;
  private final CredentialsLocalAuthentication localAuthentication;
  private final JwtHttpHandler jwtHttpHandler;

  public ChangePasswordAction(DbClient dbClient, UserUpdater userUpdater, UserSession userSession, CredentialsLocalAuthentication localAuthentication,
    JwtHttpHandler jwtHttpHandler) {
    this.dbClient = dbClient;
    this.userUpdater = userUpdater;
    this.userSession = userSession;
    this.localAuthentication = localAuthentication;
    this.jwtHttpHandler = jwtHttpHandler;
  }

  @Override
  public UrlPattern doGetPattern() {
    return UrlPattern.create(CHANGE_PASSWORD_URL);
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(CHANGE_PASSWORD)
      .setDescription("Update a user's password. Authenticated users can change their own password, " +
        "provided that the account is not linked to an external authentication system. " +
        "Administer System permission is required to change another user's password.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(ServletFilterHandler.INSTANCE)
      .setChangelog(new Change("8.6", "It's no more possible for the password to be the same as the previous one"));

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("myuser");

    action.createParam(PARAM_PASSWORD)
      .setDescription("New password")
      .setRequired(true)
      .setExampleValue("mypassword");

    action.createParam(PARAM_PREVIOUS_PASSWORD)
      .setDescription("Previous password. Required when changing one's own password.")
      .setRequired(false)
      .setExampleValue("oldpassword");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    userSession.checkLoggedIn();
    try (DbSession dbSession = dbClient.openSession(false)) {
      String login = getParamOrThrow(request, PARAM_LOGIN);
      String newPassword = getParamOrThrow(request, PARAM_PASSWORD);
      UserDto user;

      if (login.equals(userSession.getLogin())) {
        user = getUserOrThrow(dbSession, login);
        String previousPassword = getParamOrThrow(request, PARAM_PREVIOUS_PASSWORD);
        checkPreviousPassword(dbSession, user, previousPassword);
        checkArgument(!previousPassword.equals(newPassword), "Password must be different from old password");
        deleteTokensAndRefreshSession(request, response, dbSession, user);
      } else {
        userSession.checkIsSystemAdministrator();
        user = getUserOrThrow(dbSession, login);
        dbClient.sessionTokensDao().deleteByUser(dbSession, user);
      }
      updatePassword(dbSession, user, newPassword);
      setResponseStatus(response, HTTP_NO_CONTENT);
    } catch (BadRequestException badRequestException) {
      setResponseStatus(response, HTTP_BAD_REQUEST);
    }
  }

  private static String getParamOrThrow(ServletRequest request, String key) {
    String value = request.getParameter(key);
    checkArgument(!isNullOrEmpty(value), MSG_PARAMETER_MISSING, key);
    return value;
  }

  private void checkPreviousPassword(DbSession dbSession, UserDto user, String password) {
    try {
      localAuthentication.authenticate(dbSession, user, password, AuthenticationEvent.Method.BASIC);
    } catch (AuthenticationException ex) {
      throw new IllegalArgumentException("Incorrect password");
    }
  }

  private UserDto getUserOrThrow(DbSession dbSession, String login) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
    if (user == null || !user.isActive()) {
      throw new NotFoundException(format("User with login '%s' has not been found", login));
    }
    return user;
  }

  private void deleteTokensAndRefreshSession(ServletRequest request, ServletResponse response, DbSession dbSession, UserDto user) {
    dbClient.sessionTokensDao().deleteByUser(dbSession, user);
    refreshJwtToken(request, response, user);
  }

  private void refreshJwtToken(ServletRequest request, ServletResponse response, UserDto user) {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    jwtHttpHandler.removeToken(httpRequest, httpResponse);
    jwtHttpHandler.generateToken(user, httpRequest, httpResponse);
  }

  private void updatePassword(DbSession dbSession, UserDto user, String newPassword) {
    UpdateUser updateUser = new UpdateUser().setPassword(newPassword);
    userUpdater.updateAndCommit(dbSession, user, updateUser, u -> {
    });
  }

  private static void setResponseStatus(ServletResponse response, int newStatusCode) {
    ((HttpServletResponse) response).setStatus(newStatusCode);
  }
}
