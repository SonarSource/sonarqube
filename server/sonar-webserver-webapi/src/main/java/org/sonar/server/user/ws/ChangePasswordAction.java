/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.http.HttpRequest;
import org.sonar.api.server.http.HttpResponse;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.web.FilterChain;
import org.sonar.api.web.HttpFilter;
import org.sonar.api.web.UrlPattern;
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

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.sonar.server.user.ws.ChangePasswordAction.PasswordMessage.NEW_PASSWORD_SAME_AS_OLD;
import static org.sonar.server.user.ws.ChangePasswordAction.PasswordMessage.OLD_PASSWORD_INCORRECT;
import static org.sonarqube.ws.MediaTypes.JSON;
import static org.sonarqube.ws.WsUtils.isNullOrEmpty;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PASSWORD;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PREVIOUS_PASSWORD;

public class ChangePasswordAction extends HttpFilter implements BaseUsersWsAction {

  private static final Logger LOG = LoggerFactory.getLogger(ChangePasswordAction.class);

  private static final String CHANGE_PASSWORD = "change_password";
  private static final String CHANGE_PASSWORD_URL = "/" + UsersWs.API_USERS + "/" + CHANGE_PASSWORD;
  private static final String MSG_PARAMETER_MISSING = "The '%s' parameter is missing";

  private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
  private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
  private static final Pattern DIGIT_PATTERN = Pattern.compile("\\d");
  private static final Pattern SPECIAL_CHARACTER_PATTERN = Pattern.compile("[^a-zA-Z0-9]");

  private static final int MIN_PASSWORD_LENGTH = 12;

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
      .setExampleValue("My_Passw0rd%")
      .setMinimumLength(12)
      .setDescription("The password needs to fulfill the following requirements: at least 12 characters " +
                      "and contain at least one uppercase character, one lowercase character, one digit and one special character.");

    action.createParam(PARAM_PREVIOUS_PASSWORD)
      .setDescription("Previous password. Required when changing one's own password.")
      .setRequired(false)
      .setExampleValue("My_Previous_Passw0rd%");
  }

  @Override
  public void doFilter(HttpRequest request, HttpResponse response, FilterChain chain) {
    userSession.checkLoggedIn();
    try (DbSession dbSession = dbClient.openSession(false)) {
      String login = getParamOrThrow(request, PARAM_LOGIN);
      String newPassword = getParamOrThrow(request, PARAM_PASSWORD);
      assertPasswordFormatIsValid(newPassword);
      UserDto user;

      if (login.equals(userSession.getLogin())) {
        user = getUserOrThrow(dbSession, login);
        String previousPassword = getParamOrThrow(request, PARAM_PREVIOUS_PASSWORD);
        checkPreviousPassword(dbSession, user, previousPassword);
        checkNewPasswordSameAsOld(newPassword, previousPassword);
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
      writeJsonResponse(badRequestException.getMessage(), response);
      LOG.debug(badRequestException.getMessage(), badRequestException);
    } catch (PasswordException passwordException) {
      LOG.debug(passwordException.getMessage(), passwordException);
      setResponseStatus(response, HTTP_BAD_REQUEST);
      String message = passwordException.getPasswordMessage().map(pm -> pm.key).orElseGet(passwordException::getMessage);
      writeJsonResponse(message, response);
    }
  }

  private static void assertPasswordFormatIsValid(String newPassword) throws PasswordException {
    try {
      checkArgument(newPassword.length() >= MIN_PASSWORD_LENGTH, "Password must be at least %s characters long", MIN_PASSWORD_LENGTH);
      checkArgument(UPPERCASE_PATTERN.matcher(newPassword).find(), "Password must contain at least one uppercase character");
      checkArgument(LOWERCASE_PATTERN.matcher(newPassword).find(), "Password must contain at least one lowercase character");
      checkArgument(DIGIT_PATTERN.matcher(newPassword).find(), "Password must contain at least one digit");
      checkArgument(SPECIAL_CHARACTER_PATTERN.matcher(newPassword).find(), "Password must contain at least one special character");
    } catch (IllegalArgumentException e) {
      throw new PasswordException(e.getMessage());
    }
  }

  private static String getParamOrThrow(HttpRequest request, String key) throws PasswordException {
    String value = request.getParameter(key);
    if (isNullOrEmpty(value)) {
      throw new PasswordException(format(MSG_PARAMETER_MISSING, key));
    }
    return value;
  }

  private void checkPreviousPassword(DbSession dbSession, UserDto user, String password) throws PasswordException {
    try {
      localAuthentication.authenticate(dbSession, user, password, AuthenticationEvent.Method.BASIC);
    } catch (AuthenticationException ex) {
      throw new PasswordException(OLD_PASSWORD_INCORRECT, "Incorrect password");
    }
  }

  private static void checkNewPasswordSameAsOld(String newPassword, String previousPassword) throws PasswordException {
    if (previousPassword.equals(newPassword)) {
      throw new PasswordException(NEW_PASSWORD_SAME_AS_OLD, "Password must be different from old password");
    }
  }

  private UserDto getUserOrThrow(DbSession dbSession, String login) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
    if (user == null || !user.isActive()) {
      throw new NotFoundException(format("User with login '%s' has not been found", login));
    }
    return user;
  }

  private void deleteTokensAndRefreshSession(HttpRequest request, HttpResponse response, DbSession dbSession, UserDto user) {
    dbClient.sessionTokensDao().deleteByUser(dbSession, user);
    refreshJwtToken(request, response, user);
  }

  private void refreshJwtToken(HttpRequest request, HttpResponse response, UserDto user) {
    jwtHttpHandler.removeToken(request, response);
    jwtHttpHandler.generateToken(user, request, response);
  }

  private void updatePassword(DbSession dbSession, UserDto user, String newPassword) {
    UpdateUser updateUser = new UpdateUser().setPassword(newPassword);
    userUpdater.updateAndCommit(dbSession, user, updateUser, u -> {
    });
  }

  private static void setResponseStatus(HttpResponse response, int newStatusCode) {
    response.setStatus(newStatusCode);
  }

  private static void writeJsonResponse(String msg, HttpResponse response) {
    Gson gson = new GsonBuilder()
      .disableHtmlEscaping()
      .create();

    try (OutputStream output = response.getOutputStream();
      JsonWriter writer = gson.newJsonWriter(new OutputStreamWriter(output, UTF_8))) {
      response.setContentType(JSON);
      writer.beginObject()
        .name("result").value(msg);
      writer.endObject();
    } catch (Exception e) {
      throw new IllegalStateException("Error while writing message", e);
    }
  }

  enum PasswordMessage {
    OLD_PASSWORD_INCORRECT("old_password_incorrect"),
    NEW_PASSWORD_SAME_AS_OLD("new_password_same_as_old");

    final String key;

    PasswordMessage(String key) {
      this.key = key;
    }
  }

  private static class PasswordException extends Exception {
    private final PasswordMessage passwordMessage;

    public PasswordException(PasswordMessage passwordMessage, String message) {
      super(message);
      this.passwordMessage = passwordMessage;
    }

    public PasswordException(String message) {
      super(message);
      this.passwordMessage = null;
    }

    public Optional<PasswordMessage> getPasswordMessage() {
      return Optional.ofNullable(passwordMessage);
    }
  }

}
