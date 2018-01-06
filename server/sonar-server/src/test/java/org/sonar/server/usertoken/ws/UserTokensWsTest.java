/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.server.user.UserSession;
import org.sonar.server.usertoken.TokenGenerator;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class UserTokensWsTest {
  static final String CONTROLLER_KEY = "api/user_tokens";

  WsTester ws;

  @Before
  public void setUp() {
    UserSession userSession = mock(UserSession.class);
    DbClient dbClient = mock(DbClient.class);
    System2 system = mock(System2.class);
    TokenGenerator tokenGenerator = mock(TokenGenerator.class);

    ws = new WsTester(new UserTokensWs(
      new GenerateAction(dbClient, userSession, system, tokenGenerator),
      new RevokeAction(dbClient, userSession),
      new SearchAction(dbClient, userSession)));
  }

  @Test
  public void generate_action() {
    WebService.Action action = ws.action(CONTROLLER_KEY, "generate");

    assertThat(action).isNotNull();
    assertThat(action.since()).isEqualTo("5.3");
    assertThat(action.responseExampleAsString()).isNotEmpty();
    assertThat(action.isPost()).isTrue();
    assertThat(action.param("login").isRequired()).isFalse();
    assertThat(action.param("name").isRequired()).isTrue();
  }

  @Test
  public void revoke_action() {
    WebService.Action action = ws.action(CONTROLLER_KEY, "revoke");

    assertThat(action).isNotNull();
    assertThat(action.since()).isEqualTo("5.3");
    assertThat(action.isPost()).isTrue();
    assertThat(action.param("login").isRequired()).isFalse();
    assertThat(action.param("name").isRequired()).isTrue();
  }

  @Test
  public void search_action() {
    WebService.Action action = ws.action(CONTROLLER_KEY, "search");

    assertThat(action).isNotNull();
    assertThat(action.since()).isEqualTo("5.3");
    assertThat(action.isPost()).isFalse();
    assertThat(action.param("login").isRequired()).isFalse();
  }
}
