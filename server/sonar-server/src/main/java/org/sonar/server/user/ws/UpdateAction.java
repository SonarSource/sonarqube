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

import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Set;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbSession;
import org.sonar.db.MyBatis;
import org.sonar.db.DbClient;
import org.sonar.server.user.UpdateUser;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.UserUpdater;
import org.sonar.server.user.index.UserDoc;
import org.sonar.server.user.index.UserIndex;

public class UpdateAction implements UsersWsAction {

  private static final String PARAM_LOGIN = "login";
  private static final String PARAM_NAME = "name";
  private static final String PARAM_EMAIL = "email";
  private static final String PARAM_SCM_ACCOUNTS = "scmAccounts";
  private static final String PARAM_SCM_ACCOUNTS_DEPRECATED = "scm_accounts";

  private final UserIndex index;
  private final UserUpdater userUpdater;
  private final UserSession userSession;
  private final UserJsonWriter userWriter;
  private final DbClient dbClient;

  public UpdateAction(UserIndex index, UserUpdater userUpdater, UserSession userSession, UserJsonWriter userWriter, DbClient dbClient) {
    this.index = index;
    this.userUpdater = userUpdater;
    this.userSession = userSession;
    this.userWriter = userWriter;
    this.dbClient = dbClient;
  }

  @Override
  public void define(WebService.NewController controller) {
    WebService.NewAction action = controller.createAction("update")
      .setDescription("Update a user. If a deactivated user account exists with the given login, it will be reactivated. " +
        "Requires Administer System permission. Since 5.2, a user's password can only be changed using the 'change_password' action.")
      .setSince("3.7")
      .setPost(true)
      .setHandler(this)
      .setResponseExample(getClass().getResource("example-update.json"));

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
      .setDescription("SCM accounts. This parameter has been added in 5.1")
      .setDeprecatedKey(PARAM_SCM_ACCOUNTS_DEPRECATED)
      .setExampleValue("myscmaccount1,myscmaccount2");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkGlobalPermission(GlobalPermissions.SYSTEM_ADMIN);

    String login = request.mandatoryParam(PARAM_LOGIN);
    UpdateUser updateUser = UpdateUser.create(login);
    if (request.hasParam(PARAM_NAME)) {
      updateUser.setName(request.mandatoryParam(PARAM_NAME));
    }
    if (request.hasParam(PARAM_EMAIL)) {
      updateUser.setEmail(request.param(PARAM_EMAIL));
    }
    if (request.hasParam(PARAM_SCM_ACCOUNTS) || request.hasParam(PARAM_SCM_ACCOUNTS_DEPRECATED)) {
      updateUser.setScmAccounts(request.paramAsStrings(PARAM_SCM_ACCOUNTS));
    }

    userUpdater.update(updateUser);
    writeResponse(response, login);
  }

  private void writeResponse(Response response, String login) {
    UserDoc user = index.getByLogin(login);
    JsonWriter json = response.newJsonWriter().beginObject();
    writeUser(json, user);
    json.endObject().close();
  }

  private void writeUser(JsonWriter json, UserDoc user) {
    json.name("user");
    Set<String> groups = Sets.newHashSet();
    DbSession dbSession = dbClient.openSession(false);
    try {
      groups.addAll(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, Arrays.asList(user.login())).get(user.login()));
    } finally {
      MyBatis.closeQuietly(dbSession);
    }
    userWriter.write(json, user, groups, UserJsonWriter.FIELDS);
  }
}
