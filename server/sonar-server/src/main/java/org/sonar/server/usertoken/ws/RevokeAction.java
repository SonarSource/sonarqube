/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.usertoken.ws;

import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;

import static org.sonar.server.usertoken.ws.UserTokenSupport.ACTION_REVOKE;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_LOGIN;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_NAME;

public class RevokeAction implements UserTokensWsAction {

  private final DbClient dbClient;
  private final UserTokenSupport userTokenSupport;

  public RevokeAction(DbClient dbClient, UserTokenSupport userTokenSupport) {
    this.dbClient = dbClient;
    this.userTokenSupport = userTokenSupport;
  }

  @Override
  public void define(WebService.NewController context) {
    WebService.NewAction action = context.createAction(ACTION_REVOKE)
      .setDescription("Revoke a user access token. <br/>" +
        "It requires administration permissions to specify a 'login' and revoke a token for another user. Otherwise, the token for the current user is revoked.")
      .setSince("5.3")
      .setPost(true)
      .setHandler(this);

    action.createParam(PARAM_LOGIN)
      .setDescription("User login")
      .setExampleValue("g.hopper");

    action.createParam(PARAM_NAME)
      .setRequired(true)
      .setDescription("Token name")
      .setExampleValue("Project scan on Travis");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    String name = request.mandatoryParam(PARAM_NAME);
    try (DbSession dbSession = dbClient.openSession(false)) {
      UserDto user = userTokenSupport.getUser(dbSession, request);
      dbClient.userTokenDao().deleteByUserAndName(dbSession, user, name);
      dbSession.commit();
    }
    response.noContent();
  }
}
