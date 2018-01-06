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
package org.sonar.server.platform.monitoring.cluster;

import java.io.File;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.process.systeminfo.protobuf.ProtobufSystemInfo;
import org.sonar.server.platform.ServerLogging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.monitoring.SystemInfoTesting.assertThatAttributeIs;

public class LoggingSectionTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private SonarRuntime runtime = mock(SonarRuntime.class);
  private ServerLogging logging = mock(ServerLogging.class);
  private File logDir;
  private LoggingSection underTest = new LoggingSection(runtime, logging);

  @Before
  public void setUp() throws Exception {
    logDir = temp.newFolder();
    when(logging.getLogsDir()).thenReturn(logDir);
    when(logging.getRootLoggerLevel()).thenReturn(LoggerLevel.DEBUG);
  }

  @Test
  public void return_logging_attributes_of_compute_engine() {
    when(runtime.getSonarQubeSide()).thenReturn(SonarQubeSide.COMPUTE_ENGINE);

    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThat(section.getName()).isEqualTo("Compute Engine Logging");
    assertThatAttributeIs(section, "Logs Dir", logDir.getAbsolutePath());
    assertThatAttributeIs(section, "Logs Level", "DEBUG");
  }

  @Test
  public void return_logging_attributes_of_web_server() {
    when(runtime.getSonarQubeSide()).thenReturn(SonarQubeSide.SERVER);

    ProtobufSystemInfo.Section section = underTest.toProtobuf();
    assertThat(section.getName()).isEqualTo("Web Logging");
    assertThatAttributeIs(section, "Logs Dir", logDir.getAbsolutePath());
    assertThatAttributeIs(section, "Logs Level", "DEBUG");
  }
}
