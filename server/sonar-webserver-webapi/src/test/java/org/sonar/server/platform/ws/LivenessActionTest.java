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
package org.sonar.server.platform.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LivenessActionTest {

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  private final SystemPasscode systemPasscode = mock(SystemPasscode.class);
  private final LivenessChecker livenessChecker = mock(LivenessChecker.class);
  private final LivenessActionSupport livenessActionSupport = new LivenessActionSupport(livenessChecker);
  private final WsActionTester underTest = new WsActionTester(new LivenessAction(livenessActionSupport, systemPasscode, userSessionRule));

  @Test
  public void verify_definition() {
    WebService.Action definition = underTest.getDef();

    assertThat(definition.key()).isEqualTo("liveness");
    assertThat(definition.isPost()).isFalse();
    assertThat(definition.description()).isNotEmpty();
    assertThat(definition.since()).isEqualTo("9.1");
    assertThat(definition.isInternal()).isTrue();
    assertThat(definition.responseExample()).isNull();
    assertThat(definition.params()).isEmpty();
  }

  @Test
  public void fail_when_system_passcode_is_invalid() {
    when(systemPasscode.isValid(any())).thenReturn(false);

    TestRequest request = underTest.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void fail_when_user_is_not_system_admin() {
    when(systemPasscode.isValid(any())).thenReturn(false);
    userSessionRule.logIn();

    TestRequest request = underTest.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void liveness_check_failed_expect_500() {
    when(systemPasscode.isValid(any())).thenReturn(true);
    when(livenessChecker.liveness()).thenReturn(false);

    TestRequest request = underTest.newRequest();
    assertThatThrownBy(request::execute)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Liveness check failed");
  }

  @Test
  public void liveness_check_success_expect_204() {
    when(systemPasscode.isValid(any())).thenReturn(true);
    when(livenessChecker.liveness()).thenReturn(true);

    assertThat(underTest.newRequest().execute().getStatus()).isEqualTo(204);
  }

}
