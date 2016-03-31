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

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.internal.SimpleGetRequest;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.monitoring.Monitor;
import org.sonar.server.platform.monitoring.ProcessSystemInfoClient;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.WsTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InfoActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone().login("login")
    .setName("name");

  Monitor monitor1 = mock(Monitor.class);
  Monitor monitor2 = mock(Monitor.class);
  ProcessSystemInfoClient processSystemInfoClient = mock(ProcessSystemInfoClient.class, Mockito.RETURNS_MOCKS);
  InfoAction underTest = new InfoAction(userSessionRule, processSystemInfoClient, monitor1, monitor2);

  @Test(expected = ForbiddenException.class)
  public void should_fail_when_does_not_have_admin_right() {
    userSessionRule.setGlobalPermissions(GlobalPermissions.SCAN_EXECUTION);

    underTest.handle(mock(Request.class), mock(Response.class));
  }

  @Test
  public void write_json() {
    userSessionRule.setGlobalPermissions(GlobalPermissions.SYSTEM_ADMIN);

    Map<String, Object> attributes1 = new LinkedHashMap<>();
    attributes1.put("foo", "bar");
    Map<String, Object> attributes2 = new LinkedHashMap<>();
    attributes2.put("one", 1);
    attributes2.put("two", 2);
    when(monitor1.name()).thenReturn("Monitor One");
    when(monitor1.attributes()).thenReturn(attributes1);
    when(monitor2.name()).thenReturn("Monitor Two");
    when(monitor2.attributes()).thenReturn(attributes2);

    WsTester.TestResponse response = new WsTester.TestResponse();
    underTest.handle(new SimpleGetRequest(), response);
    // response does not contain empty "Monitor Three"
    assertThat(response.outputAsString()).isEqualTo("{\"Monitor One\":{\"foo\":\"bar\"},\"Monitor Two\":{\"one\":1,\"two\":2}}");
  }
}
