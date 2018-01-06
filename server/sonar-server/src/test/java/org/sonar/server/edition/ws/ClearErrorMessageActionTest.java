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
package org.sonar.server.edition.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.edition.MutableEditionManagementState;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class ClearErrorMessageActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MutableEditionManagementState editionManagementState = Mockito.mock(MutableEditionManagementState.class);
  private ClearErrorMessageAction underTest = new ClearErrorMessageAction(userSessionRule, editionManagementState);
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void verify_definition() {
    WebService.Action def = actionTester.getDef();

    assertThat(def.key()).isEqualTo("clear_error_message");
    assertThat(def.since()).isEqualTo("6.7");
    assertThat(def.isPost()).isTrue();
    assertThat(def.isInternal()).isFalse();
    assertThat(def.description()).isNotEmpty();
    assertThat(def.responseExample()).isNull();
    assertThat(def.params()).isEmpty();
  }

  @Test
  public void request_fails_if_user_not_logged_in() {
    userSessionRule.anonymous();
    TestRequest request = actionTester.newRequest();

    expectedException.expect(UnauthorizedException.class);
    expectedException.expectMessage("Authentication is required");

    request.execute();
  }

  @Test
  public void request_fails_if_user_is_not_system_administer() {
    userSessionRule.logIn();
    TestRequest request = actionTester.newRequest();

    expectedException.expect(ForbiddenException.class);
    expectedException.expectMessage("Insufficient privileges");

    request.execute();
  }

  @Test
  public void request_clears_errorMessage_from_editionManagement_state() {
    userSessionRule.logIn().setSystemAdministrator();

    actionTester.newRequest().execute();

    verify(editionManagementState).clearInstallErrorMessage();
  }
}
