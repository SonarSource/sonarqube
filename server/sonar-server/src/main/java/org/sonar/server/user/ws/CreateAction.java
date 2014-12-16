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
import org.sonar.server.user.NewUser;
import org.sonar.server.user.ReactivationException;
import org.sonar.server.user.UserService;

public class CreateAction implements RequestHandler {

  private static final String PARAM_LOGIN = "login";
  private static final String PARAM_PASSWORD = "password";
  private static final String PARAM_PASSWORD_CONFIRMATION = "password_confirmation";
  private static final String PARAM_NAME = "name";
  private static final String PARAM_EMAIL = "email";
  private static final String PARAM_PREVENT_REACTIVATION = "prevent_reactivation";

  private final UserService userService;

  public CreateAction(UserService userService) {
    this.userService = userService;
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

    action.createParam(PARAM_PREVENT_REACTIVATION)
      .setDescription("If set to true and if the user has been removed, a status 409 will be returned")
      .setDefaultValue(false)
      .setBooleanPossibleValues();
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    NewUser newUser = NewUser.create()
      .setLogin(request.mandatoryParam(PARAM_LOGIN))
      .setName(request.mandatoryParam(PARAM_NAME))
      .setEmail(request.param(PARAM_EMAIL))
      .setPassword(request.mandatoryParam(PARAM_PASSWORD))
      .setPasswordConfirmation(request.mandatoryParam(PARAM_PASSWORD_CONFIRMATION))
      .setPreventReactivation(request.mandatoryParamAsBoolean(PARAM_PREVENT_REACTIVATION));

    try {
      userService.create(newUser);
    } catch (ReactivationException e) {
      // write409(response, e.ruleKey());
    }
  }

  // private void writeResponse(Response response, RuleKey ruleKey) {
  // Rule rule = service.getNonNullByKey(ruleKey);
  // JsonWriter json = response.newJsonWriter().beginObject().name("rule");
  // mapping.write(rule, json, null /* TODO replace by SearchOptions immutable constant */);
  // json.endObject().close();
  // }
  //
  // private void write409(Response response, RuleKey ruleKey) {
  // Rule rule = service.getNonNullByKey(ruleKey);
  //
  // Response.Stream stream = response.stream();
  // stream.setStatus(409);
  // stream.setMediaType(MimeTypes.JSON);
  // JsonWriter json = JsonWriter.of(new OutputStreamWriter(stream.output())).beginObject().name("rule");
  // mapping.write(rule, json, null /* TODO replace by SearchOptions immutable constant */);
  // json.endObject().close();
  // }
}
