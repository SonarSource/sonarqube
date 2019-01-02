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
package org.sonar.ce.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.Props;
import org.sonar.process.logging.LogbackHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;

public class CeProcessLoggingTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private File logDir;
  private Props props = new Props(new Properties());
  private CeProcessLogging underTest = new CeProcessLogging();

  @Before
  public void setUp() throws IOException {
    logDir = temp.newFolder();
    props.set(PATH_LOGS.getKey(), logDir.getAbsolutePath());
  }

  @AfterClass
  public static void resetLogback() throws JoranException {
    new LogbackHelper().resetFromXml("/logback-test.xml");
  }

  @Test
  public void do_not_log_to_console() {
    LoggerContext ctx = underTest.configure(props);

    Logger root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
    Appender appender = root.getAppender("CONSOLE");
    assertThat(appender).isNull();
  }

  @Test
  public void startup_logger_prints_to_only_to_system_out() {
    LoggerContext ctx = underTest.configure(props);

    Logger startup = ctx.getLogger("startup");
    assertThat(startup.isAdditive()).isFalse();
    Appender appender = startup.getAppender("CONSOLE");
    assertThat(appender).isInstanceOf(ConsoleAppender.class);
    ConsoleAppender<ILoggingEvent> consoleAppender = (ConsoleAppender<ILoggingEvent>) appender;
    assertThat(consoleAppender.getTarget()).isEqualTo("System.out");
    assertThat(consoleAppender.getEncoder()).isInstanceOf(PatternLayoutEncoder.class);
    PatternLayoutEncoder patternEncoder = (PatternLayoutEncoder) consoleAppender.getEncoder();
    assertThat(patternEncoder.getPattern()).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level app[][%logger{20}] %msg%n");
  }

  @Test
  public void log_to_ce_file() {
    LoggerContext ctx = underTest.configure(props);

    Logger root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
    Appender<ILoggingEvent> appender = root.getAppender("file_ce");
    assertThat(appender).isInstanceOf(FileAppender.class);
    FileAppender fileAppender = (FileAppender) appender;
    assertThat(fileAppender.getFile()).isEqualTo(new File(logDir, "ce.log").getAbsolutePath());
    assertThat(fileAppender.getEncoder()).isInstanceOf(PatternLayoutEncoder.class);
    PatternLayoutEncoder encoder = (PatternLayoutEncoder) fileAppender.getEncoder();
    assertThat(encoder.getPattern()).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level ce[%X{ceTaskUuid}][%logger{20}] %msg%n");
  }

  @Test
  public void default_level_for_root_logger_is_INFO() {
    LoggerContext ctx = underTest.configure(props);

    verifyRootLogLevel(ctx, Level.INFO);
  }

  @Test
  public void root_logger_level_changes_with_global_property() {
    props.set("sonar.log.level", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyRootLogLevel(ctx, Level.TRACE);
  }

  @Test
  public void root_logger_level_changes_with_ce_property() {
    props.set("sonar.log.level.ce", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyRootLogLevel(ctx, Level.TRACE);
  }

  @Test
  public void root_logger_level_is_configured_from_ce_property_over_global_property() {
    props.set("sonar.log.level", "TRACE");
    props.set("sonar.log.level.ce", "DEBUG");

    LoggerContext ctx = underTest.configure(props);

    verifyRootLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void root_logger_level_changes_with_ce_property_and_is_case_insensitive() {
    props.set("sonar.log.level.ce", "debug");

    LoggerContext ctx = underTest.configure(props);

    verifyRootLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void sql_logger_level_changes_with_global_property_and_is_case_insensitive() {
    props.set("sonar.log.level", "InFO");

    LoggerContext ctx = underTest.configure(props);

    verifySqlLogLevel(ctx, Level.INFO);
  }

  @Test
  public void sql_logger_level_changes_with_ce_property_and_is_case_insensitive() {
    props.set("sonar.log.level.ce", "TrACe");

    LoggerContext ctx = underTest.configure(props);

    verifySqlLogLevel(ctx, Level.TRACE);
  }

  @Test
  public void sql_logger_level_changes_with_ce_sql_property_and_is_case_insensitive() {
    props.set("sonar.log.level.ce.sql", "debug");

    LoggerContext ctx = underTest.configure(props);

    verifySqlLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void sql_logger_level_is_configured_from_ce_sql_property_over_ce_property() {
    props.set("sonar.log.level.ce.sql", "debug");
    props.set("sonar.log.level.ce", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifySqlLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void sql_logger_level_is_configured_from_ce_sql_property_over_global_property() {
    props.set("sonar.log.level.ce.sql", "debug");
    props.set("sonar.log.level", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifySqlLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void sql_logger_level_is_configured_from_ce_property_over_global_property() {
    props.set("sonar.log.level.ce", "debug");
    props.set("sonar.log.level", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifySqlLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void es_logger_level_changes_with_global_property_and_is_case_insensitive() {
    props.set("sonar.log.level", "InFO");

    LoggerContext ctx = underTest.configure(props);

    verifyEsLogLevel(ctx, Level.INFO);
  }

  @Test
  public void es_logger_level_changes_with_ce_property_and_is_case_insensitive() {
    props.set("sonar.log.level.ce", "TrACe");

    LoggerContext ctx = underTest.configure(props);

    verifyEsLogLevel(ctx, Level.TRACE);
  }

  @Test
  public void es_logger_level_changes_with_ce_es_property_and_is_case_insensitive() {
    props.set("sonar.log.level.ce.es", "debug");

    LoggerContext ctx = underTest.configure(props);

    verifyEsLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void es_logger_level_is_configured_from_ce_es_property_over_ce_property() {
    props.set("sonar.log.level.ce.es", "debug");
    props.set("sonar.log.level.ce", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyEsLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void es_logger_level_is_configured_from_ce_es_property_over_global_property() {
    props.set("sonar.log.level.ce.es", "debug");
    props.set("sonar.log.level", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyEsLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void es_logger_level_is_configured_from_ce_property_over_global_property() {
    props.set("sonar.log.level.ce", "debug");
    props.set("sonar.log.level", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyEsLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void jmx_logger_level_changes_with_global_property_and_is_case_insensitive() {
    props.set("sonar.log.level", "InFO");

    LoggerContext ctx = underTest.configure(props);

    verifyJmxLogLevel(ctx, Level.INFO);
  }

  @Test
  public void jmx_logger_level_changes_with_jmx_property_and_is_case_insensitive() {
    props.set("sonar.log.level.ce", "TrACe");

    LoggerContext ctx = underTest.configure(props);

    verifyJmxLogLevel(ctx, Level.TRACE);
  }

  @Test
  public void jmx_logger_level_changes_with_ce_jmx_property_and_is_case_insensitive() {
    props.set("sonar.log.level.ce.jmx", "debug");

    LoggerContext ctx = underTest.configure(props);

    verifyJmxLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void jmx_logger_level_is_configured_from_ce_jmx_property_over_ce_property() {
    props.set("sonar.log.level.ce.jmx", "debug");
    props.set("sonar.log.level.ce", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyJmxLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void jmx_logger_level_is_configured_from_ce_jmx_property_over_global_property() {
    props.set("sonar.log.level.ce.jmx", "debug");
    props.set("sonar.log.level", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyJmxLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void jmx_logger_level_is_configured_from_ce_property_over_global_property() {
    props.set("sonar.log.level.ce", "debug");
    props.set("sonar.log.level", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyJmxLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void root_logger_level_defaults_to_INFO_if_ce_property_has_invalid_value() {
    props.set("sonar.log.level.ce", "DodoDouh!");

    LoggerContext ctx = underTest.configure(props);
    verifyRootLogLevel(ctx, Level.INFO);
  }

  @Test
  public void sql_logger_level_defaults_to_INFO_if_ce_sql_property_has_invalid_value() {
    props.set("sonar.log.level.ce.sql", "DodoDouh!");

    LoggerContext ctx = underTest.configure(props);
    verifySqlLogLevel(ctx, Level.INFO);
  }

  @Test
  public void es_logger_level_defaults_to_INFO_if_ce_es_property_has_invalid_value() {
    props.set("sonar.log.level.ce.es", "DodoDouh!");

    LoggerContext ctx = underTest.configure(props);
    verifyEsLogLevel(ctx, Level.INFO);
  }

  @Test
  public void jmx_loggers_level_defaults_to_INFO_if_ce_jmx_property_has_invalid_value() {
    props.set("sonar.log.level.ce.jmx", "DodoDouh!");

    LoggerContext ctx = underTest.configure(props);
    verifyJmxLogLevel(ctx, Level.INFO);
  }

  @Test
  public void fail_with_IAE_if_global_property_unsupported_level() {
    props.set("sonar.log.level", "ERROR");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("log level ERROR in property sonar.log.level is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.configure(props);
  }

  @Test
  public void fail_with_IAE_if_ce_property_unsupported_level() {
    props.set("sonar.log.level.ce", "ERROR");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("log level ERROR in property sonar.log.level.ce is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.configure(props);
  }

  @Test
  public void fail_with_IAE_if_ce_sql_property_unsupported_level() {
    props.set("sonar.log.level.ce.sql", "ERROR");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("log level ERROR in property sonar.log.level.ce.sql is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.configure(props);
  }

  @Test
  public void fail_with_IAE_if_ce_es_property_unsupported_level() {
    props.set("sonar.log.level.ce.es", "ERROR");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("log level ERROR in property sonar.log.level.ce.es is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.configure(props);
  }

  @Test
  public void fail_with_IAE_if_ce_jmx_property_unsupported_level() {
    props.set("sonar.log.level.ce.jmx", "ERROR");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("log level ERROR in property sonar.log.level.ce.jmx is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.configure(props);
  }

  @Test
  public void configure_defines_hardcoded_levels() {
    LoggerContext context = underTest.configure(props);

    verifyImmutableLogLevels(context);
  }

  @Test
  public void configure_defines_hardcoded_levels_unchanged_by_global_property() {
    props.set("sonar.log.level", "TRACE");

    LoggerContext context = underTest.configure(props);

    verifyImmutableLogLevels(context);
  }

  @Test
  public void configure_defines_hardcoded_levels_unchanged_by_ce_property() {
    props.set("sonar.log.level.ce", "TRACE");

    LoggerContext context = underTest.configure(props);

    verifyImmutableLogLevels(context);
  }

  @Test
  public void configure_turns_off_some_MsSQL_driver_logger() {
    LoggerContext context = underTest.configure(props);

    Stream.of("com.microsoft.sqlserver.jdbc.internals",
      "com.microsoft.sqlserver.jdbc.ResultSet",
      "com.microsoft.sqlserver.jdbc.Statement",
      "com.microsoft.sqlserver.jdbc.Connection")
      .forEach(loggerName -> assertThat(context.getLogger(loggerName).getLevel()).isEqualTo(Level.OFF));
  }

  private void verifyRootLogLevel(LoggerContext ctx, Level expected) {
    assertThat(ctx.getLogger(ROOT_LOGGER_NAME).getLevel()).isEqualTo(expected);
  }

  private void verifySqlLogLevel(LoggerContext ctx, Level expected) {
    assertThat(ctx.getLogger("sql").getLevel()).isEqualTo(expected);
  }

  private void verifyEsLogLevel(LoggerContext ctx, Level expected) {
    assertThat(ctx.getLogger("es").getLevel()).isEqualTo(expected);
  }

  private void verifyJmxLogLevel(LoggerContext ctx, Level expected) {
    assertThat(ctx.getLogger("javax.management.remote.timeout").getLevel()).isEqualTo(expected);
    assertThat(ctx.getLogger("javax.management.remote.misc").getLevel()).isEqualTo(expected);
    assertThat(ctx.getLogger("javax.management.remote.rmi").getLevel()).isEqualTo(expected);
    assertThat(ctx.getLogger("javax.management.mbeanserver").getLevel()).isEqualTo(expected);
    assertThat(ctx.getLogger("sun.rmi.loader").getLevel()).isEqualTo(expected);
    assertThat(ctx.getLogger("sun.rmi.transport.tcp").getLevel()).isEqualTo(expected);
    assertThat(ctx.getLogger("sun.rmi.transport.misc").getLevel()).isEqualTo(expected);
    assertThat(ctx.getLogger("sun.rmi.server.call").getLevel()).isEqualTo(expected);
    assertThat(ctx.getLogger("sun.rmi.dgc").getLevel()).isEqualTo(expected);
  }

  private void verifyImmutableLogLevels(LoggerContext ctx) {
    assertThat(ctx.getLogger("org.apache.ibatis").getLevel()).isEqualTo(Level.WARN);
    assertThat(ctx.getLogger("java.sql").getLevel()).isEqualTo(Level.WARN);
    assertThat(ctx.getLogger("java.sql.ResultSet").getLevel()).isEqualTo(Level.WARN);
    assertThat(ctx.getLogger("org.elasticsearch").getLevel()).isEqualTo(Level.INFO);
    assertThat(ctx.getLogger("org.elasticsearch.node").getLevel()).isEqualTo(Level.INFO);
    assertThat(ctx.getLogger("org.elasticsearch.http").getLevel()).isEqualTo(Level.INFO);
    assertThat(ctx.getLogger("ch.qos.logback").getLevel()).isEqualTo(Level.WARN);
    assertThat(ctx.getLogger("org.apache.catalina").getLevel()).isEqualTo(Level.INFO);
    assertThat(ctx.getLogger("org.apache.coyote").getLevel()).isEqualTo(Level.INFO);
    assertThat(ctx.getLogger("org.apache.jasper").getLevel()).isEqualTo(Level.INFO);
    assertThat(ctx.getLogger("org.apache.tomcat").getLevel()).isEqualTo(Level.INFO);
  }
}
