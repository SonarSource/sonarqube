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
package org.sonar.server.user.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.authentication.CredentialsLocalAuthentication;
import org.sonar.server.authentication.event.AuthenticationEvent;
import org.sonar.server.authentication.event.AuthenticationException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserUpdater;

import static java.lang.String.format;

public class ChangePasswordAction implements UsersWsAction {

  private static final String PARAM_LOGIN = "login";
  private static final String PARAM_PASSWORD = "password";
  private static final String PARAM_PREVIOUS_PASSWORD = "previousPassword";

  private final DbClient dbClient;
  private final UserUpdater userUpdater;
  private final UserSession userSession;
  private final CredentialsLocalAuthentication localAuthentication;

  public ChangePasswordAction(DbClient dbClient, UserUpdater userUpdater, UserSession userSession, CredentialsLocalAuthentication localAuthentication) {
    this.dbClient = dbClient;
    this.userUpdater = userUpdater;
    this.userSession = userSession;
    this.localAuthentication = localAuthentication;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("change_password")
      .setDescription("Update a user's password. Authenticated users can change their own password, " +
        "provided that the account is not linked to an external authentication system. " +
        "Administer System permission is required to change another user's password.")
      .setSince("5.2")
      .setPost(true)
      .setHandler(this);

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
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn();

    try (DbSession dbSession = dbClient.openSession(false)) {
      String login = request.mandatoryParam(PARAM_LOGIN);
      UserDto user = getUser(dbSession, login);
      if (login.equals(userSession.getLogin())) {
        String previousPassword = request.mandatoryParam(PARAM_PREVIOUS_PASSWORD);
        checkCurrentPassword(dbSession, user, previousPassword);
      } else {
        userSession.checkIsSystemAdministrator();
      }

      String password = request.mandatoryParam(PARAM_PASSWORD);
      UpdateUser updateUser = new UpdateUser().setPassword(password);
      userUpdater.updateAndCommit(dbSession, user, updateUser, u -> {
      });
    }
    response.noContent();
  }

  private UserDto getUser(DbSession dbSession, String login) {
    UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
    if (user == null || !user.isActive()) {
      throw new NotFoundException(format("User with login '%s' has not been found", login));
    }
    return user;
  }

  private void checkCurrentPassword(DbSession dbSession, UserDto user, String password) {
    try {
      localAuthentication.authenticate(dbSession, user, password, AuthenticationEvent.Method.BASIC);
    } catch (AuthenticationException ex) {
      throw new IllegalArgumentException("Incorrect password");
    }
  }
}
