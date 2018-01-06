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

import java.io.StringWriter;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.ce.http.CeHttpClient;
import org.sonar.ce.http.CeHttpClientImpl;
import org.sonar.process.systeminfo.SystemInfoSection;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.health.TestStandaloneHealthChecker;
import org.sonar.server.telemetry.TelemetryDataLoader;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.process.systeminfo.SystemInfoUtils.setAttribute;

public class StandaloneSystemInfoWriterTest {
  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone()
    .logIn("login")
    .setName("name");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SystemInfoSection section1 = mock(SystemInfoSection.class);
  private SystemInfoSection section2 = mock(SystemInfoSection.class);
  private CeHttpClient ceHttpClient = mock(CeHttpClientImpl.class, Mockito.RETURNS_MOCKS);
  private TestStandaloneHealthChecker healthChecker = new TestStandaloneHealthChecker();
  private TelemetryDataLoader telemetry = mock(TelemetryDataLoader.class, Mockito.RETURNS_MOCKS);

  private StandaloneSystemInfoWriter underTest = new StandaloneSystemInfoWriter(telemetry, ceHttpClient, healthChecker, section1, section2);

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

    StringWriter writer = new StringWriter();
    JsonWriter jsonWriter = JsonWriter.of(writer);
    jsonWriter.beginObject();
    underTest.write(jsonWriter);
    jsonWriter.endObject();
    // response does not contain empty "Section Three"
    assertThat(writer.toString()).isEqualTo("{\"Health\":\"GREEN\",\"Health Causes\":[],\"Section One\":{\"foo\":\"bar\"},\"Section Two\":{\"one\":1,\"two\":2}," +
      "\"Statistics\":{\"id\":\"\",\"version\":\"\",\"database\":{\"name\":\"\",\"version\":\"\"},\"plugins\":[],\"userCount\":0,\"projectCount\":0,\"usingBranches\":false," +
      "\"lines\":0,\"ncloc\":0,\"projectCountByLanguage\":[],\"nclocByLanguage\":[]}}");
  }

  private void logInAsSystemAdministrator() {
    userSessionRule.logIn().setSystemAdministrator();
  }
}
