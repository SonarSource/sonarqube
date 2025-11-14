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
package org.sonar.server.user.ws;

import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.management.ManagedInstanceChecker;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserUpdater;

import static java.lang.String.format;
import static org.sonar.server.user.UserUpdater.LOGIN_MAX_LENGTH;
import static org.sonar.server.user.UserUpdater.LOGIN_MIN_LENGTH;

public class UpdateLoginAction implements UsersWsAction {

  public static final String PARAM_LOGIN = "login";
  public static final String PARAM_NEW_LOGIN = "newLogin";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final UserUpdater userUpdater;
  private final ManagedInstanceChecker managedInstanceChecker;

  public UpdateLoginAction(DbClient dbClient, UserSession userSession, UserUpdater userUpdater,
    ManagedInstanceChecker managedInstanceChecker) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.userUpdater = userUpdater;
    this.managedInstanceChecker = managedInstanceChecker;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("update_login")
      .setDescription("Update a user login. A login can be updated many times.<br/>" +
        "Requires Administer System permission")
      .setSince("7.6")
      .setPost(true)
      .setHandler(this)
      .setDeprecatedSince("10.4")
      .setChangelog(new Change("10.4", "Deprecated. Use PATCH api/v2/users-management/users/{id} instead"));

    action.createParam(PARAM_LOGIN)
      .setRequired(true)
      .setDescription("The current login (case-sensitive)")
      .setExampleValue("mylogin");

    action.createParam(PARAM_NEW_LOGIN)
      .setRequired(true)
      .setMaximumLength(LOGIN_MAX_LENGTH)
      .setMinimumLength(LOGIN_MIN_LENGTH)
      .setDescription("The new login. It must not already exist.")
      .setExampleValue("mynewlogin");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    managedInstanceChecker.throwIfInstanceIsManaged();
    String login = request.mandatoryParam(PARAM_LOGIN);
    String newLogin = request.mandatoryParam(PARAM_NEW_LOGIN);
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = getUser(dbSession, login);
      userUpdater.updateAndCommit(dbSession, user, new UpdateUser().setLogin(newLogin), u -> {
      });
      response.noContent();
    }
  }

  private UserDto getUser(DbSession dbSession, String login) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
    if (user == null || !user.isActive()) {
      throw new NotFoundException(format("User '%s' doesn't exist", login));
    }
    return user;
  }

}
