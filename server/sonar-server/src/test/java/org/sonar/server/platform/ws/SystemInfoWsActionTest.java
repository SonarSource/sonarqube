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

import org.junit.Test;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.internal.SimpleGetRequest;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.monitoring.Monitor;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.ws.WsTester;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SystemInfoWsActionTest {

  Monitor monitor1 = mock(Monitor.class);
  Monitor monitor2 = mock(Monitor.class);
  SystemInfoWsAction sut = new SystemInfoWsAction(monitor1, monitor2);

  @Test(expected = ForbiddenException.class)
  public void should_fail_when_does_not_have_admin_right() throws Exception {
    MockUserSession.set()
      .setLogin("login")
      .setName("name")
      .setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    sut.handle(mock(Request.class), mock(Response.class));
  }

  @Test
  public void write_json() throws Exception {
    MockUserSession.set()
      .setLogin("login")
      .setName("name")
      .setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    LinkedHashMap<String, Object> attributes1 = new LinkedHashMap<>();
    attributes1.put("foo", "bar");
    LinkedHashMap<String, Object> attributes2 = new LinkedHashMap<>();
    attributes2.put("one", 1);
    attributes2.put("two", 2);
    when(monitor1.name()).thenReturn("Monitor One");
    when(monitor1.attributes()).thenReturn(attributes1);
    when(monitor2.name()).thenReturn("Monitor Two");
    when(monitor2.attributes()).thenReturn(attributes2);

    WsTester.TestResponse response = new WsTester.TestResponse();
    sut.handle(new SimpleGetRequest(), response);
    assertThat(response.outputAsString()).isEqualTo("{\"Monitor One\":{\"foo\":\"bar\"},\"Monitor Two\":{\"one\":1,\"two\":2}}");
  }
}
