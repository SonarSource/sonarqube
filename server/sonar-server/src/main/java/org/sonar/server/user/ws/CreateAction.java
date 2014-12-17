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

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.server.plugins.MimeTypes;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.ReactivationException;
import org.sonar.server.user.UserService;
import org.sonar.server.user.index.UserDoc;

import java.io.OutputStreamWriter;

public class CreateAction implements RequestHandler {

  private static final String PARAM_LOGIN = "login";
  private static final String PARAM_PASSWORD = "password";
  private static final String PARAM_PASSWORD_CONFIRMATION = "password_confirmation";
  private static final String PARAM_NAME = "name";
  private static final String PARAM_EMAIL = "email";
  private static final String PARAM_SCM_ACCOUNTS = "scm_accounts";
  private static final String PARAM_PREVENT_REACTIVATION = "prevent_reactivation";

  private final UserService service;

  public CreateAction(UserService service) {
    this.service = service;
  }

  void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("create")
      .setDescription("Create a user. Requires Administer System permission")
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

    action.createParam(PARAM_PASSWORD_CONFIRMATION)
      .setDescription("Must be the same value as \"password\"")
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
      .setDescription("SCM accounts")
      .setExampleValue("myscmaccount1, myscmaccount2");

    action.createParam(PARAM_PREVENT_REACTIVATION)
      .setDescription("If set to true and if the user has been removed, a status 409 will be returned")
      .setDefaultValue(false)
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String login = request.mandatoryParam(PARAM_LOGIN);
    NewUser newUser = NewUser.create()
      .setLogin(login)
      .setName(request.mandatoryParam(PARAM_NAME))
      .setEmail(request.param(PARAM_EMAIL))
      .setScmAccounts(request.paramAsStrings(PARAM_SCM_ACCOUNTS))
      .setPassword(request.mandatoryParam(PARAM_PASSWORD))
      .setPasswordConfirmation(request.mandatoryParam(PARAM_PASSWORD_CONFIRMATION))
      .setPreventReactivation(request.mandatoryParamAsBoolean(PARAM_PREVENT_REACTIVATION));

    try {
      service.create(newUser);
      writeResponse(response, login);
    } catch (ReactivationException e) {
      write409(response, login);
    }
  }

  private void writeResponse(Response response, String login) {
    UserDoc user = service.getByLogin(login);
    JsonWriter json = response.newJsonWriter().beginObject().name("user");
    writeUser(json, user);
    json.endObject().close();
  }

  private void write409(Response response, String login) {
    UserDoc user = service.getByLogin(login);

    Response.Stream stream = response.stream();
    stream.setStatus(409);
    stream.setMediaType(MimeTypes.JSON);
    JsonWriter json = JsonWriter.of(new OutputStreamWriter(stream.output())).beginObject().name("user");
    writeUser(json, user);
    json.endObject().close();
  }

  private void writeUser(JsonWriter json, UserDoc user) {
    json.beginObject()
      .prop("login", user.login())
      .prop("name", user.name())
      .prop("email", user.email())
      .prop("active", user.active())
      .name("scmAccounts").beginArray().values(user.scmAccounts()).endArray()
      .endObject();
  }
}
