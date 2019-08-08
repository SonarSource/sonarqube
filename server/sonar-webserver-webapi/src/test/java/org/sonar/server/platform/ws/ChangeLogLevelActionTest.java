/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.server.ce.http.CeHttpClient;
import org.sonar.server.ce.http.CeHttpClientImpl;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.log.ServerLogging;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ChangeLogLevelActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ServerLogging serverLogging = mock(ServerLogging.class);
  private CeHttpClient ceHttpClient = mock(CeHttpClientImpl.class);
  private ChangeLogLevelAction underTest = new ChangeLogLevelAction(userSession, new ChangeLogLevelStandaloneService(serverLogging, ceHttpClient));
  private WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_logged_in() {
    expectedException.expect(ForbiddenException.class);

    actionTester.newRequest().setMethod("POST").execute();
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_system_administrator() {
    userSession.logIn().setNonSystemAdministrator();

    expectedException.expect(ForbiddenException.class);

    actionTester.newRequest().setMethod("POST").execute();
  }

  @Test
  public void enable_debug_logs() {
    logInAsSystemAdministrator();

    actionTester.newRequest()
      .setParam("level", "DEBUG")
      .setMethod("POST")
      .execute();

    verify(serverLogging).changeLevel(LoggerLevel.DEBUG);
    verify(ceHttpClient).changeLogLevel(LoggerLevel.DEBUG);
  }

  @Test
  public void enable_trace_logs() {
    logInAsSystemAdministrator();

    actionTester.newRequest()
      .setParam("level", "TRACE")
      .setMethod("POST")
      .execute();

    verify(serverLogging).changeLevel(LoggerLevel.TRACE);
    verify(ceHttpClient).changeLogLevel(LoggerLevel.TRACE);
  }

  @Test
  public void fail_if_unsupported_level() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);

    actionTester.newRequest()
      .setParam("level", "ERROR")
      .setMethod("POST")
      .execute();
  }

  @Test
  public void fail_if_missing_level() {
    logInAsSystemAdministrator();

    expectedException.expect(IllegalArgumentException.class);

    actionTester.newRequest()
      .setMethod("POST")
      .execute();
  }

  private void logInAsSystemAdministrator() {
    userSession.logIn().setSystemAdministrator();
  }
}
