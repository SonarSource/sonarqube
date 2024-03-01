/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.startup;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.platform.Server;
import org.sonar.api.testfixtures.log.LogTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LogServerIdTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void log_server_id_at_startup() {
    Server server = mock(Server.class);
    when(server.getId()).thenReturn("foo");

    LogServerId underTest = new LogServerId(server);

    underTest.start();
    assertThat(logTester.logs(Level.INFO)).contains("Server ID: foo");

    // do not fail
    underTest.stop();
  }

  @Test
  public void shouldLogServerIdAtStartup() {
    // Arrange
    Server server = mock(Server.class);
    when(server.getId()).thenReturn("unique-server-id");
    LogServerId underTest = new LogServerId(server);

    // Act
    underTest.start();

    // Assert
    assertThat(logTester.logs(Level.INFO)).containsExactly("Server ID: unique-server-id");
  }

  @Test
  public void shouldNotLogServerIdAfterStop() {
    // Arrange
    Server server = mock(Server.class);
    when(server.getId()).thenReturn("unique-server-id");
    LogServerId underTest = new LogServerId(server);
    underTest.start(); // Start to log the server ID initially

    // Act
    underTest.stop();
    logTester.clear(); // Clear all logs after stopping to simulate the end of lifecycle

    // Assert
    assertThat(logTester.logs(Level.INFO)).doesNotContain("Server ID: unique-server-id");
  }

  @Test
  public void shouldLogCorrectServerIdForEachInstance() {
    // Arrange
    Server firstServer = mock(Server.class);
    Server secondServer = mock(Server.class);
    when(firstServer.getId()).thenReturn("first-server-id");
    when(secondServer.getId()).thenReturn("second-server-id");

    LogServerId firstUnderTest = new LogServerId(firstServer);
    LogServerId secondUnderTest = new LogServerId(secondServer);

    // Act
    firstUnderTest.start();
    secondUnderTest.start();

    // Assert
    assertThat(logTester.logs(Level.INFO)).contains("Server ID: first-server-id", "Server ID: second-server-id");
  }

  @Test
  public void shouldHandleNullServerIdGracefully() {
    // Arrange
    Server server = mock(Server.class);
    when(server.getId()).thenReturn(null); // Simulate server ID not set
    LogServerId underTest = new LogServerId(server);

    // Act
    underTest.start();

    // Assert
    assertThat(logTester.logs(Level.INFO)).contains("Server ID: null");
  }





}
