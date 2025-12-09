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
package org.sonar.server.platform;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedStatic;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.Version;
import org.sonar.core.platform.SonarQubeVersion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class LogServerVersionTest {

  @RegisterExtension
  private final LogTesterJUnit5 logs = new LogTesterJUnit5();

  private final Version version = Version.create(7, 9);
  private final SonarQubeVersion sonarQubeVersion = new SonarQubeVersion(version);
  private final LogServerVersion logServerVersion = new LogServerVersion(sonarQubeVersion);

  @Test
  void start_should_log_info_message_when_info_enabled() {
    try (MockedStatic<LogServerVersion> mockedStatic = mockStatic(LogServerVersion.class)) {
      Properties properties = new Properties();
      properties.setProperty("Implementation-Build", "42");

      mockedStatic.when(() -> LogServerVersion.read("/build.properties")).thenReturn(properties);

      logs.setLevel(Level.INFO);

      logServerVersion.start();

      assertThat(logs.logs(Level.INFO)).contains("SonarQube Server / 7.9 / 42");
    }
  }

}
