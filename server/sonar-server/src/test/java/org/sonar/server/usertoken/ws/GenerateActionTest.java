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
import org.sonar.db.DbTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usertoken.TokenGenerator;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.UserTokens.GenerateWsResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.db.user.UserTokenTesting.newUserToken;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonar.server.usertoken.ws.UserTokensWsParameters.PARAM_LOGIN;
import static org.sonar.server.usertoken.ws.UserTokensWsParameters.PARAM_NAME;

public class GenerateActionTest {
  private static final String GRACE_HOPPER = "grace.hopper";
  private static final String ADA_LOVELACE = "ada.lovelace";
  private static final String TOKEN_NAME = "Third Party Application";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TokenGenerator tokenGenerator = mock(TokenGenerator.class);
  private WsActionTester ws;

  @Before
  public void setUp() {
    when(tokenGenerator.generate()).thenReturn("123456789");
    when(tokenGenerator.hash(anyString())).thenReturn("987654321");
    db.users().insertUser(newUserDto().setLogin(GRACE_HOPPER));
    db.users().insertUser(newUserDto().setLogin(ADA_LOVELACE));

    ws = new WsActionTester(
      new GenerateAction(db.getDbClient(), userSession, System2.INSTANCE, tokenGenerator));
  }

  @Test
  public void json_example() {
    logInAsSystemAdministrator();

    String response = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam(PARAM_LOGIN, GRACE_HOPPER)
      .setParam(PARAM_NAME, TOKEN_NAME)
      .execute().getInput();

    assertJson(response).ignoreFields("createdAt").isSimilarTo(getClass().getResource("generate-example.json"));
  }

  @Test
  public void a_user_can_generate_token_for_himself() {
    userSession.logIn(GRACE_HOPPER);

    GenerateWsResponse response = newRequest(null, TOKEN_NAME);

    assertThat(response.getLogin()).isEqualTo(GRACE_HOPPER);
    assertThat(response.getCreatedAt()).isNotEmpty();
  }

  @Test
  public void fail_if_login_does_not_exist() {
    logInAsSystemAdministrator();

    expectedException.expect(ForbiddenException.class);

    newRequest("unknown-login", "any-name");
  }

  @Test
  public void fail_if_name_is_blank() {
    logInAsSystemAdministrator();

    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The 'name' parameter must not be blank");

    newRequest(GRACE_HOPPER, "   ");
  }

  @Test
  public void fail_if_token_with_same_login_and_name_exists() {
    logInAsSystemAdministrator();

    newRequest(GRACE_HOPPER, TOKEN_NAME);
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("A user token with login 'grace.hopper' and name 'Third Party Application' already exists");

    newRequest(GRACE_HOPPER, TOKEN_NAME);
  }

  @Test
  public void fail_if_token_hash_already_exists_in_db() {
    logInAsSystemAdministrator();

    when(tokenGenerator.hash(anyString())).thenReturn("987654321");
    db.getDbClient().userTokenDao().insert(db.getSession(), newUserToken().setTokenHash("987654321"));
    db.commit();
    expectedException.expect(ServerException.class);
    expectedException.expectMessage("Error while generating token. Please try again.");

    newRequest(GRACE_HOPPER, TOKEN_NAME);
  }

  @Test
  public void throw_ForbiddenException_if_non_administrator_creates_token_for_someone_else() {
    userSession.logIn().setNonSystemAdministrator();

    expectedException.expect(ForbiddenException.class);

    newRequest(GRACE_HOPPER, TOKEN_NAME);
  }

  @Test
  public void throw_UnauthorizedException_if_not_logged_in() {
    userSession.anonymous();

    expectedException.expect(UnauthorizedException.class);

    newRequest(GRACE_HOPPER, TOKEN_NAME);
  }

  private GenerateWsResponse newRequest(@Nullable String login, String name) {
    TestRequest testRequest = ws.newRequest()
      .setParam(PARAM_NAME, name);
    if (login != null) {
      testRequest.setParam(PARAM_LOGIN, login);
    }

    return testRequest.executeProtobuf(GenerateWsResponse.class);
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }
}
