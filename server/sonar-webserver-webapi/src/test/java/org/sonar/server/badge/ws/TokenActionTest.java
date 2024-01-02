/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.badge.ws;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.web.UserRole;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.user.TokenType;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.usertoken.TokenGenerator;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.mockito.Mockito.when;

public class TokenActionTest {

  @Rule
  public DbTester db = DbTester.create();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final TokenGenerator tokenGenerator = Mockito.mock(TokenGenerator.class);

  private final WsActionTester ws = new WsActionTester(
    new TokenAction(
      db.getDbClient(),
      tokenGenerator, userSession));

  @Test
  public void missing_project_parameter_should_fail() {
    TestRequest request = ws.newRequest();
    Assertions.assertThatThrownBy(request::execute)
      .hasMessage("The 'project' parameter is missing")
      .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void missing_project_permission_should_fail() {
    ComponentDto project = db.components().insertPrivateProject();

    TestRequest request = ws.newRequest().setParam("project", project.getKey());

    Assertions.assertThatThrownBy(request::execute)
      .hasMessage("Insufficient privileges")
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void should_generate_token() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    when(tokenGenerator.generate(TokenType.PROJECT_BADGE_TOKEN)).thenReturn("generated_token");

    TestResponse response = ws.newRequest().setParam("project", project.getKey()).execute();

    response.assertJson("{\"token\":\"generated_token\"}");
  }

  @Test
  public void handle_whenApplicationKeyPassed_shouldReturnToken() {
    ComponentDto application = db.components().insertPrivateApplication();
    userSession.logIn().addProjectPermission(UserRole.USER, application);
    when(tokenGenerator.generate(TokenType.PROJECT_BADGE_TOKEN)).thenReturn("generated_token");

    TestResponse response = ws.newRequest().setParam("project", application.getKey()).execute();

    response.assertJson("{\"token\":\"generated_token\"}");
  }


  @Test
  public void should_reuse_generated_token() {
    ComponentDto project = db.components().insertPrivateProject();
    userSession.logIn().addProjectPermission(UserRole.USER, project);
    when(tokenGenerator.generate(TokenType.PROJECT_BADGE_TOKEN)).thenReturn("generated_token");

    // first call, generating the token
    TestResponse firstResponse = ws.newRequest().setParam("project", project.getKey()).execute();
    firstResponse.assertJson("{\"token\":\"generated_token\"}");

    // 2nd call, reusing the existing token
    when(tokenGenerator.generate(TokenType.USER_TOKEN)).thenReturn("never_generated_token");
    TestResponse secondResponse = ws.newRequest().setParam("project", project.getKey()).execute();

    secondResponse.assertJson("{\"token\":\"generated_token\"}");

  }

}
