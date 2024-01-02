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
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.server.app.ProcessCommandWrapper;
import org.sonar.server.app.RestartFlagHolder;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.NodeInformation;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestartActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();
  @Rule
  public LogTester logTester = new LogTester();

  private ProcessCommandWrapper processCommandWrapper = mock(ProcessCommandWrapper.class);
  private RestartFlagHolder restartFlagHolder = mock(RestartFlagHolder.class);
  private NodeInformation nodeInformation = mock(NodeInformation.class);
  private RestartAction sut = new RestartAction(userSessionRule, processCommandWrapper, restartFlagHolder, nodeInformation);
  private InOrder inOrder = Mockito.inOrder(restartFlagHolder, processCommandWrapper);

  private WsActionTester actionTester = new WsActionTester(sut);

  @Test
  public void request_fails_in_production_mode_with_ForbiddenException_when_user_is_not_logged_in() {
    when(nodeInformation.isStandalone()).thenReturn(true);

    assertThatThrownBy(() -> actionTester.newRequest().execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void request_fails_in_production_mode_with_ForbiddenException_when_user_is_not_system_administrator() {
    when(nodeInformation.isStandalone()).thenReturn(true);
    userSessionRule.logIn().setNonSystemAdministrator();

    assertThatThrownBy(() -> actionTester.newRequest().execute())
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void request_fails_in_cluster_mode_with_IllegalArgumentException() {
    when(nodeInformation.isStandalone()).thenReturn(false);

    assertThatThrownBy(() -> actionTester.newRequest().execute())
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Restart not allowed for cluster nodes");
  }

  @Test
  public void calls_ProcessCommandWrapper_requestForSQRestart_in_production_mode() {
    when(nodeInformation.isStandalone()).thenReturn(true);
    userSessionRule.logIn().setSystemAdministrator();

    actionTester.newRequest().execute();

    inOrder.verify(restartFlagHolder).set();
    inOrder.verify(processCommandWrapper).requestSQRestart();
  }

  @Test
  public void logs_login_of_authenticated_user_requesting_the_restart_in_production_mode() {
    when(nodeInformation.isStandalone()).thenReturn(true);
    String login = "BigBother";
    userSessionRule.logIn(login).setSystemAdministrator();

    actionTester.newRequest().execute();

    assertThat(logTester.logs(Level.INFO))
      .contains("SonarQube restart requested by " + login);
  }

}
