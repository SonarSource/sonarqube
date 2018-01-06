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

import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.UserTokenTesting.newUserToken;
import static org.sonar.server.usertoken.ws.UserTokensWsParameters.PARAM_LOGIN;
import static org.sonar.server.usertoken.ws.UserTokensWsParameters.PARAM_NAME;


public class RevokeActionTest {
  private static final String GRACE_HOPPER = "grace.hopper";
  private static final String ADA_LOVELACE = "ada.lovelace";
  private static final String TOKEN_NAME = "token-name";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private WsActionTester ws;

  @Before
  public void setUp() {
    ws = new WsActionTester(
      new RevokeAction(dbClient, userSession));
  }

  @Test
  public void delete_token_in_db() {
    logInAsSystemAdministrator();
    insertUserToken(newUserToken().setLogin(GRACE_HOPPER).setName("token-to-delete"));
    insertUserToken(newUserToken().setLogin(GRACE_HOPPER).setName("token-to-keep-1"));
    insertUserToken(newUserToken().setLogin(GRACE_HOPPER).setName("token-to-keep-2"));
    insertUserToken(newUserToken().setLogin(ADA_LOVELACE).setName("token-to-delete"));

    String response = newRequest(GRACE_HOPPER, "token-to-delete");

    assertThat(response).isEmpty();
    assertThat(dbClient.userTokenDao().selectByLogin(dbSession, GRACE_HOPPER)).extracting("name").containsOnly("token-to-keep-1", "token-to-keep-2");
    assertThat(dbClient.userTokenDao().selectByLogin(dbSession, ADA_LOVELACE)).extracting("name").containsOnly("token-to-delete");
  }

  @Test
  public void user_can_delete_its_own_tokens() {
    userSession.logIn(GRACE_HOPPER);
    insertUserToken(newUserToken().setLogin(GRACE_HOPPER).setName("token-to-delete"));

    String response = newRequest(null, "token-to-delete");

    assertThat(response).isEmpty();
    assertThat(dbClient.userTokenDao().selectByLogin(dbSession, GRACE_HOPPER)).isEmpty();
  }

  @Test
  public void does_not_fail_when_incorrect_login_or_name() {
    logInAsSystemAdministrator();
    insertUserToken(newUserToken().setLogin(GRACE_HOPPER).setName(TOKEN_NAME));

    newRequest(ADA_LOVELACE, "another-token-name");
  }

  @Test
  public void throw_ForbiddenException_if_non_administrator_revokes_token_of_someone_else() {
    userSession.logIn();
    insertUserToken(newUserToken().setLogin(GRACE_HOPPER).setName(TOKEN_NAME));

    expectedException.expect(ForbiddenException.class);

    newRequest(GRACE_HOPPER, TOKEN_NAME);
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();
    insertUserToken(newUserToken().setLogin(GRACE_HOPPER).setName(TOKEN_NAME));

    expectedException.expect(UnauthorizedException.class);

    newRequest(GRACE_HOPPER, TOKEN_NAME);
  }

  private String newRequest(@Nullable String login, String name) {
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_NAME, name);
    if (login != null) {
      testRequest.setParam(PARAM_LOGIN, login);
    }

    return testRequest.execute().getInput();
  }

  private void insertUserToken(UserTokenDto userToken) {
    dbClient.userTokenDao().insert(dbSession, userToken);
    dbSession.commit();
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }
}
