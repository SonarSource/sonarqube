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

import java.util.function.Supplier;
import javax.inject.Inject;
import org.apache.commons.lang.RandomStringUtils;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.server.ws.WebService.NewAction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.user.ExternalIdentity;
import org.sonar.server.user.UserSession;
import org.sonar.server.user.index.UserIndexer;

import static org.sonar.server.exceptions.NotFoundException.checkFound;

public class AnonymizeAction implements UsersWsAction {
  private static final int LOGIN_RANDOM_LENGTH = 6;
  private static final String PARAM_LOGIN = "login";

  private final DbClient dbClient;
  private final UserIndexer userIndexer;
  private final UserSession userSession;
  private final Supplier<String> randomNameGenerator;

  @Inject
  public AnonymizeAction(DbClient dbClient, UserIndexer userIndexer, UserSession userSession) {
    this(dbClient, userIndexer, userSession, () -> "sq-removed-" + RandomStringUtils.randomAlphanumeric(LOGIN_RANDOM_LENGTH));
  }

  public AnonymizeAction(DbClient dbClient, UserIndexer userIndexer, UserSession userSession, Supplier<String> randomNameGenerator) {
    this.dbClient = dbClient;
    this.userIndexer = userIndexer;
    this.userSession = userSession;
    this.randomNameGenerator = randomNameGenerator;
  }

  @Override
  public void define(WebService.NewController controller) {
    NewAction action = controller.createAction("anonymize")
      .setDescription("Anonymize a deactivated user. Requires Administer System permission")
      .setSince("9.7")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setRequired(true)
      .setExampleValue("myuser");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    userSession.checkLoggedIn().checkIsSystemAdministrator();
    String login = request.mandatoryParam(PARAM_LOGIN);

    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = dbClient.userDao().selectByLogin(dbSession, login);
      checkFound(user, "User '%s' doesn't exist", login);
      if (user.isActive()) {
        throw new IllegalArgumentException(String.format("User '%s' is not deactivated", login));
      }

      String newLogin = generateAnonymousLogin(dbSession);
      user
        .setLogin(newLogin)
        .setName(newLogin)
        .setExternalIdentityProvider(ExternalIdentity.SQ_AUTHORITY)
        .setLocal(true)
        .setExternalId(newLogin)
        .setExternalLogin(newLogin);
      dbClient.userDao().update(dbSession, user);
      userIndexer.commitAndIndex(dbSession, user);
    }

    response.noContent();
  }

  private String generateAnonymousLogin(DbSession session) {
    for (int i = 0; i < 10; i++) {
      String candidate = randomNameGenerator.get();
      if (dbClient.userDao().selectByLogin(session, candidate) == null) {
        return candidate;
      }
    }
    throw new IllegalStateException("Could not find a unique login");
  }
}
