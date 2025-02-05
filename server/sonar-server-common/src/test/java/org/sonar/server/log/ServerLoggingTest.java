/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import ch.qos.logback.classic.Level;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.db.Database;
import org.sonar.process.ProcessProperties;
import org.sonar.process.logging.LogLevelConfig;
import org.sonar.process.logging.LogbackHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.log.LoggerLevel.DEBUG;
import static org.sonar.api.utils.log.LoggerLevel.ERROR;
import static org.sonar.api.utils.log.LoggerLevel.INFO;
import static org.sonar.api.utils.log.LoggerLevel.TRACE;
import static org.sonar.api.utils.log.LoggerLevel.WARN;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;

@RunWith(DataProviderRunner.class)
public class ServerLoggingTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private final String rootLoggerName = RandomStringUtils.secure().nextAlphabetic(20);
  private final LogbackHelper logbackHelper = spy(new LogbackHelper());
  private final MapSettings settings = new MapSettings();
  private final ServerProcessLogging serverProcessLogging = mock(ServerProcessLogging.class);
  private final Database database = mock(Database.class);
  private final ServerLogging underTest = new ServerLogging(logbackHelper, settings.asConfig(), serverProcessLogging, database);

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void getLogsDir() throws IOException {
    File dir = temp.newFolder();
    settings.setProperty(PATH_LOGS.getKey(), dir.getAbsolutePath());

    assertThat(underTest.getLogsDir()).isEqualTo(dir);
  }

  @Test
  public void getRootLoggerLevel() {
    logTester.setLevel(TRACE);
    assertThat(underTest.getRootLoggerLevel()).isEqualTo(TRACE);
  }

  @Test
  @UseDataProvider("supportedSonarApiLevels")
  public void changeLevel_calls_changeRoot_with_LogLevelConfig_and_level_converted_to_logback_class_then_log_INFO_message(LoggerLevel level) {
    LogLevelConfig logLevelConfig = LogLevelConfig.newBuilder(rootLoggerName).build();
    when(serverProcessLogging.getLogLevelConfig()).thenReturn(logLevelConfig);

    underTest.changeLevel(level);

    verify(logbackHelper).changeRoot(logLevelConfig, Level.valueOf(level.name()));
  }

  @Test
  public void changeLevel_to_trace_enables_db_logging() {
    LogLevelConfig logLevelConfig = LogLevelConfig.newBuilder(rootLoggerName).build();
    when(serverProcessLogging.getLogLevelConfig()).thenReturn(logLevelConfig);

    reset(database);
    underTest.changeLevel(INFO);
    verify(database).enableSqlLogging(false);

    reset(database);
    underTest.changeLevel(DEBUG);
    verify(database).enableSqlLogging(false);

    reset(database);
    underTest.changeLevel(TRACE);
    verify(database).enableSqlLogging(true);
  }

  @DataProvider
  public static Object[][] supportedSonarApiLevels() {
    return new Object[][] {
      {INFO},
      {DEBUG},
      {TRACE}
    };
  }

  @Test
  public void changeLevel_fails_with_IAE_when_level_is_ERROR() {
    assertThatThrownBy(() -> underTest.changeLevel(ERROR))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("ERROR log level is not supported (allowed levels are [TRACE, DEBUG, INFO])");
  }

  @Test
  public void changeLevel_fails_with_IAE_when_level_is_WARN() {
    assertThatThrownBy(() -> underTest.changeLevel(WARN))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("WARN log level is not supported (allowed levels are [TRACE, DEBUG, INFO])");
  }

  @Test
  public void getLogsForSingleNode_shouldReturnFile() throws IOException {
    File dir = temp.newFolder();
    settings.setProperty(PATH_LOGS.getKey(), dir.getAbsolutePath());
    File file = new File(dir, "ce.log");
    file.createNewFile();

    File ce = underTest.getLogsForSingleNode("ce");

    assertThat(ce).isFile();
  }

  @Test
  public void getLogFilePath_whenMatchingFileDoesNotExist_shouldReturnEmpty() throws IOException {
    File dir = temp.newFolder();
    settings.setProperty(PATH_LOGS.getKey(), dir.getAbsolutePath());
    File file = new File(dir, "ce.log");
    file.createNewFile();

    Optional<Path> path = underTest.getLogFilePath("web", dir);

    assertThat(path).isEmpty();
  }

  @Test
  public void getLogFilePath_whenMatchingFileExists_shouldReturnPath() throws IOException {
    File dir = temp.newFolder();
    settings.setProperty(PATH_LOGS.getKey(), dir.getAbsolutePath());
    File file = new File(dir, "web.log");
    file.createNewFile();

    Optional<Path> path = underTest.getLogFilePath("web", dir);

    assertThat(path).isNotEmpty();
  }

  @Test
  public void hasMatchingLogFiles_shouldReturnFalse() {
    boolean result = underTest.hasMatchingLogFiles("ce").test(Path.of("web.log"));

    assertThat(result).isFalse();
  }

  @Test
  public void hasMatchingLogFiles_shouldReturnTrue() {
    boolean result = underTest.hasMatchingLogFiles("ce").test(Path.of("ce.log"));

    assertThat(result).isTrue();
  }

  @Test
  public void getLogsForSingleNode_whenNoFiles_shouldReturnNull() throws IOException {
    File dir = temp.newFolder();
    settings.setProperty(PATH_LOGS.getKey(), dir.getAbsolutePath());

    File ce = underTest.getLogsForSingleNode("web");

    assertThat(ce).isNull();
  }

  @Test
  public void getDistributedLogs_shouldReturnException() {
    assertThatThrownBy(() -> underTest.getDistributedLogs("a", "b"))
      .isInstanceOf(UnsupportedOperationException.class)
      .hasMessage("This method should not be called on a standalone instance of SonarQube");
  }

  @Test
  public void isValidNodeToNodeCall_shouldReturnFalse() {
    assertThat(underTest.isValidNodeToNodeCall(Map.of("node_to_node_secret", "secret"))).isFalse();
  }

  @Test
  public void getWebAPIPortFromHazelcastQuery_shouldReturnPortByDefault() {
    underTest.start();

    assertThat(ServerLogging.getWebAPIPortFromHazelcastQuery()).isEqualTo(9000);
  }

  @Test
  public void getWebAPIPortFromHazelcastQuery_whenPortSpecified_shouldReturnPort() {
    underTest.start();

    settings.setProperty(ProcessProperties.Property.WEB_PORT.getKey(), "8000");
    assertThat(ServerLogging.getWebAPIPortFromHazelcastQuery()).isEqualTo(8000);
  }

  @Test
  public void getWebAPIAddressFromHazelcastQuery_whenSpecified_shouldReturnAddress() {
    underTest.start();
    settings.setProperty(ProcessProperties.Property.CLUSTER_NODE_HOST.getKey(), "anyhost");

    assertThat(ServerLogging.getWebAPIAddressFromHazelcastQuery()).isEqualTo("anyhost");
  }

  @Test
  public void getWebAPIAddressFromHazelcastQuery_whenSpecified_shouldReturnContext() {
    underTest.start();
    settings.setProperty(ProcessProperties.Property.WEB_CONTEXT.getKey(), "any_context");

    assertThat(ServerLogging.getWebAPIContextFromHazelcastQuery()).isEqualTo("any_context");
  }
}
