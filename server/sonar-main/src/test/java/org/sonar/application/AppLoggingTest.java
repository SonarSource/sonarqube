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
package org.sonar.application;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.joran.spi.ConsoleTarget;
import ch.qos.logback.core.rolling.RollingFileAppender;
import java.io.File;
import java.util.Iterator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;
import org.sonar.application.config.AppSettings;
import org.sonar.application.config.TestAppSettings;
import org.sonar.process.logging.LogbackHelper;
import org.sonar.process.logging.LogbackJsonLayout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.sonar.application.process.StreamGobbler.LOGGER_GOBBLER;
import static org.sonar.process.ProcessProperties.Property.CLUSTER_ENABLED;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;

public class AppLoggingTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private File logDir;

  private AppSettings settings = new TestAppSettings();
  private AppLogging underTest = new AppLogging(settings);

  @Before
  public void setUp() throws Exception {
    logDir = temp.newFolder();
    settings.getProps().set(PATH_LOGS.getKey(), logDir.getAbsolutePath());
  }

  @AfterClass
  public static void resetLogback() throws Exception {
    new LogbackHelper().resetFromXml("/logback-test.xml");
  }

  @Test
  public void no_writing_to_sonar_log_file_when_running_from_sonar_script() {
    emulateRunFromSonarScript();

    LoggerContext ctx = underTest.configure();

    ctx.getLoggerList().forEach(AppLoggingTest::verifyNoFileAppender);
  }

  @Test
  public void root_logger_only_writes_to_console_with_formatting_when_running_from_sonar_script() {
    emulateRunFromSonarScript();

    LoggerContext ctx = underTest.configure();

    Logger rootLogger = ctx.getLogger(ROOT_LOGGER_NAME);
    ConsoleAppender<ILoggingEvent> consoleAppender = (ConsoleAppender<ILoggingEvent>) rootLogger.getAppender("APP_CONSOLE");
    verifyAppFormattedLogEncoder(consoleAppender.getEncoder());
    assertThat(rootLogger.iteratorForAppenders()).toIterable().hasSize(1);
  }

  @Test
  public void gobbler_logger_writes_to_console_without_formatting_when_running_from_sonar_script() {
    emulateRunFromSonarScript();

    LoggerContext ctx = underTest.configure();

    Logger gobblerLogger = ctx.getLogger(LOGGER_GOBBLER);
    verifyGobblerConsoleAppender(gobblerLogger);
    assertThat(gobblerLogger.iteratorForAppenders()).toIterable().hasSize(1);
  }

  @Test
  public void root_logger_writes_to_console_with_formatting_and_to_sonar_log_file_when_running_from_command_line() {
    emulateRunFromCommandLine(false);

    LoggerContext ctx = underTest.configure();

    Logger rootLogger = ctx.getLogger(ROOT_LOGGER_NAME);
    verifyAppConsoleAppender(rootLogger.getAppender("APP_CONSOLE"));
    verifySonarLogFileAppender(rootLogger.getAppender("file_sonar"));
    assertThat(rootLogger.iteratorForAppenders()).toIterable().hasSize(2);

    // verify no other logger writes to sonar.log
    ctx.getLoggerList()
      .stream()
      .filter(logger -> !ROOT_LOGGER_NAME.equals(logger.getName()))
      .forEach(AppLoggingTest::verifyNoFileAppender);
  }

  @Test
  public void gobbler_logger_writes_to_console_without_formatting_when_running_from_command_line() {
    emulateRunFromCommandLine(false);

    LoggerContext ctx = underTest.configure();

    Logger gobblerLogger = ctx.getLogger(LOGGER_GOBBLER);
    verifyGobblerConsoleAppender(gobblerLogger);
    assertThat(gobblerLogger.iteratorForAppenders()).toIterable().hasSize(1);
  }

  @Test
  public void root_logger_writes_to_console_with_formatting_and_to_sonar_log_file_when_running_from_ITs() {
    emulateRunFromCommandLine(true);

    LoggerContext ctx = underTest.configure();

    Logger rootLogger = ctx.getLogger(ROOT_LOGGER_NAME);
    verifyAppConsoleAppender(rootLogger.getAppender("APP_CONSOLE"));
    verifySonarLogFileAppender(rootLogger.getAppender("file_sonar"));
    assertThat(rootLogger.iteratorForAppenders()).toIterable().hasSize(2);

    ctx.getLoggerList()
      .stream()
      .filter(logger -> !ROOT_LOGGER_NAME.equals(logger.getName()))
      .forEach(AppLoggingTest::verifyNoFileAppender);
  }

  @Test
  public void gobbler_logger_writes_to_console_without_formatting_when_running_from_ITs() {
    emulateRunFromCommandLine(true);

    LoggerContext ctx = underTest.configure();

    Logger gobblerLogger = ctx.getLogger(LOGGER_GOBBLER);
    verifyGobblerConsoleAppender(gobblerLogger);
    assertThat(gobblerLogger.iteratorForAppenders()).toIterable().hasSize(1);
  }

  @Test
  public void configure_no_rotation_on_sonar_file() {
    settings.getProps().set("sonar.log.rollingPolicy", "none");

    LoggerContext ctx = underTest.configure();

    Logger rootLogger = ctx.getLogger(ROOT_LOGGER_NAME);
    Appender<ILoggingEvent> appender = rootLogger.getAppender("file_sonar");
    assertThat(appender)
      .isNotInstanceOf(RollingFileAppender.class)
      .isInstanceOf(FileAppender.class);
  }

  @Test
  public void default_level_for_root_logger_is_INFO() {
    LoggerContext ctx = underTest.configure();

    verifyRootLogLevel(ctx, Level.INFO);
  }

  @Test
  public void root_logger_level_changes_with_global_property() {
    settings.getProps().set("sonar.log.level", "TRACE");

    LoggerContext ctx = underTest.configure();

    verifyRootLogLevel(ctx, Level.TRACE);
  }

  @Test
  public void root_logger_level_changes_with_app_property() {
    settings.getProps().set("sonar.log.level.app", "TRACE");

    LoggerContext ctx = underTest.configure();

    verifyRootLogLevel(ctx, Level.TRACE);
  }

  @Test
  public void root_logger_level_is_configured_from_app_property_over_global_property() {
    settings.getProps().set("sonar.log.level", "TRACE");
    settings.getProps().set("sonar.log.level.app", "DEBUG");

    LoggerContext ctx = underTest.configure();

    verifyRootLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void root_logger_level_changes_with_app_property_and_is_case_insensitive() {
    settings.getProps().set("sonar.log.level.app", "debug");

    LoggerContext ctx = underTest.configure();

    verifyRootLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void default_to_INFO_if_app_property_has_invalid_value() {
    settings.getProps().set("sonar.log.level.app", "DodoDouh!");

    LoggerContext ctx = underTest.configure();
    verifyRootLogLevel(ctx, Level.INFO);
  }

  @Test
  public void fail_with_IAE_if_global_property_unsupported_level() {
    settings.getProps().set("sonar.log.level", "ERROR");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("log level ERROR in property sonar.log.level is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.configure();
  }

  @Test
  public void fail_with_IAE_if_app_property_unsupported_level() {
    settings.getProps().set("sonar.log.level.app", "ERROR");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("log level ERROR in property sonar.log.level.app is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.configure();
  }

  @Test
  public void no_info_log_from_hazelcast() {
    settings.getProps().set(CLUSTER_ENABLED.getKey(), "true");
    underTest.configure();

    assertThat(
      LoggerFactory.getLogger("com.hazelcast").isInfoEnabled()).isEqualTo(false);
  }

  @Test
  public void use_json_output() {
    settings.getProps().set("sonar.log.useJsonOutput", "true");

    LoggerContext ctx = underTest.configure();
    Logger rootLogger = ctx.getLogger(ROOT_LOGGER_NAME);
    ConsoleAppender appender = (ConsoleAppender<ILoggingEvent>)rootLogger.getAppender("APP_CONSOLE");
    Encoder<ILoggingEvent> encoder = appender.getEncoder();
    assertThat(encoder).isInstanceOf(LayoutWrappingEncoder.class);
    assertThat(((LayoutWrappingEncoder)encoder).getLayout()).isInstanceOf(LogbackJsonLayout.class);
  }

  private void emulateRunFromSonarScript() {
    settings.getProps().set("sonar.wrapped", "true");
  }

  private void emulateRunFromCommandLine(boolean withAllLogsPrintedToConsole) {
    if (withAllLogsPrintedToConsole) {
      settings.getProps().set("sonar.log.console", "true");
    }
  }

  private static void verifyNoFileAppender(Logger logger) {
    Iterator<Appender<ILoggingEvent>> iterator = logger.iteratorForAppenders();
    while (iterator.hasNext()) {
      assertThat(iterator.next()).isNotInstanceOf(FileAppender.class);
    }
  }

  private void verifySonarLogFileAppender(Appender<ILoggingEvent> appender) {
    assertThat(appender).isInstanceOf(FileAppender.class);
    FileAppender fileAppender = (FileAppender) appender;
    assertThat(fileAppender.getFile()).isEqualTo(new File(logDir, "sonar.log").getAbsolutePath());
    verifyAppFormattedLogEncoder(fileAppender.getEncoder());
  }

  private void verifyAppConsoleAppender(Appender<ILoggingEvent> appender) {
    assertThat(appender).isInstanceOf(ConsoleAppender.class);
    ConsoleAppender<ILoggingEvent> consoleAppender = (ConsoleAppender<ILoggingEvent>) appender;
    assertThat(consoleAppender.getTarget()).isEqualTo(ConsoleTarget.SystemOut.getName());
    verifyAppFormattedLogEncoder(consoleAppender.getEncoder());
  }

  private void verifyAppFormattedLogEncoder(Encoder<ILoggingEvent> encoder) {
    verifyFormattedLogEncoder(encoder, "%d{yyyy.MM.dd HH:mm:ss} %-5level app[][%logger{20}] %msg%n");
  }

  private void verifyGobblerConsoleAppender(Logger logger) {
    Appender<ILoggingEvent> appender = logger.getAppender("GOBBLER_CONSOLE");
    assertThat(appender).isInstanceOf(ConsoleAppender.class);
    ConsoleAppender<ILoggingEvent> consoleAppender = (ConsoleAppender<ILoggingEvent>) appender;
    assertThat(consoleAppender.getTarget()).isEqualTo(ConsoleTarget.SystemOut.getName());
    verifyFormattedLogEncoder(consoleAppender.getEncoder(), "%msg%n");
  }

  private void verifyFormattedLogEncoder(Encoder<ILoggingEvent> encoder, String logPattern) {
    assertThat(encoder).isInstanceOf(PatternLayoutEncoder.class);
    PatternLayoutEncoder patternEncoder = (PatternLayoutEncoder) encoder;
    assertThat(patternEncoder.getPattern()).isEqualTo(logPattern);
  }

  private void verifyRootLogLevel(LoggerContext ctx, Level expected) {
    Logger rootLogger = ctx.getLogger(ROOT_LOGGER_NAME);
    assertThat(rootLogger.getLevel()).isEqualTo(expected);
  }
}
