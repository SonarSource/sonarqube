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
import org.sonar.api.server.ws.WebService;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserTokenDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.UserTokens.SearchWsResponse;
import org.sonarqube.ws.UserTokens.SearchWsResponse.UserToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.api.utils.DateUtils.formatDateTime;
import static org.sonar.server.usertoken.ws.UserTokenSupport.PARAM_LOGIN;
import static org.sonar.test.JsonAssert.assertJson;

public class SearchActionTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private WsActionTester ws = new WsActionTester(new SearchAction(dbClient, new UserTokenSupport(db.getDbClient(), userSession)));

  @Test
  public void search_action() {
    WebService.Action action = ws.getDef();

    assertThat(action).isNotNull();
    assertThat(action.key()).isEqualTo("search");
    assertThat(action.since()).isEqualTo("5.3");
    assertThat(action.isPost()).isFalse();
    assertThat(action.param("login").isRequired()).isFalse();
  }

  @Test
  public void search_json_example() {
    UserDto user1 = db.users().insertUser(u -> u.setLogin("grace.hopper"));
    UserDto user2 = db.users().insertUser(u -> u.setLogin("ada.lovelace"));
    db.users().insertToken(user1, t -> t.setName("Project scan on Travis").setCreatedAt(1448523067221L));
    db.users().insertToken(user1, t -> t.setName("Project scan on AppVeyor").setCreatedAt(1438523067221L));
    db.users().insertToken(user1, t -> t.setName("Project scan on Jenkins").setCreatedAt(1428523067221L));
    db.users().insertToken(user2, t -> t.setName("Project scan on Travis").setCreatedAt(141456787123L));
    logInAsSystemAdministrator();

    String response = ws.newRequest()
      .setParam(PARAM_LOGIN, user1.getLogin())
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("search-example.json"));
  }

  @Test
  public void a_user_can_search_its_own_token() {
    UserDto user = db.users().insertUser();
    db.users().insertToken(user, t -> t.setName("Project scan on Travis").setCreatedAt(1448523067221L));
    userSession.logIn(user);

    SearchWsResponse response = newRequest(null);

    assertThat(response.getUserTokensCount()).isEqualTo(1);
  }

  @Test
  public void return_last_connection_date() {
    UserDto user = db.users().insertUser();
    UserTokenDto token1 = db.users().insertToken(user);
    UserTokenDto token2 = db.users().insertToken(user);
    db.getDbClient().userTokenDao().update(db.getSession(), token1.setLastConnectionDate(10_000_000_000L));
    db.commit();
    logInAsSystemAdministrator();

    SearchWsResponse response = newRequest(user.getLogin());

    assertThat(response.getUserTokensList())
      .extracting(UserToken::getName, UserToken::hasLastConnectionDate, UserToken::getLastConnectionDate)
      .containsExactlyInAnyOrder(
        tuple(token1.getName(), true, formatDateTime(10_000_000_000L)),
        tuple(token2.getName(), false, ""));
  }

  @Test
  public void fail_when_login_does_not_exist() {
    logInAsSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User with login 'unknown-login' doesn't exist");

    newRequest("unknown-login");
  }

  @Test
  public void throw_ForbiddenException_if_a_non_root_administrator_searches_for_tokens_of_someone_else() {
    UserDto user = db.users().insertUser();
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest(user.getLogin());
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    UserDto user = db.users().insertUser();
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    newRequest(user.getLogin());
  }

  private SearchWsResponse newRequest(@Nullable String login) {
    TestRequest testRequest = ws.newRequest();
    if (login != null) {
      testRequest.setParam(PARAM_LOGIN, login);
    }

    return testRequest.executeProtobuf(SearchWsResponse.class);
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }
}
