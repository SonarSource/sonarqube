/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserUpdater;
import org.sonarqube.ws.client.user.UpdateRequest;

import static com.google.common.base.Strings.emptyToNull;
import static java.util.Collections.singletonList;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonarqube.ws.client.user.UsersWsParameters.ACTION_UPDATE;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_EMAIL;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_NAME;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNT;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNTS;
import static org.sonarqube.ws.client.user.UsersWsParameters.PARAM_SCM_ACCOUNTS_DEPRECATED;

public class UpdateAction implements UsersWsAction {

  private final UserUpdater userUpdater;
  private final UserSession userSession;
  private final UserJsonWriter userWriter;
  private final DbClient dbClient;

  public UpdateAction(UserUpdater userUpdater, UserSession userSession, UserJsonWriter userWriter, DbClient dbClient) {
    this.userUpdater = userUpdater;
    this.userSession = userSession;
    this.userWriter = userWriter;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction(ACTION_UPDATE)
      .setDescription("Update a user. If a deactivated user account exists with the given login, it will be reactivated. " +
        "Requires Administer System permission")
      .setSince("3.7")
      .setChangelog(new Change("5.2", "User's password can only be changed using the 'change_password' action."))
      .setPost(true)
      .setHandler(this)
      .setResponseExample(getClass().getResource("update-example.json"));

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("myuser");

    action.createParam(PARAM_NAME)
      .setDescription("User name")
      .setExampleValue("My Name");

    action.createParam(PARAM_EMAIL)
      .setDescription("User email")
      .setExampleValue("myname@email.com");

    action.createParam(PARAM_SCM_ACCOUNTS)
      .setDescription("This parameter is deprecated, please use '%s' instead", PARAM_SCM_ACCOUNT)
      .setDeprecatedKey(PARAM_SCM_ACCOUNTS_DEPRECATED, "6.0")
      .setDeprecatedSince("6.1")
      .setExampleValue("myscmaccount1,myscmaccount2");

    action.createParam(PARAM_SCM_ACCOUNT)
      .setDescription("SCM accounts. To set several values, the parameter must be called once for each value.")
      .setExampleValue("scmAccount=firstValue&scmAccount=secondValue&scmAccount=thirdValue");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    UpdateRequest updateRequest = toWsRequest(request);
    try (DbSession dbSession = dbClient.openSession(false)) {
      doHandle(dbSession, toWsRequest(request));
      writeUser(dbSession, response, updateRequest.getLogin());
    }
  }

  private void doHandle(DbSession dbSession, UpdateRequest request) {
    String login = request.getLogin();
    UpdateUser updateUser = UpdateUser.create(login);
    if (request.getName() != null) {
      updateUser.setName(request.getName());
    }
    if (request.getEmail() != null) {
      updateUser.setEmail(emptyToNull(request.getEmail()));
    }
    if (!request.getScmAccounts().isEmpty()) {
      updateUser.setScmAccounts(request.getScmAccounts());
    }
    userUpdater.updateAndCommit(dbSession, updateUser, u -> {});
  }

  private void writeUser(DbSession dbSession, Response response, String login) {
    JsonWriter json = response.newJsonWriter().beginObject();
    json.name("user");
    Set<String> groups = Sets.newHashSet();
    UserDto user = checkFound(dbClient.userDao().selectByLogin(dbSession, login), "User '%s' doesn't exist", login);
    groups.addAll(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(login)).get(login));
    userWriter.write(json, user, groups, UserJsonWriter.FIELDS);
    json.endObject().close();
  }

  private static UpdateRequest toWsRequest(Request request) {
    return UpdateRequest.builder()
      .setLogin(request.mandatoryParam(PARAM_LOGIN))
      .setName(request.param(PARAM_NAME))
      .setEmail(request.param(PARAM_EMAIL))
      .setScmAccounts(getScmAccounts(request))
      .build();
  }

  private static List<String> getScmAccounts(Request request) {
    if (request.hasParam(PARAM_SCM_ACCOUNT)) {
      return new ArrayList<>(request.multiParam(PARAM_SCM_ACCOUNT));
    }
    List<String> oldScmAccounts = request.paramAsStrings(PARAM_SCM_ACCOUNTS);
    return oldScmAccounts != null ? oldScmAccounts : new ArrayList<>();
  }
}
