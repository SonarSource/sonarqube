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

import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_LOGIN;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_NAME;

public class RevokeActionTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private WsActionTester ws = new WsActionTester(new RevokeAction(dbClient, new UserTokenSupport(db.getDbClient(), userSession)));

  @Test
  public void delete_token_in_db() {
    logInAsSystemAdministrator();
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    UserTokenDto tokenToDelete = db.users().insertToken(user1);
    UserTokenDto tokenToKeep1 = db.users().insertToken(user1);
    UserTokenDto tokenToKeep2 = db.users().insertToken(user1);
    UserTokenDto tokenFromAnotherUser = db.users().insertToken(user2);

    String response = newRequest(user1.getLogin(), tokenToDelete.getName());

    assertThat(response).isEmpty();
    assertThat(dbClient.userTokenDao().selectByUser(dbSession, user1))
      .extracting(UserTokenDto::getName)
      .containsExactlyInAnyOrder(tokenToKeep1.getName(), tokenToKeep2.getName());
    assertThat(dbClient.userTokenDao().selectByUser(dbSession, user2))
      .extracting(UserTokenDto::getName)
      .containsExactlyInAnyOrder(tokenFromAnotherUser.getName());
  }

  @Test
  public void user_can_delete_its_own_tokens() {
    UserDto user = db.users().insertUser();
    UserTokenDto token = db.users().insertToken(user);
    userSession.logIn(user);

    String response = newRequest(null, token.getName());

    assertThat(response).isEmpty();
    assertThat(dbClient.userTokenDao().selectByUser(dbSession, user)).isEmpty();
  }

  @Test
  public void does_not_fail_when_incorrect_login_or_name() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user);

    logInAsSystemAdministrator();

    newRequest(user.getLogin(), "another-token-name");
  }

  @Test
  public void throw_ForbiddenException_if_non_administrator_revokes_token_of_someone_else() {
    UserDto user = db.users().insertUser();
    UserTokenDto token = db.users().insertToken(user);
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest(user.getLogin(), token.getName());
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    UserDto user = db.users().insertUser();
    UserTokenDto token = db.users().insertToken(user);
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    newRequest(user.getLogin(), token.getName());
  }

  @Test
  public void fail_if_login_does_not_exist() {
    logInAsSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User with login 'unknown-login' doesn't exist");

    newRequest("unknown-login", "any-name");
  }

  private String newRequest(@Nullable String login, String name) {
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_NAME, name);
    if (login != null) {
      testRequest.setParam(PARAM_LOGIN, login);
    }

    return testRequest.execute().getInput();
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }
}
