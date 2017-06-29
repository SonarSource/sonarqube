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
package org.sonar.server.platform;

import ch.qos.logback.classic.Level;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.process.ProcessProperties;
import org.sonar.process.logging.LogLevelConfig;
import org.sonar.process.logging.LogbackHelper;
import org.sonar.server.app.ServerProcessLogging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class ServerLoggingTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private LogbackHelper logbackHelper = spy(new LogbackHelper());
  private MapSettings settings = new MapSettings();
  private ServerLogging underTest = new ServerLogging(logbackHelper, settings.asConfig());

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void getLogsDir() throws IOException {
    File dir = temp.newFolder();
    settings.setProperty(ProcessProperties.PATH_LOGS, dir.getAbsolutePath());

    assertThat(underTest.getLogsDir()).isEqualTo(dir);
  }

  @Test
  public void getRootLoggerLevel() {
    logTester.setLevel(LoggerLevel.TRACE);
    assertThat(underTest.getRootLoggerLevel()).isEqualTo(LoggerLevel.TRACE);
  }

  @Test
  @UseDataProvider("supportedSonarApiLevels")
  public void changeLevel_calls_changeRoot_with_LogLevelConfig_and_level_converted_to_logback_class_then_log_INFO_message(LoggerLevel level) {
    ServerProcessLogging serverProcessLogging = mock(ServerProcessLogging.class);
    LogLevelConfig logLevelConfig = LogLevelConfig.newBuilder().build();
    when(serverProcessLogging.getLogLevelConfig()).thenReturn(logLevelConfig);

    underTest.changeLevel(serverProcessLogging, level);

    verify(logbackHelper).changeRoot(logLevelConfig, Level.valueOf(level.name()));
  }

  @DataProvider
  public static Object[][] supportedSonarApiLevels() {
    return new Object[][] {
      {LoggerLevel.INFO},
      {LoggerLevel.DEBUG},
      {LoggerLevel.TRACE}
    };
  }

  @Test
  public void changeLevel_fails_with_IAE_when_level_is_ERROR() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("ERROR log level is not supported (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.changeLevel(mock(ServerProcessLogging.class), LoggerLevel.ERROR);
  }

  @Test
  public void changeLevel_fails_with_IAE_when_level_is_WARN() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("WARN log level is not supported (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.changeLevel(mock(ServerProcessLogging.class), LoggerLevel.WARN);
  }
}
