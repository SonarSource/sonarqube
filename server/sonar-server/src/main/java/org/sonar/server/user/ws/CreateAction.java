/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.user.ws;

import com.google.common.collect.ImmutableSet;
import org.sonar.api.i18n.I18n;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

public class CreateAction implements UsersWsAction {

  private static final String PARAM_LOGIN = "login";
  private static final String PARAM_PASSWORD = "password";
  private static final String PARAM_NAME = "name";
  private static final String PARAM_EMAIL = "email";
  private static final String PARAM_SCM_ACCOUNTS = "scmAccounts";
  private static final String PARAM_SCM_ACCOUNTS_DEPRECATED = "scm_accounts";

  private final UserIndex index;
  private final UserUpdater userUpdater;
  private final I18n i18n;
  private final UserSession userSession;
  private final UserJsonWriter userWriter;

  public CreateAction(UserIndex index, UserUpdater userUpdater, I18n i18n, UserSession userSession, UserJsonWriter userWriter) {
    this.index = index;
    this.userUpdater = userUpdater;
    this.i18n = i18n;
    this.userSession = userSession;
    this.userWriter = userWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("create")
      .setDescription("Create a user. If a deactivated user account exists with the given login, it will be reactivated. " +
        "Requires Administer System permission")
      .setSince("3.7")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("myuser");

    action.createParam(PARAM_PASSWORD)
      .setDescription("User password")
      .setRequired(true)
      .setExampleValue("mypassword");

    action.createParam(PARAM_NAME)
      .setDescription("User name")
      .setRequired(true)
      .setExampleValue("My Name");

    action.createParam(PARAM_EMAIL)
      .setDescription("User email")
      .setExampleValue("myname@email.com");

    action.createParam(PARAM_SCM_ACCOUNTS)
      .setDescription("SCM accounts. This parameter has been added in 5.1")
      .setDeprecatedKey(PARAM_SCM_ACCOUNTS_DEPRECATED)
      .setExampleValue("myscmaccount1,myscmaccount2");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    String login = request.mandatoryParam(PARAM_LOGIN);
    String password = request.mandatoryParam(PARAM_PASSWORD);
    NewUser newUser = NewUser.create()
      .setLogin(login)
      .setName(request.mandatoryParam(PARAM_NAME))
      .setEmail(request.param(PARAM_EMAIL))
      .setScmAccounts(request.paramAsStrings(PARAM_SCM_ACCOUNTS))
      .setPassword(password)
      .setPasswordConfirmation(password);

    boolean isUserReactivated = userUpdater.create(newUser);
    writeResponse(response, login, isUserReactivated);
  }

  private void writeResponse(Response response, String login, boolean isUserReactivated) {
    UserDoc user = index.getByLogin(login);
    JsonWriter json = response.newJsonWriter().beginObject();
    writeUser(json, user);
    if (isUserReactivated) {
      writeReactivationMessage(json, login);
    }
    json.endObject().close();
  }

  private void writeUser(JsonWriter json, UserDoc user) {
    json.name("user");
    userWriter.write(json, user, ImmutableSet.<String>of(), UserJsonWriter.FIELDS);
  }

  private void writeReactivationMessage(JsonWriter json, String login) {
    json.name("infos").beginArray();
    json.beginObject();
    String text = i18n.message(userSession.locale(), "user.reactivated", "user.reactivated", login);
    json.prop("msg", text);
    json.endObject();
    json.endArray();
  }
}
