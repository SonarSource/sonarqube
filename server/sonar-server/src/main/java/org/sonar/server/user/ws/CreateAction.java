/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.NewUser;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserUpdater;
import org.sonarqube.ws.client.user.CreateRequest;

import static java.lang.String.format;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_EMAIL;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_PASSWORD;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNT;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNTS;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNTS_DEPRECATED;

public class CreateAction implements UsersWsAction {

  private final DbClient dbClient;
  private final UserUpdater userUpdater;
  private final UserSession userSession;
  private final UserJsonWriter userWriter;

  public CreateAction(DbClient dbClient, UserUpdater userUpdater, UserSession userSession, UserJsonWriter userWriter) {
    this.dbClient = dbClient;
    this.userUpdater = userUpdater;
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
      .setDescription("This parameter is deprecated, please use '%s' instead", PARAM_SCM_ACCOUNT)
      .setDeprecatedKey(PARAM_SCM_ACCOUNTS_DEPRECATED)
      .setDeprecatedSince("6.1")
      .setExampleValue("myscmaccount1,myscmaccount2");

    action.createParam(PARAM_SCM_ACCOUNT)
      .setDescription("SCM accounts. To set several values, the parameter must be called once for each value.")
      .setExampleValue("scmAccount=firstValue&scmAccount=secondValue&scmAccount=thirdValue");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkPermission(GlobalPermissions.SYSTEM_ADMIN);
    doHandle(toWsRequest(request), response);
  }

  private void doHandle(CreateRequest request, Response response) {
    NewUser newUser = NewUser.create()
      .setLogin(request.getLogin())
      .setName(request.getName())
      .setEmail(request.getEmail())
      .setScmAccounts(request.getScmAccounts())
      .setPassword(request.getPassword());
    boolean isUserReactivated = userUpdater.create(newUser);
    writeResponse(response, request.getLogin(), isUserReactivated);
  }

  private void writeResponse(Response response, String login, boolean isUserReactivated) {
    UserDto user = loadUser(login);
    JsonWriter json = response.newJsonWriter().beginObject();
    writeUser(json, user);
    if (isUserReactivated) {
      writeReactivationMessage(json, login);
    }
    json.endObject().close();
  }

  private void writeUser(JsonWriter json, UserDto user) {
    json.name("user");
    userWriter.write(json, user, ImmutableSet.of(), UserJsonWriter.FIELDS);
  }

  private static void writeReactivationMessage(JsonWriter json, String login) {
    json.name("infos").beginArray();
    json.beginObject();
    String text = format("The user '%s' has been reactivated", login);
    json.prop("msg", text);
    json.endObject();
    json.endArray();
  }

  private UserDto loadUser(String login) {
    DbSession dbSession = dbClient.openSession(false);
    try {
      return dbClient.userDao().selectOrFailByLogin(dbSession, login);
    } finally {
      dbClient.closeSession(dbSession);
    }
  }

  private static CreateRequest toWsRequest(Request request) {
    return CreateRequest.builder()
      .setLogin(request.mandatoryParam(PARAM_LOGIN))
      .setPassword(request.mandatoryParam(PARAM_PASSWORD))
      .setName(request.param(PARAM_NAME))
      .setEmail(request.param(PARAM_EMAIL))
      .setScmAccounts(getScmAccounts(request))
      .build();
  }

  private static List<String> getScmAccounts(Request request) {
    if (request.hasParam(PARAM_SCM_ACCOUNT)) {
      return request.multiParam(PARAM_SCM_ACCOUNT);
    }
    List<String> oldScmAccounts = request.paramAsStrings(PARAM_SCM_ACCOUNTS);
    return oldScmAccounts != null ? oldScmAccounts : Collections.emptyList();
  }
}
