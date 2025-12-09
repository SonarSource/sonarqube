/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.log;

import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;
import org.sonar.process.logging.LogLevelConfig;
import org.sonar.process.logging.RootLoggerConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.ProcessId.WEB_SERVER;
import static org.sonar.process.logging.RootLoggerConfig.newRootLoggerConfigBuilder;

public class ServerProcessLoggingTest {

  @Test
  public void buildRootLoggerConfig_shouldBuildConfig() {
    ServerProcessLogging serverProcessLogging = getServerProcessLoggingFakeImpl(WEB_SERVER, "threadIdFieldPattern");
    Props props = Mockito.mock(Props.class);
    RootLoggerConfig expected = newRootLoggerConfigBuilder()
      .setProcessId(WEB_SERVER)
      .setNodeNameField(null)
      .setThreadIdFieldPattern("threadIdFieldPattern")
      .build();

    RootLoggerConfig result = serverProcessLogging.buildRootLoggerConfig(props);

    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  private ServerProcessLogging getServerProcessLoggingFakeImpl(ProcessId processId, String threadIdFieldPattern) {
    return new ServerProcessLogging(processId, threadIdFieldPattern) {
      @Override
      protected void extendLogLevelConfiguration(LogLevelConfig.Builder logLevelConfigBuilder) {
        //Not needed for this test
      }

      @Override
      protected void extendConfigure(Props props) {
        //Not needed for this test
      }
    };
  }
}
