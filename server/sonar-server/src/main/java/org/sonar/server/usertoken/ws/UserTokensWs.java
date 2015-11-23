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

package org.sonar.server.usertoken.ws;

import org.sonar.api.server.ws.WebService;

import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.USER_TOKENS_ENDPOINT;

public class UserTokensWs implements WebService {
  private final UserTokensWsAction[] actions;

  public UserTokensWs(UserTokensWsAction... actions) {
    this.actions = actions;
  }

  @Override
  public void define(Context context) {
    NewController controller = context.createController(USER_TOKENS_ENDPOINT)
      .setDescription("User token management. To enhance security, tokens can be used to take the place " +
        "of user credentials in analysis configuration. A token can be revoked at any time.")
      .setSince("5.3");

    for (UserTokensWsAction action : actions) {
      action.define(controller);
    }

    controller.done();
  }
}
