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
package org.sonar.server.ce.http;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okio.Buffer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.process.sharedmemoryfile.DefaultProcessCommands;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessId;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.test.ExceptionCauseMatcher.hasType;

public class CeHttpClientTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public MockWebServer server = new MockWebServer();

  private File ipcSharedDir;
  private CeHttpClient underTest;

  @Before
  public void setUp() throws Exception {
    ipcSharedDir = temp.newFolder();
    MapSettings settings = new MapSettings();
    settings.setProperty(ProcessEntryPoint.PROPERTY_SHARED_PATH, ipcSharedDir.getAbsolutePath());
    underTest = new CeHttpClientImpl(settings.asConfig());
  }

  @Test
  public void retrieveSystemInfo_returns_absent_if_process_is_down() {
    Optional<ProtobufSystemInfo.SystemInfo> info = underTest.retrieveSystemInfo();

    assertThat(info.isPresent()).isFalse();
  }

  @Test
  public void retrieveSystemInfo_get_information_if_process_is_up() {
    Buffer response = new Buffer();
    response.read(ProtobufSystemInfo.Section.newBuilder().build().toByteArray());
    server.enqueue(new MockResponse().setBody(response));

    // initialize registration of process
    setUpWithHttpUrl(ProcessId.COMPUTE_ENGINE);

    Optional<ProtobufSystemInfo.SystemInfo> info = underTest.retrieveSystemInfo();
    assertThat(info.get().getSectionsCount()).isEqualTo(0);
  }

  @Test
  public void retrieveSystemInfo_throws_ISE_if_http_error() {
    server.enqueue(new MockResponse().setResponseCode(500));

    // initialize registration of process
    setUpWithHttpUrl(ProcessId.COMPUTE_ENGINE);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Failed to call HTTP server of process " + ProcessId.COMPUTE_ENGINE);
    expectedException.expectCause(hasType(IOException.class)
      .andMessage(format("Server returned HTTP response code: 500 for URL: http://%s:%d/systemInfo", server.getHostName(), server.getPort())));
    underTest.retrieveSystemInfo();
  }

  @Test
  public void changeLogLevel_throws_NPE_if_level_argument_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("level can't be null");

    underTest.changeLogLevel(null);
  }

  @Test
  public void changeLogLevel_throws_ISE_if_http_error() {
    String message = "blah";
    server.enqueue(new MockResponse().setResponseCode(500).setBody(message));

    // initialize registration of process
    setUpWithHttpUrl(ProcessId.COMPUTE_ENGINE);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Failed to call HTTP server of process " + ProcessId.COMPUTE_ENGINE);
    expectedException.expectCause(hasType(IOException.class)
      .andMessage(format("Failed to change log level in Compute Engine. Code was '500' and response was 'blah' for url " +
        "'http://%s:%s/changeLogLevel'", server.getHostName(), server.getPort())));

    underTest.changeLogLevel(LoggerLevel.DEBUG);
  }

  @Test
  public void changeLogLevel_does_not_fail_when_http_code_is_200() {
    server.enqueue(new MockResponse().setResponseCode(200));

    setUpWithHttpUrl(ProcessId.COMPUTE_ENGINE);

    underTest.changeLogLevel(LoggerLevel.TRACE);
  }

  @Test
  public void changelogLevel_does_not_fail_if_process_is_down() {
    underTest.changeLogLevel(LoggerLevel.INFO);
  }

  private void setUpWithHttpUrl(ProcessId processId) {
    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(ipcSharedDir, processId.getIpcIndex())) {
      processCommands.setUp();
      processCommands.setHttpUrl(format("http://%s:%d", server.getHostName(), server.getPort()));
    }
  }
}
