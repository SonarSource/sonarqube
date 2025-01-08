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
package org.sonar.server.usertoken.ws;

import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.server.ws.Request;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.sonar.db.permission.GlobalPermission.SCAN;
import static org.sonar.server.exceptions.NotFoundException.checkFound;
import static org.sonar.server.user.AbstractUserSession.insufficientPrivilegesException;

public class UserTokenSupport {

  static final String CONTROLLER = "api/user_tokens";
  static final String ACTION_SEARCH = "search";
  static final String ACTION_REVOKE = "revoke";
  static final String ACTION_GENERATE = "generate";
  static final String PARAM_LOGIN = "login";
  static final String PARAM_NAME = "name";
  static final String PARAM_TYPE = "type";
  static final String PARAM_PROJECT_KEY = "projectKey";
  static final String PARAM_EXPIRATION_DATE = "expirationDate";

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

  boolean sameLoginAsConnectedUser(Request request) {
    return request.param(PARAM_LOGIN) == null || isLoggedInUser(userSession, request.param(PARAM_LOGIN));
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

  public void validateGlobalScanPermission() {
    if (userSession.hasPermission(SCAN)){
      return;
    }
    throw insufficientPrivilegesException();
  }

  public void validateProjectScanPermission(DbSession dbSession, String projectKeyFromRequest) {
    Optional<ProjectDto> projectDto = dbClient.projectDao().selectProjectByKey(dbSession, projectKeyFromRequest);
    if (projectDto.isEmpty()) {
      throw new NotFoundException(format("Project key '%s' not found", projectKeyFromRequest));
    }
    validateProjectScanPermission(projectDto.get());
  }

  private void validateProjectScanPermission(ProjectDto projectDto) {
    if (userSession.hasEntityPermission(UserRole.SCAN, projectDto) || userSession.hasPermission(SCAN)) {
      return;
    }
    throw insufficientPrivilegesException();
  }
}
