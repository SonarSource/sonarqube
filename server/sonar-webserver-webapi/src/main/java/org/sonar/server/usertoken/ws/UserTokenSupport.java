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

import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;
import static org.sonar.server.exceptions.NotFoundException.checkFound;

public class UserTokenSupport {

  static final String CONTROLLER = "api/user_tokens";
  static final String ACTION_SEARCH = "search";
  static final String ACTION_REVOKE = "revoke";
  static final String ACTION_GENERATE = "generate";
  static final String PARAM_LOGIN = "login";
  static final String PARAM_NAME = "name";

  private final DbClient dbClient;
  private final UserSession userSession;

  public UserTokenSupport(DbClient dbClient, UserSession userSession) {
    this.dbClient = dbClient;
    this.userSession = userSession;
  }

  UserDto getUser(DbSession dbSession, Request request) {
    String login = request.param(PARAM_LOGIN);
    login = login == null ? userSession.getLogin() : login;
    validate(userSession, login);
    UserDto user = dbClient.userDao().selectByLogin(dbSession, requireNonNull(login, "Login should not be null"));
    checkFound(user, "User with login '%s' doesn't exist", login);
    return user;
  }

  private static void validate(UserSession userSession, @Nullable String requestLogin) {
    userSession.checkLoggedIn();
    if (userSession.isSystemAdministrator() || isLoggedInUser(userSession, requestLogin)) {
      return;
    }
    throw insufficientPrivilegesException();
  }

  private static boolean isLoggedInUser(UserSession userSession, @Nullable String requestLogin) {
    return requestLogin != null && requestLogin.equals(userSession.getLogin());
  }
}
