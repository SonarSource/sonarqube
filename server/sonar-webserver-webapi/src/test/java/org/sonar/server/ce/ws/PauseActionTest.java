/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.ce.ws;

import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.server.ws.WebService;
import org.sonar.ce.queue.CeQueue;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.user.SystemPasscode;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PauseActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private SystemPasscode passcode = mock(SystemPasscode.class);
  private CeQueue ceQueue = mock(CeQueue.class);
  private PauseAction underTest = new PauseAction(userSession, passcode, ceQueue);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void test_definition() {
    WebService.Action def = ws.getDef();
    assertThat(def.key()).isEqualTo("pause");
    assertThat(def.isInternal()).isTrue();
    assertThat(def.isPost()).isTrue();
    assertThat(def.params()).isEmpty();
    assertThat(def.responseExampleAsString()).isNull();
  }

  @Test
  public void pause_workers() {
    userSession.logIn().setSystemAdministrator();

    ws.newRequest().execute();

    verify(ceQueue).pauseWorkers();
  }

  @Test
  public void throw_ForbiddenException_if_not_system_administrator() {
    userSession.logIn().setNonSystemAdministrator();

    assertThatThrownBy(() -> ws.newRequest().execute())
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void throw_ForbiddenException_if_invalid_passcode() {
    userSession.anonymous();
    when(passcode.isValid(any())).thenReturn(false);

    assertThatThrownBy(() -> ws.newRequest().execute())
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @Test
  public void authenticate_with_passcode() {
    userSession.anonymous();
    when(passcode.isValid(any())).thenReturn(true);

    ws.newRequest().execute();

    verify(ceQueue).pauseWorkers();
  }
}
