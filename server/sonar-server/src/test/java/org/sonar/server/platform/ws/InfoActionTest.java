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

import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.ce.http.CeHttpClient;
import org.sonar.ce.http.CeHttpClientImpl;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.health.TestStandaloneHealthChecker;
import org.sonar.server.telemetry.TelemetryData;
import org.sonar.server.telemetry.TelemetryDataLoader;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

public class InfoActionTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone().logIn("login")
    .setName("name");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SystemInfoSection section1 = mock(SystemInfoSection.class);
  private SystemInfoSection section2 = mock(SystemInfoSection.class);
  private CeHttpClient ceHttpClient = mock(CeHttpClientImpl.class, Mockito.RETURNS_MOCKS);
  private TestStandaloneHealthChecker healthChecker = new TestStandaloneHealthChecker();
  private TelemetryDataLoader statistics = mock(TelemetryDataLoader.class);

  private InfoAction underTest = new InfoAction(userSessionRule, ceHttpClient, healthChecker, statistics, section1, section2);
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

    ProtobufSystemInfo.Section.Builder attributes1 = ProtobufSystemInfo.Section.newBuilder()
      .setName("Section One");
    setAttribute(attributes1, "foo", "bar");
    when(section1.toProtobuf()).thenReturn(attributes1.build());

    ProtobufSystemInfo.Section.Builder attributes2 = ProtobufSystemInfo.Section.newBuilder()
      .setName("Section Two");
    setAttribute(attributes2, "one", 1);
    setAttribute(attributes2, "two", 2);
    when(section2.toProtobuf()).thenReturn(attributes2.build());
    when(ceHttpClient.retrieveSystemInfo()).thenReturn(Optional.empty());
    when(statistics.load()).thenReturn(mock(TelemetryData.class));

    TestResponse response = ws.newRequest().execute();
    // response does not contain empty "Section Three"
    verify(statistics).load();
    assertThat(response.getInput()).isEqualTo("{\"Health\":\"GREEN\",\"Health Causes\":[],\"Section One\":{\"foo\":\"bar\"},\"Section Two\":{\"one\":1,\"two\":2}," +
      "\"Statistics\":{\"plugins\":{},\"userCount\":0,\"projectCount\":0,\"lines\":0,\"ncloc\":0,\"projectCountByLanguage\":{},\"nclocByLanguage\":{}}}");
  }

  private void logInAsSystemAdministrator() {
    userSessionRule.logIn().setSystemAdministrator();
  }
}
