/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.platform.ws;

import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

public class HealthActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AbstractHealthActionTestSupport healthActionTestSupport = new AbstractHealthActionTestSupport();
  private WsActionTester underTest = new WsActionTester(new HealthAction(userSessionRule, healthActionTestSupport.mockedHealthChecker));

  @Test
  public void verify_definition() {
    WebService.Action definition = underTest.getDef();

    healthActionTestSupport.verifyDefinition(definition);
  }

  @Test
  public void execute_fails_with_UnauthorizedException_if_user_is_not_logged_in() {
    TestRequest request = underTest.newRequest();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    request.execute();
  }

  @Test
  public void execute_fails_with_ForbiddenException_if_user_logged_in_but_not_root() {
    TestRequest request = underTest.newRequest();
    userSessionRule.logIn();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    request.execute();
  }

  @Test
  public void verify_example() {
    userSessionRule.logIn();
    rootOrSystemAdmin();

    healthActionTestSupport.verifyExample(underTest);
  }

  @Test
  public void request_returns_status_and_causes_from_HealthChecker_check_method() {
    userSessionRule.logIn();
    rootOrSystemAdmin();

    healthActionTestSupport.requestReturnsStatusAndCausesFromHealthCheckerCheckMethod(underTest);
  }

  private void rootOrSystemAdmin() {
    if (new Random().nextBoolean()) {
      userSessionRule.setRoot();
    } else {
      userSessionRule.setSystemAdministrator();
    }
  }
}
