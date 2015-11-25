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

import java.io.IOException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.System2;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.db.DbTester;
import org.sonar.db.user.UserDbTester;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.ServerException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usertoken.TokenGenerator;
import org.sonar.server.ws.WsActionTester;
import org.sonar.test.DbTests;
import org.sonarqube.ws.MediaTypes;
import org.sonarqube.ws.WsUserTokens.GenerateWsResponse;

import static com.google.common.base.Throwables.propagate;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.user.UserTesting.newUserDto;
import static org.sonar.db.user.UserTokenTesting.newUserToken;
import static org.sonar.test.JsonAssert.assertJson;
import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.PARAM_LOGIN;
import static org.sonarqube.ws.client.usertoken.UserTokensWsParameters.PARAM_NAME;

@Category(DbTests.class)
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
  public void return_json_example() {
    String response = ws.newRequest()
      .setMediaType(MediaTypes.JSON)
      .setParam(PARAM_LOGIN, GRACE_HOPPER)
      .setParam(PARAM_NAME, TOKEN_NAME)
      .execute().getInput();

    assertJson(response).isSimilarTo(getClass().getResource("generate-example.json"));
  }

  @Test
  public void fail_if_login_does_not_exist() {
    expectedException.expect(NotFoundException.class);
    expectedException.expectMessage("User with login 'unknown-login' not found");

    newRequest("unknown-login", "any-name");
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

  private GenerateWsResponse newRequest(String login, String name) {
    InputStream responseStream = ws.newRequest()
      .setMediaType(MediaTypes.PROTOBUF)
      .setParam(PARAM_LOGIN, login)
      .setParam(PARAM_NAME, name)
      .execute().getInputStream();

    try {
      return GenerateWsResponse.parseFrom(responseStream);
    } catch (IOException e) {
      throw propagate(e);
    }
  }
}
