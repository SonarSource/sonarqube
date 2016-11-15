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

import com.google.common.collect.Sets;
import java.util.Set;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserIndexer;

import static java.util.Collections.singletonList;
import static org.sonar.server.ws.WsUtils.checkFound;
import static org.sonar.server.ws.WsUtils.checkRequest;

public class DeactivateAction implements UsersWsAction {

  private static final String PARAM_LOGIN = "login";

  private final DbClient dbClient;
  private final UserIndexer userIndexer;
  private final UserSession userSession;
  private final UserJsonWriter userWriter;

  public DeactivateAction(DbClient dbClient, UserIndexer userIndexer, UserSession userSession, UserJsonWriter userWriter) {
    this.dbClient = dbClient;
    this.userIndexer = userIndexer;
    this.userSession = userSession;
    this.userWriter = userWriter;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("deactivate")
      .setDescription("Deactivate a user. Requires Administer System permission")
      .setSince("3.7")
      .setPost(true)
      .setHandler(this);

    action.createParam("login")
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("myuser");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkPermission(GlobalPermissions.SYSTEM_ADMIN);

    String login = request.mandatoryParam(PARAM_LOGIN);
    checkRequest(!login.equals(userSession.getLogin()), "Self-deactivation is not possible");

    UserDto user;
    try (DbSession dbSession = dbClient.openSession(false)) {
      user = dbClient.userDao().selectByLogin(dbSession, login);
      checkFound(user, "User '%s' doesn't exist", login);

      dbClient.userTokenDao().deleteByLogin(dbSession, login);
      dbClient.userDao().deactivateUserByLogin(dbSession, login);
      dbSession.commit();
    }

    userIndexer.index();
    writeResponse(response, login);
  }

  private void writeResponse(Response response, String login) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      JsonWriter json = response.newJsonWriter().beginObject();
      json.name("user");
      Set<String> groups = Sets.newHashSet();
      UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
      checkFound(user, "User '%s' doesn't exist", login);
      groups.addAll(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(login)).get(login));
      userWriter.write(json, user, groups, UserJsonWriter.FIELDS);
      json.endObject().close();
    }
  }

}
