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
package org.sonar.server.app;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.joran.spi.JoranException;
import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.process.Props;
import org.sonar.process.logging.LogbackHelper;
import org.sonar.process.logging.LogbackJsonLayout;
import org.sonar.process.logging.PatternLayoutEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;

@RunWith(DataProviderRunner.class)
public class WebServerProcessLoggingTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File logDir;
  private final Props props = new Props(new Properties());
  private final WebServerProcessLogging underTest = new WebServerProcessLogging();

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
  public void check_level_of_jul() throws IOException {
    Props props = new Props(new Properties());
    File dir = temp.newFolder();
    props.set(PATH_LOGS.getKey(), dir.getAbsolutePath());
    props.set("sonar.log.level.web", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    MemoryAppender memoryAppender = new MemoryAppender();
    memoryAppender.start();
    ctx.getLogger(ROOT_LOGGER_NAME).addAppender(memoryAppender);

    java.util.logging.Logger logger = java.util.logging.Logger.getLogger("com.ms.sqlserver.jdbc.DTV");
    logger.finest("Test");
    memoryAppender.stop();
    assertThat(memoryAppender.getLogs())
      .filteredOn(ILoggingEvent::getLoggerName, "com.ms.sqlserver.jdbc.DTV")
      .extracting(ILoggingEvent::getLevel, ILoggingEvent::getMessage)
      .containsOnly(new Tuple(Level.TRACE, "Test"));
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
  public void log_to_web_file() {
    LoggerContext ctx = underTest.configure(props);

    Logger root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
    Appender<ILoggingEvent> appender = root.getAppender("file_web");
    assertThat(appender).isInstanceOf(FileAppender.class);
    FileAppender fileAppender = (FileAppender) appender;
    assertThat(fileAppender.getFile()).isEqualTo(new File(logDir, "web.log").getAbsolutePath());
    assertThat(fileAppender.getEncoder()).isInstanceOf(PatternLayoutEncoder.class);
    PatternLayoutEncoder encoder = (PatternLayoutEncoder) fileAppender.getEncoder();
    assertThat(encoder.getPattern()).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level web[%X{HTTP_REQUEST_ID}][%logger{20}] %msg%n");
  }

  @Test
  public void log_for_cluster_changes_layout_in_file_and_console() {
    props.set("sonar.cluster.enabled", "true");
    props.set("sonar.cluster.node.name", "my-node");
    LoggerContext ctx = underTest.configure(props);

    Logger root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
    FileAppender fileAppender = (FileAppender) root.getAppender("file_web");
    PatternLayoutEncoder encoder = (PatternLayoutEncoder) fileAppender.getEncoder();

    assertThat(encoder.getPattern()).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level my-node web[%X{HTTP_REQUEST_ID}][%logger{20}] %msg%n");

    Logger startup = ctx.getLogger("startup");
    ConsoleAppender<ILoggingEvent> consoleAppender = (ConsoleAppender<ILoggingEvent>) startup.getAppender("CONSOLE");
    PatternLayoutEncoder patternEncoder = (PatternLayoutEncoder) consoleAppender.getEncoder();
    assertThat(patternEncoder.getPattern()).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level my-node app[][%logger{20}] %msg%n");
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
  public void root_logger_level_changes_with_web_property() {
    props.set("sonar.log.level.web", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyRootLogLevel(ctx, Level.TRACE);
  }

  @Test
  public void root_logger_level_is_configured_from_web_property_over_global_property() {
    props.set("sonar.log.level", "TRACE");
    props.set("sonar.log.level.web", "DEBUG");

    LoggerContext ctx = underTest.configure(props);

    verifyRootLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void root_logger_level_changes_with_web_property_and_is_case_insensitive() {
    props.set("sonar.log.level.web", "debug");

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
  public void sql_logger_level_changes_with_web_property_and_is_case_insensitive() {
    props.set("sonar.log.level.web", "TrACe");

    LoggerContext ctx = underTest.configure(props);

    verifySqlLogLevel(ctx, Level.TRACE);
  }

  @Test
  public void sql_logger_level_changes_with_web_sql_property_and_is_case_insensitive() {
    props.set("sonar.log.level.web.sql", "debug");

    LoggerContext ctx = underTest.configure(props);

    verifySqlLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void sql_logger_level_is_configured_from_web_sql_property_over_web_property() {
    props.set("sonar.log.level.web.sql", "debug");
    props.set("sonar.log.level.web", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifySqlLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void sql_logger_level_is_configured_from_web_sql_property_over_global_property() {
    props.set("sonar.log.level.web.sql", "debug");
    props.set("sonar.log.level", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifySqlLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void sql_logger_level_is_configured_from_web_property_over_global_property() {
    props.set("sonar.log.level.web", "debug");
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
  public void es_logger_level_changes_with_web_property_and_is_case_insensitive() {
    props.set("sonar.log.level.web", "TrACe");

    LoggerContext ctx = underTest.configure(props);

    verifyEsLogLevel(ctx, Level.TRACE);
  }

  @Test
  public void es_logger_level_changes_with_web_es_property_and_is_case_insensitive() {
    props.set("sonar.log.level.web.es", "debug");

    LoggerContext ctx = underTest.configure(props);

    verifyEsLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void es_logger_level_is_configured_from_web_es_property_over_web_property() {
    props.set("sonar.log.level.web.es", "debug");
    props.set("sonar.log.level.web", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyEsLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void es_logger_level_is_configured_from_web_es_property_over_global_property() {
    props.set("sonar.log.level.web.es", "debug");
    props.set("sonar.log.level", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyEsLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void es_logger_level_is_configured_from_web_property_over_global_property() {
    props.set("sonar.log.level.web", "debug");
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
    props.set("sonar.log.level.web", "TrACe");

    LoggerContext ctx = underTest.configure(props);

    verifyJmxLogLevel(ctx, Level.TRACE);
  }

  @Test
  public void jmx_logger_level_changes_with_web_jmx_property_and_is_case_insensitive() {
    props.set("sonar.log.level.web.jmx", "debug");

    LoggerContext ctx = underTest.configure(props);

    verifyJmxLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void jmx_logger_level_is_configured_from_web_jmx_property_over_web_property() {
    props.set("sonar.log.level.web.jmx", "debug");
    props.set("sonar.log.level.web", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyJmxLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void jmx_logger_level_is_configured_from_web_jmx_property_over_global_property() {
    props.set("sonar.log.level.web.jmx", "debug");
    props.set("sonar.log.level", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyJmxLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void jmx_logger_level_is_configured_from_web_property_over_global_property() {
    props.set("sonar.log.level.web", "debug");
    props.set("sonar.log.level", "TRACE");

    LoggerContext ctx = underTest.configure(props);

    verifyJmxLogLevel(ctx, Level.DEBUG);
  }

  @Test
  public void root_logger_level_defaults_to_INFO_if_web_property_has_invalid_value() {
    props.set("sonar.log.level.web", "DodoDouh!");

    LoggerContext ctx = underTest.configure(props);
    verifyRootLogLevel(ctx, Level.INFO);
  }

  @Test
  public void sql_logger_level_defaults_to_INFO_if_web_sql_property_has_invalid_value() {
    props.set("sonar.log.level.web.sql", "DodoDouh!");

    LoggerContext ctx = underTest.configure(props);
    verifySqlLogLevel(ctx, Level.INFO);
  }

  @Test
  public void es_logger_level_defaults_to_INFO_if_web_es_property_has_invalid_value() {
    props.set("sonar.log.level.web.es", "DodoDouh!");

    LoggerContext ctx = underTest.configure(props);
    verifyEsLogLevel(ctx, Level.INFO);
  }

  @Test
  public void jmx_loggers_level_defaults_to_INFO_if_wedb_jmx_property_has_invalid_value() {
    props.set("sonar.log.level.web.jmx", "DodoDouh!");

    LoggerContext ctx = underTest.configure(props);
    verifyJmxLogLevel(ctx, Level.INFO);
  }

  @Test
  public void fail_with_IAE_if_global_property_unsupported_level() {
    props.set("sonar.log.level", "ERROR");

    assertThatThrownBy(() -> underTest.configure(props))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("log level ERROR in property sonar.log.level is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");
  }

  @Test
  public void fail_with_IAE_if_web_property_unsupported_level() {
    props.set("sonar.log.level.web", "ERROR");

    assertThatThrownBy(() -> underTest.configure(props))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("log level ERROR in property sonar.log.level.web is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");
  }

  @Test
  public void fail_with_IAE_if_web_sql_property_unsupported_level() {
    props.set("sonar.log.level.web.sql", "ERROR");

    assertThatThrownBy(() -> underTest.configure(props))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("log level ERROR in property sonar.log.level.web.sql is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");
  }

  @Test
  public void fail_with_IAE_if_web_es_property_unsupported_level() {
    props.set("sonar.log.level.web.es", "ERROR");

    assertThatThrownBy(() -> underTest.configure(props))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("log level ERROR in property sonar.log.level.web.es is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");
  }

  @Test
  public void fail_with_IAE_if_web_jmx_property_unsupported_level() {
    props.set("sonar.log.level.web.jmx", "ERROR");

    assertThatThrownBy(() -> underTest.configure(props))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("log level ERROR in property sonar.log.level.web.jmx is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");
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
  public void configure_turns_off_some_Tomcat_loggers_if_global_log_level_is_not_set() {
    LoggerContext context = underTest.configure(props);

    verifyTomcatLoggersLogLevelsOff(context);
  }

  @Test
  public void configure_turns_off_some_Tomcat_loggers_if_global_log_level_is_INFO() {
    props.set("sonar.log.level", "INFO");

    LoggerContext context = underTest.configure(props);

    verifyTomcatLoggersLogLevelsOff(context);
  }

  @Test
  public void configure_turns_off_some_Tomcat_loggers_if_global_log_level_is_DEBUG() {
    props.set("sonar.log.level", "DEBUG");

    LoggerContext context = underTest.configure(props);

    verifyTomcatLoggersLogLevelsOff(context);
  }

  @Test
  public void configure_turns_off_some_Tomcat_loggers_if_global_log_level_is_TRACE() {
    props.set("sonar.log.level", "TRACE");

    LoggerContext context = underTest.configure(props);

    assertThat(context.getLogger("org.apache.catalina.core.ContainerBase").getLevel()).isNull();
    assertThat(context.getLogger("org.apache.catalina.core.StandardContext").getLevel()).isNull();
    assertThat(context.getLogger("org.apache.catalina.core.StandardService").getLevel()).isNull();
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

  @Test
  public void use_json_output() {
    props.set("sonar.log.jsonOutput", "true");

    LoggerContext context = underTest.configure(props);

    Logger rootLogger = context.getLogger(ROOT_LOGGER_NAME);
    OutputStreamAppender appender = (OutputStreamAppender) rootLogger.getAppender("file_web");
    Encoder<ILoggingEvent> encoder = appender.getEncoder();
    assertThat(encoder).isInstanceOf(LayoutWrappingEncoder.class);
    assertThat(((LayoutWrappingEncoder) encoder).getLayout()).isInstanceOf(LogbackJsonLayout.class);
  }

  @DataProvider
  public static Object[][] configuration() {
    return new Object[][] {
      {Map.of("sonar.deprecationLogs.loginEnabled", "true"), "%d{yyyy.MM.dd HH:mm:ss} %-5level web[%X{HTTP_REQUEST_ID}] %X{LOGIN} %X{ENTRYPOINT} %msg%n"},
      {Map.of("sonar.deprecationLogs.loginEnabled", "false"), "%d{yyyy.MM.dd HH:mm:ss} %-5level web[%X{HTTP_REQUEST_ID}] %X{ENTRYPOINT} %msg%n"},
      {Map.of(), "%d{yyyy.MM.dd HH:mm:ss} %-5level web[%X{HTTP_REQUEST_ID}] %X{ENTRYPOINT} %msg%n"},
    };
  }

  @Test
  @UseDataProvider("configuration")
  public void configure_whenJsonPropFalse_shouldConfigureDeprecatedLoggerWithPatternLayout(Map<String, String> additionalProps, String expectedPattern) {
    props.set("sonar.log.jsonOutput", "false");
    additionalProps.forEach(props::set);

    LoggerContext context = underTest.configure(props);

    Logger logger = context.getLogger("SONAR_DEPRECATION");
    assertThat(logger.isAdditive()).isFalse();
    Appender<ILoggingEvent> appender = logger.getAppender("file_deprecation");
    assertThat(appender).isNotNull()
      .isInstanceOf(FileAppender.class);
    FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) appender;
    Encoder<ILoggingEvent> encoder = fileAppender.getEncoder();
    assertThat(encoder).isInstanceOf(PatternLayoutEncoder.class);
    PatternLayoutEncoder patternLayoutEncoder = (PatternLayoutEncoder) encoder;
    assertThat(patternLayoutEncoder.getPattern()).isEqualTo(expectedPattern);
  }

  @Test
  public void configure_whenJsonPropTrue_shouldConfigureDeprecatedLoggerWithJsonLayout() {
    props.set("sonar.log.jsonOutput", "true");

    LoggerContext context = underTest.configure(props);

    Logger logger = context.getLogger("SONAR_DEPRECATION");
    assertThat(logger.isAdditive()).isFalse();
    Appender<ILoggingEvent> appender = logger.getAppender("file_deprecation");
    assertThat(appender).isNotNull()
      .isInstanceOf(FileAppender.class);
    FileAppender<ILoggingEvent> fileAppender = (FileAppender<ILoggingEvent>) appender;
    assertThat(fileAppender.getEncoder()).isInstanceOf(LayoutWrappingEncoder.class);
  }

  @Test
  public void configure_shouldConfigureDeprecatedLoggerWithConsoleAppender() {
    LoggerContext ctx = underTest.configure(props);

    Logger root = ctx.getLogger("SONAR_DEPRECATION");
    Appender<ILoggingEvent> appender = root.getAppender("CONSOLE");
    assertThat(appender).isNotNull();
  }

  private void verifyRootLogLevel(LoggerContext ctx, Level expected) {
    Logger rootLogger = ctx.getLogger(ROOT_LOGGER_NAME);
    assertThat(rootLogger.getLevel()).isEqualTo(expected);
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

  private void verifyTomcatLoggersLogLevelsOff(LoggerContext context) {
    assertThat(context.getLogger("org.apache.catalina.core.ContainerBase").getLevel()).isEqualTo(Level.OFF);
    assertThat(context.getLogger("org.apache.catalina.core.StandardContext").getLevel()).isEqualTo(Level.OFF);
    assertThat(context.getLogger("org.apache.catalina.core.StandardService").getLevel()).isEqualTo(Level.OFF);
  }

  public static class MemoryAppender extends AppenderBase<ILoggingEvent> {
    private static final List<ILoggingEvent> LOGS = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent eventObject) {
      LOGS.add(eventObject);
    }

    public List<ILoggingEvent> getLogs() {
      return ImmutableList.copyOf(LOGS);
    }

    public void clear() {
      LOGS.clear();
    }
  }
}
