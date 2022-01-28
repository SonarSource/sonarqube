/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.HashSet;
import java.util.Set;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.property.PropertyQuery;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserIndexer;

import static java.util.Collections.singletonList;
import static org.sonar.api.CoreProperties.DEFAULT_ISSUE_ASSIGNEE;
import static org.sonar.db.permission.GlobalPermission.ADMINISTER;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.exceptions.NotFoundException.checkFound;

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
      .setResponseExample(getClass().getResource("deactivate-example.json"))
      .setHandler(this);

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("myuser");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String login;

    userSession.checkLoggedIn().checkIsSystemAdministrator();
    login = request.mandatoryParam(PARAM_LOGIN);
    checkRequest(!login.equals(userSession.getLogin()), "Self-deactivation is not possible");

    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
      checkFound(user, "User '%s' doesn't exist", login);

      ensureNotLastAdministrator(dbSession, user);

      String userUuid = user.getUuid();
      dbClient.userTokenDao().deleteByUser(dbSession, user);
      dbClient.propertiesDao().deleteByKeyAndValue(dbSession, DEFAULT_ISSUE_ASSIGNEE, user.getLogin());
      dbClient.propertiesDao().deleteByQuery(dbSession, PropertyQuery.builder().setUserUuid(userUuid).build());
      dbClient.userGroupDao().deleteByUserUuid(dbSession, user);
      dbClient.userPermissionDao().deleteByUserUuid(dbSession, user);
      dbClient.permissionTemplateDao().deleteUserPermissionsByUserUuid(dbSession, userUuid, user.getLogin());
      dbClient.qProfileEditUsersDao().deleteByUser(dbSession, user);
      dbClient.userPropertiesDao().deleteByUser(dbSession, user);
      dbClient.almPatDao().deleteByUser(dbSession, user);
      dbClient.sessionTokensDao().deleteByUser(dbSession, user);
      dbClient.userDismissedMessagesDao().deleteByUser(dbSession, user);
      dbClient.qualityGateUserPermissionDao().deleteByUser(dbSession, user);
      dbClient.userDao().deactivateUser(dbSession, user);
      userIndexer.commitAndIndex(dbSession, user);
    }

    writeResponse(response, login);
  }

  private void writeResponse(Response response, String login) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
      // safeguard. It exists as the check has already been done earlier
      // when deactivating user
      checkFound(user, "User '%s' doesn't exist", login);

      try (JsonWriter json = response.newJsonWriter()) {
        json.beginObject();
        json.name("user");
        Set<String> groups = new HashSet<>(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(login)).get(login));
        userWriter.write(json, user, groups, UserJsonWriter.FIELDS);
        json.endObject();
      }
    }
  }

  private void ensureNotLastAdministrator(DbSession dbSession, UserDto user) {
    boolean isLastAdmin = dbClient.authorizationDao().countUsersWithGlobalPermissionExcludingUser(dbSession, ADMINISTER.getKey(), user.getUuid()) == 0;
    checkRequest(!isLastAdmin, "User is last administrator, and cannot be deactivated");
  }

}
