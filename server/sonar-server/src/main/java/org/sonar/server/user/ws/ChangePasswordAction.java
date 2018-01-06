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
package org.sonar.server.user.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserUpdater;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.db.user.UserDto.encryptPassword;

public class ChangePasswordAction implements UsersWsAction {

  private static final String PARAM_LOGIN = "login";
  private static final String PARAM_PASSWORD = "password";
  private static final String PARAM_PREVIOUS_PASSWORD = "previousPassword";

  private final DbClient dbClient;
  private final UserUpdater userUpdater;
  private final UserSession userSession;

  public ChangePasswordAction(DbClient dbClient, UserUpdater userUpdater, UserSession userSession) {
    this.dbClient = dbClient;
    this.userUpdater = userUpdater;
    this.userSession = userSession;
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
      if (login.equals(userSession.getLogin())) {
        String previousPassword = request.mandatoryParam(PARAM_PREVIOUS_PASSWORD);
        checkCurrentPassword(dbSession, login, previousPassword);
      } else {
        userSession.checkIsSystemAdministrator();
      }

      String password = request.mandatoryParam(PARAM_PASSWORD);
      UpdateUser updateUser = UpdateUser.create(login).setPassword(password);

      userUpdater.updateAndCommit(dbSession, updateUser, u -> {});
    }
    response.noContent();
  }

  private void checkCurrentPassword(DbSession dbSession, String login, String password) {
    UserDto user = dbClient.userDao().selectOrFailByLogin(dbSession, login);
    String cryptedPassword = encryptPassword(password, user.getSalt());
    checkArgument(cryptedPassword.equals(user.getCryptedPassword()), "Incorrect password");
  }
}
