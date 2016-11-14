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
package org.sonar.server.platform;

import ch.qos.logback.classic.Level;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.config.MapSettings;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.process.LogbackHelper;
import org.sonar.process.ProcessProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class ServerLoggingTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private LogbackHelper logbackHelper = spy(new LogbackHelper());
  private Settings settings = new MapSettings();
  private ServerLogging underTest = new ServerLogging(logbackHelper, settings);

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
  public void getCurrentLogFile() throws IOException {
    File dir = temp.newFolder();
    File logFile = new File(dir, "sonar.log");
    FileUtils.touch(logFile);
    settings.setProperty(ProcessProperties.PATH_LOGS, dir.getAbsolutePath());

    assertThat(underTest.getCurrentLogFile()).isEqualTo(logFile);
  }

  @Test
  public void configureHardcodedLevels() {
    LogbackHelper logbackHelper = mock(LogbackHelper.class);
    ServerLogging.configureHardcodedLevels(logbackHelper);

    verifyHardcodedLevels(logbackHelper);
  }

  @Test
  public void changeLevel_throws_IAE_if_level_is_WARN() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("WARN log level is not supported (allowed levels are [");

    underTest.changeLevel(LoggerLevel.WARN);
  }

  @Test
  public void changeLevel_throws_IAE_if_level_is_ERROR() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("ERROR log level is not supported (allowed levels are [");

    underTest.changeLevel(LoggerLevel.ERROR);
  }

  @Test
  public void changeLevel_changes_root_logger_level_to_INFO() {
    underTest.changeLevel(LoggerLevel.INFO);

    verify(logbackHelper).configureRootLogLevel(Level.INFO);
    verifyHardcodedLevels(logbackHelper);
  }

  @Test
  public void changeLevel_changes_root_logger_level_to_DEBUG() {
    underTest.changeLevel(LoggerLevel.DEBUG);

    verify(logbackHelper).configureRootLogLevel(Level.DEBUG);
    verifyHardcodedLevels(logbackHelper);
  }

  @Test
  public void changeLevel_changes_root_logger_level_to_TRACE() {
    underTest.changeLevel(LoggerLevel.TRACE);

    verify(logbackHelper).configureRootLogLevel(Level.TRACE);
    verifyHardcodedLevels(logbackHelper);
  }

  private void verifyHardcodedLevels(LogbackHelper logbackHelper) {
    verify(logbackHelper).configureLogger("rails", Level.WARN);
    verify(logbackHelper).configureLogger("org.apache.ibatis", Level.WARN);
    verify(logbackHelper).configureLogger("java.sql", Level.WARN);
    verify(logbackHelper).configureLogger("java.sql.ResultSet", Level.WARN);
    verify(logbackHelper).configureLogger("org.sonar.MEASURE_FILTER", Level.WARN);
    verify(logbackHelper).configureLogger("org.elasticsearch", Level.INFO);
    verify(logbackHelper).configureLogger("org.elasticsearch.node", Level.INFO);
    verify(logbackHelper).configureLogger("org.elasticsearch.http", Level.INFO);
    verify(logbackHelper).configureLogger("ch.qos.logback", Level.WARN);
    verify(logbackHelper).configureLogger("org.apache.catalina", Level.INFO);
    verify(logbackHelper).configureLogger("org.apache.coyote", Level.INFO);
    verify(logbackHelper).configureLogger("org.apache.jasper", Level.INFO);
    verify(logbackHelper).configureLogger("org.apache.tomcat", Level.INFO);
  }
}
