/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ws.Change;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.common.user.service.UserService;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.user.UserSession;

import static java.util.Collections.singletonList;
import static org.sonar.server.exceptions.BadRequestException.checkRequest;
import static org.sonar.server.exceptions.NotFoundException.checkFound;

public class DeactivateAction implements UsersWsAction {

  private static final Logger logger = LoggerFactory.getLogger(DeactivateAction.class);

  private static final String PARAM_LOGIN = "login";
  private static final String PARAM_ANONYMIZE = "anonymize";

  private final DbClient dbClient;
  private final UserSession userSession;
  private final UserJsonWriter userWriter;
  private final UserService userService;

  public DeactivateAction(DbClient dbClient, UserSession userSession, UserJsonWriter userWriter, UserService userService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.userWriter = userWriter;
    this.userService = userService;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("deactivate")
      .setDescription("Deactivate a user. Requires Administer System permission")
      .setSince("3.7")
      .setPost(true)
      .setResponseExample(getClass().getResource("deactivate-example.json"))
      .setHandler(this)
      .setDeprecatedSince("10.4")
      .setChangelog(new Change("10.4", "Deprecated. Use DELETE api/v2/users-management/users/{id} instead"));

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("myuser");

    action.createParam(PARAM_ANONYMIZE)
      .setDescription("Anonymize user in addition to deactivating it")
      .setBooleanPossibleValues()
      .setRequired(false)
      .setSince("9.7")
      .setDefaultValue(false);
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    String login = request.mandatoryParam(PARAM_LOGIN);
    checkRequest(!login.equals(userSession.getLogin()), "Self-deactivation is not possible");
    boolean shouldAnonymize = request.mandatoryParamAsBoolean(PARAM_ANONYMIZE);
    String remoteAddress = request.header("X-Forwarded-For").orElse("n/a");
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto userDto = dbClient.userDao().selectByLogin(dbSession, login);
      checkFound(userDto, "User '%s' doesn't exist", login);
      UserDto deactivatedUser = userService.deactivate(userDto.getUuid(), shouldAnonymize);
      writeResponse(response, deactivatedUser);
      logger.info("User \"{}\" was deactivated. The action was executed by {} from {} remote address.", login, userSession.getLogin(), remoteAddress);
    } catch (BadRequestException ex) {
      logger.info("User \"{}\" was not deactivated. The action was executed by {} from {} remote address. Error: {}",
          login, userSession.getLogin(), remoteAddress, ex.getMessage());
      throw ex;
    }
  }

  private void writeResponse(Response response, UserDto userDto) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      try (JsonWriter json = response.newJsonWriter()) {
        json.beginObject();
        json.name("user");
        Set<String> groups = new HashSet<>(dbClient.groupMembershipDao().selectGroupsByLogins(dbSession, singletonList(userDto.getLogin())).get(userDto.getLogin()));
        userWriter.write(json, userDto, groups, UserJsonWriter.FIELDS);
        json.endObject();
      }
    }
  }

}
