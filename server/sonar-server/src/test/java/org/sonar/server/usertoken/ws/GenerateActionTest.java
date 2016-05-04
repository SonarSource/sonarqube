/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDbTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usertoken.TokenGenerator;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsUserTokens.GenerateWsResponse;

import static com.google.common.base.Throwables.propagate;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.db.user.UserTokenTesting.newUserToken;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.PARAM_NAME;

public class GenerateActionTest {
  private static final String GRACE_HOPPER = "grace.hopper";
  private static final String ADA_LOVELACE = "ada.lovelace";
  private static final String TOKEN_NAME = "Third Party Application";

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);
  UserDbTester userDb = new UserDbTester(db);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  TokenGenerator tokenGenerator = mock(TokenGenerator.class);
  WsActionTester ws;

  @Before
  public void setUp() {
    when(tokenGenerator.generate()).thenReturn("123456789");
    when(tokenGenerator.hash(anyString())).thenReturn("987654321");
    userSession
      .login()
      .setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);
    userDb.insertUser(newUserDto().setLogin(GRACE_HOPPER));
    userDb.insertUser(newUserDto().setLogin(ADA_LOVELACE));

    ws = new WsActionTester(
      new GenerateAction(db.getDbClient(), userSession, System2.INSTANCE, tokenGenerator));
  }

  @Test
  public void json_example() {
    String response = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam(PARAM_LOGIN, GRACE_HOPPER)
      .setParam(PARAM_NAME, TOKEN_NAME)
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("generate-example.json"));
  }

  @Test
  public void a_user_can_generate_token_for_himself() {
    userSession.login(GRACE_HOPPER).setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    GenerateWsResponse response = newRequest(null, TOKEN_NAME);

    assertThat(response.getLogin()).isEqualTo(GRACE_HOPPER);
  }

  @Test
  public void fail_if_name_is_longer_than_100_characters() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Token name length (101) is longer than the maximum authorized (100)");

    newRequest(GRACE_HOPPER, randomAlphabetic(101));
  }

  @Test
  public void fail_if_login_does_not_exist() {
    expectedException.expect(ForbiddenException.class);

    newRequest("unknown-login", "any-name");
  }

  @Test
  public void fail_if_name_is_blank() {
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("The 'name' parameter must not be blank");

    newRequest(GRACE_HOPPER, "   ");
  }

  @Test
  public void fail_if_token_with_same_login_and_name_exists() {
    newRequest(GRACE_HOPPER, TOKEN_NAME);
    expectedException.expect(BadRequestException.class);
    expectedException.expectMessage("A user token with login 'grace.hopper' and name 'Third Party Application' already exists");

    newRequest(GRACE_HOPPER, TOKEN_NAME);
  }

  @Test
  public void fail_if_token_hash_already_exists_in_db() {
    when(tokenGenerator.hash(anyString())).thenReturn("987654321");
    db.getDbClient().userTokenDao().insert(db.getSession(), newUserToken().setTokenHash("987654321"));
    db.commit();
    expectedException.expect(ServerException.class);
    expectedException.expectMessage("Error while generating token. Please try again.");

    newRequest(GRACE_HOPPER, TOKEN_NAME);
  }

  @Test
  public void fail_if_insufficient_privileges() {
    userSession.login(ADA_LOVELACE).setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);
    expectedException.expect(ForbiddenException.class);

    newRequest(GRACE_HOPPER, TOKEN_NAME);
  }

  private GenerateWsResponse newRequest(@Nullable String login, String name) {
    TestRequest testRequest = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_NAME, name);
    if (login != null) {
      testRequest.setParam(PARAM_LOGIN, login);
    }

    InputStream responseStream = testRequest
      .execute().getInputStream();

    try {
      return GenerateWsResponse.parseFrom(responseStream);
    } catch (IOException e) {
      throw propagate(e);
    }
  }
}
