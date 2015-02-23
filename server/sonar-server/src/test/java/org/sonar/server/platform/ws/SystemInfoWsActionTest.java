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

package org.sonar.server.platform.ws;

import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Condition;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.monitoring.Monitor;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SystemInfoWsActionTest {

  @ClassRule
  public static ServerTester serverTester = new ServerTester();

  private SystemInfoWsAction sut;

  @Test
  public void all_monitors_have_a_name() throws Exception {
    sut = serverTester.get(SystemInfoWsAction.class);

    assertThat(sut.getMonitors()).areNot(withEmptyName());
  }

  @Test(expected = ForbiddenException.class)
  public void should_fail_when_does_not_have_admin_right() throws Exception {
    sut = serverTester.get(SystemInfoWsAction.class);
    MockUserSession.set()
      .setLogin("login")
      .setName("name")
      .setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    sut.handle(mock(Request.class), mock(Response.class));
  }

  private Condition<Monitor> withEmptyName() {
    return new Condition<Monitor>() {
      @Override
      public boolean matches(Monitor m) {
        return StringUtils.isEmpty(m.name());
      }
    };
  }
}
