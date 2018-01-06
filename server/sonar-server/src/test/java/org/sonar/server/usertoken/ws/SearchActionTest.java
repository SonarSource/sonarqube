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
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.UserTokens.SearchWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.db.user.UserTokenTesting.newUserToken;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonar.server.usertoken.ws.UserTokensWsParameters.PARAM_LOGIN;

public class SearchActionTest {
  private static final String GRACE_HOPPER = "grace.hopper";
  private static final String ADA_LOVELACE = "ada.lovelace";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private WsActionTester ws = new WsActionTester(new SearchAction(dbClient, userSession));

  @Before
  public void setUp() {
    db.users().insertUser(newUserDto().setLogin(GRACE_HOPPER));
    db.users().insertUser(newUserDto().setLogin(ADA_LOVELACE));
  }

  @Test
  public void search_json_example() {
    logInAsSystemAdministrator();

    dbClient.userTokenDao().insert(dbSession, newUserToken()
      .setCreatedAt(1448523067221L)
      .setName("Project scan on Travis")
      .setLogin(GRACE_HOPPER));
    dbClient.userTokenDao().insert(dbSession, newUserToken()
      .setCreatedAt(1438523067221L)
      .setName("Project scan on AppVeyor")
      .setLogin(GRACE_HOPPER));
    dbClient.userTokenDao().insert(dbSession, newUserToken()
      .setCreatedAt(1428523067221L)
      .setName("Project scan on Jenkins")
      .setLogin(GRACE_HOPPER));
    dbClient.userTokenDao().insert(dbSession, newUserToken()
      .setCreatedAt(141456787123L)
      .setName("Project scan on Travis")
      .setLogin(ADA_LOVELACE));
    dbSession.commit();

    String response = ws.newRequest()
      .setParam(PARAM_LOGIN, GRACE_HOPPER)
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("search-example.json"));
  }

  @Test
  public void a_user_can_search_its_own_token() {
    userSession.logIn(GRACE_HOPPER);
    dbClient.userTokenDao().insert(dbSession, newUserToken()
      .setCreatedAt(1448523067221L)
      .setName("Project scan on Travis")
      .setLogin(GRACE_HOPPER));
    db.commit();

    SearchWsResponse response = newRequest(null);

    assertThat(response.getUserTokensCount()).isEqualTo(1);
  }

  @Test
  public void fail_when_login_does_not_exist() {
    logInAsSystemAdministrator();

    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User with login 'unknown-login' not found");

    newRequest("unknown-login");
  }

  @Test
  public void throw_ForbiddenException_if_a_non_root_administrator_searches_for_tokens_of_someone_else() {
    userSession.logIn();

    expectedException.expect(ForbiddenException.class);

    newRequest(GRACE_HOPPER);
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    newRequest(GRACE_HOPPER);
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
