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
package org.sonar.server.platform.ws;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.web.UserRole;
import org.sonar.db.Database;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.ServerLogging;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsActionTester;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ChangeLogLevelActionTest {

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  ServerLogging serverLogging = mock(ServerLogging.class);
  Database db = mock(Database.class);
  ChangeLogLevelAction underTest = new ChangeLogLevelAction(userSession, serverLogging, db);
  WsActionTester actionTester = new WsActionTester(underTest);

  @Test
  public void enable_debug_logs() {
    userSession.setGlobalPermissions(UserRole.ADMIN);

    actionTester.newRequest()
      .setParam("level", "DEBUG")
      .setMethod("POST")
      .execute();
    verify(serverLogging).changeLevel(LoggerLevel.DEBUG);
    verify(db).enableSqlLogging(false);
  }

  @Test
  public void enable_trace_logs() {
    userSession.setGlobalPermissions(UserRole.ADMIN);

    actionTester.newRequest()
      .setParam("level", "TRACE")
      .setMethod("POST")
      .execute();
    verify(serverLogging).changeLevel(LoggerLevel.TRACE);
    verify(db).enableSqlLogging(true);
  }

  @Test
  public void fail_if_unsupported_level() {
    expectedException.expect(IllegalArgumentException.class);
    userSession.setGlobalPermissions(UserRole.ADMIN);
    actionTester.newRequest()
      .setParam("level", "ERROR")
      .setMethod("POST")
      .execute();
  }

  @Test
  public void fail_if_missing_level() {
    expectedException.expect(IllegalArgumentException.class);
    userSession.setGlobalPermissions(UserRole.ADMIN);
    actionTester.newRequest()
      .setMethod("POST")
      .execute();
  }

  @Test
  public void requires_admin_permission() {
    expectedException.expect(ForbiddenException.class);

    actionTester.newRequest().setMethod("POST").execute();
  }
}
