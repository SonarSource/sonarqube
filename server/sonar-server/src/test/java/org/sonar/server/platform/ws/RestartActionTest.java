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
package org.sonar.server.platform.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.app.ProcessCommandWrapper;
import org.sonar.server.app.RestartFlagHolder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.WebServer;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestartActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public LogTester logTester = new LogTester();

  private ProcessCommandWrapper processCommandWrapper = mock(ProcessCommandWrapper.class);
  private RestartFlagHolder restartFlagHolder = mock(RestartFlagHolder.class);
  private WebServer webServer = mock(WebServer.class);
  private RestartAction sut = new RestartAction(userSessionRule, processCommandWrapper, restartFlagHolder, webServer);
  private InOrder inOrder = Mockito.inOrder(restartFlagHolder, processCommandWrapper);

  private WsActionTester actionTester = new WsActionTester(sut);

  @Test
  public void request_fails_in_production_mode_with_ForbiddenException_when_user_is_not_logged_in() {
    when(webServer.isStandalone()).thenReturn(true);
    expectedException.expect(ForbiddenException.class);

    actionTester.newRequest().execute();
  }

  @Test
  public void request_fails_in_production_mode_with_ForbiddenException_when_user_is_not_system_administrator() {
    when(webServer.isStandalone()).thenReturn(true);
    userSessionRule.logIn().setNonSystemAdministrator();

    expectedException.expect(ForbiddenException.class);

    actionTester.newRequest().execute();
  }

  @Test
  public void request_fails_in_cluster_mode_with_IllegalArgumentException() {
    when(webServer.isStandalone()).thenReturn(false);

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Restart not allowed for cluster nodes");

    actionTester.newRequest().execute();
  }

  @Test
  public void calls_ProcessCommandWrapper_requestForSQRestart_in_production_mode() {
    when(webServer.isStandalone()).thenReturn(true);
    userSessionRule.logIn().setSystemAdministrator();

    actionTester.newRequest().execute();

    inOrder.verify(restartFlagHolder).set();
    inOrder.verify(processCommandWrapper).requestSQRestart();
  }

  @Test
  public void logs_login_of_authenticated_user_requesting_the_restart_in_production_mode() {
    when(webServer.isStandalone()).thenReturn(true);
    String login = "BigBother";
    userSessionRule.logIn(login).setSystemAdministrator();

    actionTester.newRequest().execute();

    assertThat(logTester.logs(LoggerLevel.INFO))
      .contains("SonarQube restart requested by " + login);
  }

}
