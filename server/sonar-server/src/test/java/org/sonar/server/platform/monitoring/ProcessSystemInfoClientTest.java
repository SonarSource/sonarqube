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
package org.sonar.server.platform.monitoring;

import com.google.common.base.Optional;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;
import java.io.File;
import okio.Buffer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.Settings;
import org.sonar.process.DefaultProcessCommands;
import org.sonar.process.ProcessEntryPoint;
import org.sonar.process.ProcessId;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

public class ProcessSystemInfoClientTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public MockWebServer server = new MockWebServer();

  File ipcSharedDir;
  ProcessSystemInfoClient underTest;

  @Before
  public void setUp() throws Exception {
    ipcSharedDir = temp.newFolder();
    Settings settings = new Settings();
    settings.setProperty(ProcessEntryPoint.PROPERTY_SHARED_PATH, ipcSharedDir.getAbsolutePath());
    underTest = new ProcessSystemInfoClient(settings);
  }

  @Test
  public void connect_returns_absent_if_process_is_down() throws Exception {
    Optional<ProtobufSystemInfo.SystemInfo> info = underTest.connect(ProcessId.COMPUTE_ENGINE);

    assertThat(info.isPresent()).isFalse();
  }

  @Test
  public void get_information_if_process_is_up() throws Exception {
    Buffer response = new Buffer();
    response.read(ProtobufSystemInfo.Section.newBuilder().build().toByteArray());
    server.enqueue(new MockResponse().setBody(response));

    // initialize registration of process
    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(ipcSharedDir, ProcessId.COMPUTE_ENGINE.getIpcIndex())) {
      processCommands.setUp();
      processCommands.setSystemInfoUrl(format("http://%s:%d", server.getHostName(), server.getPort()));
    }

    Optional<ProtobufSystemInfo.SystemInfo> info = underTest.connect(ProcessId.COMPUTE_ENGINE);
    assertThat(info.get().getSectionsCount()).isEqualTo(0);
  }

  @Test
  public void throws_ISE_if_http_error() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500));

    // initialize registration of process
    try (DefaultProcessCommands processCommands = DefaultProcessCommands.secondary(ipcSharedDir, ProcessId.COMPUTE_ENGINE.getIpcIndex())) {
      processCommands.setUp();
      processCommands.setSystemInfoUrl(format("http://%s:%d", server.getHostName(), server.getPort()));
    }

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Can not get system info of process " + ProcessId.COMPUTE_ENGINE);
    underTest.connect(ProcessId.COMPUTE_ENGINE);
  }
}
