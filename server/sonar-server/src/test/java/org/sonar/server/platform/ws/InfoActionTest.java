/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.ce.http.CeHttpClient;
import org.sonar.ce.http.CeHttpClientImpl;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.platform.monitoring.Monitor;
import org.sonar.server.telemetry.TelemetryData;
import org.sonar.server.telemetry.TelemetryDataLoader;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InfoActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone().logIn("login")
    .setName("name");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Monitor monitor1 = mock(Monitor.class);
  private Monitor monitor2 = mock(Monitor.class);
  private CeHttpClient ceHttpClient = mock(CeHttpClientImpl.class, Mockito.RETURNS_MOCKS);
  private TelemetryDataLoader statistics = mock(TelemetryDataLoader.class);

  private InfoAction underTest = new InfoAction(userSessionRule, ceHttpClient, statistics, monitor1, monitor2);
  private WsActionTester ws = new WsActionTester(underTest);

  @Test
  public void test_definition() throws Exception {
    assertThat(ws.getDef().key()).isEqualTo("info");
    assertThat(ws.getDef().isInternal()).isTrue();
    assertThat(ws.getDef().responseExampleAsString()).isNotEmpty();
    assertThat(ws.getDef().params()).isEmpty();
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_logged_in() {
    expectedException.expect(ForbiddenException.class);

    ws.newRequest().execute();
  }

  @Test
  public void request_fails_with_ForbiddenException_when_user_is_not_system_administrator() {
    userSessionRule.logIn().setNonSystemAdministrator();

    expectedException.expect(ForbiddenException.class);

    ws.newRequest().execute();
  }

  @Test
  public void write_json() {
    logInAsSystemAdministrator();

    Map<String, Object> attributes1 = new LinkedHashMap<>();
    attributes1.put("foo", "bar");
    Map<String, Object> attributes2 = new LinkedHashMap<>();
    attributes2.put("one", 1);
    attributes2.put("two", 2);
    when(monitor1.name()).thenReturn("Monitor One");
    when(monitor1.attributes()).thenReturn(attributes1);
    when(monitor2.name()).thenReturn("Monitor Two");
    when(monitor2.attributes()).thenReturn(attributes2);
    when(ceHttpClient.retrieveSystemInfo()).thenReturn(Optional.empty());
    when(statistics.load()).thenReturn(mock(TelemetryData.class));

    TestResponse response = ws.newRequest().execute();
    // response does not contain empty "Monitor Three"
    verify(statistics).load();
    assertThat(response.getInput()).isEqualTo("{\"Monitor One\":{\"foo\":\"bar\"},\"Monitor Two\":{\"one\":1,\"two\":2}," +
      "\"Statistics\":{\"plugins\":{},\"userCount\":0,\"projectCount\":0,\"lines\":0,\"ncloc\":0,\"projectCountByLanguage\":{},\"nclocByLanguage\":{}}}");
  }

  private void logInAsSystemAdministrator() {
    userSessionRule.logIn().setSystemAdministrator();
  }
}
