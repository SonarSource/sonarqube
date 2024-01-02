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
package org.sonar.process.logging;

import ch.qos.logback.classic.Level;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.process.MessageException;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;

import static java.lang.String.valueOf;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertThrows;
import static org.sonar.process.logging.RootLoggerConfig.newRootLoggerConfigBuilder;

@RunWith(DataProviderRunner.class)
public class Log4JPropertiesBuilderTest {
  private static final String ROLLING_POLICY_PROPERTY = "sonar.log.rollingPolicy";
  private static final String PROPERTY_MAX_FILES = "sonar.log.maxFiles";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final RootLoggerConfig esRootLoggerConfig = newRootLoggerConfigBuilder().setProcessId(ProcessId.ELASTICSEARCH).build();

  @Test
  public void constructor_fails_with_NPE_if_Props_is_null() {
    assertThatThrownBy(() -> new Log4JPropertiesBuilder(null))
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Props can't be null");
  }

  @Test
  public void constructor_sets_status_to_ERROR() throws IOException {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);
    Properties properties = newLog4JPropertiesBuilder().rootLoggerConfig(esRootLoggerConfig).logDir(logDir).logPattern(logPattern).build();

    assertThat(properties.getProperty("status")).isEqualTo("ERROR");
  }

  @Test
  public void getRootLoggerName_returns_rootLogger() {
    assertThat(newLog4JPropertiesBuilder().getRootLoggerName()).isEqualTo("rootLogger");
  }

  @Test
  public void get_always_returns_a_new_object() throws IOException {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Properties previous = newLog4JPropertiesBuilder().rootLoggerConfig(esRootLoggerConfig).logDir(logDir).logPattern(logPattern).build();
    for (int i = 0; i < 2 + new Random().nextInt(5); i++) {
      Properties properties = newLog4JPropertiesBuilder().rootLoggerConfig(esRootLoggerConfig).logDir(logDir).logPattern(logPattern).build();
      assertThat(properties).isNotSameAs(previous);
      previous = properties;
    }
  }

  @Test
  public void buildLogPattern_puts_process_key_as_process_id() {
    String pattern = newLog4JPropertiesBuilder().buildLogPattern(newRootLoggerConfigBuilder()
      .setProcessId(ProcessId.ELASTICSEARCH)
      .build());

    assertThat(pattern).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level es[][%logger{1.}] %msg%n");
  }

  @Test
  public void buildLogPattern_puts_threadIdFieldPattern_from_RootLoggerConfig_non_null() {
    String threadIdFieldPattern = RandomStringUtils.randomAlphabetic(5);

    String pattern = newLog4JPropertiesBuilder().buildLogPattern(
      newRootLoggerConfigBuilder()
        .setProcessId(ProcessId.APP)
        .setThreadIdFieldPattern(threadIdFieldPattern)
        .build());

    assertThat(pattern).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level app[" + threadIdFieldPattern + "][%logger{1.}] %msg%n");
  }

  @Test
  public void buildLogPattern_does_not_put_threadIdFieldPattern_from_RootLoggerConfig_is_null() {
    String pattern = newLog4JPropertiesBuilder().buildLogPattern(
      newRootLoggerConfigBuilder()
        .setProcessId(ProcessId.COMPUTE_ENGINE)
        .build());

    assertThat(pattern).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level ce[][%logger{1.}] %msg%n");
  }

  @Test
  public void buildLogPattern_does_not_put_threadIdFieldPattern_from_RootLoggerConfig_is_empty() {
    String pattern = newLog4JPropertiesBuilder().buildLogPattern(
      newRootLoggerConfigBuilder()
        .setProcessId(ProcessId.WEB_SERVER)
        .setThreadIdFieldPattern("")
        .build());

    assertThat(pattern).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level web[][%logger{1.}] %msg%n");
  }

  @Test
  public void configureGlobalFileLog_sets_properties_for_daily_time_rolling_policy_with_max_7_files_for_empty_props() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);
    var underTest = newLog4JPropertiesBuilder()
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern);

    verifyTimeRollingPolicy(underTest, logDir, logPattern, "yyyy-MM-dd", 7);
  }

  @Test
  public void time_rolling_policy_has_large_max_files_if_property_is_zero() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = "foo";

    var underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "time:yyyy-MM-dd",
      PROPERTY_MAX_FILES, "0")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern);

    verifyTimeRollingPolicy(underTest, logDir, logPattern, "yyyy-MM-dd", 100_000);
  }

  @Test
  public void time_rolling_policy_has_large_max_files_if_property_is_negative() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = "foo";

    var underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "time:yyyy-MM-dd",
      PROPERTY_MAX_FILES, "-2")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern);

    verifyTimeRollingPolicy(underTest, logDir, logPattern, "yyyy-MM-dd", 100_000);
  }

  @Test
  public void size_rolling_policy_has_large_max_files_if_property_is_zero() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = "foo";
    String sizePattern = "1KB";

    var underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "size:" + sizePattern,
      PROPERTY_MAX_FILES, "0")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern);

    verifySizeRollingPolicy(underTest, logDir, logPattern, sizePattern, 100_000);
  }

  @Test
  public void size_rolling_policy_has_large_max_files_if_property_is_negative() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = "foo";
    String sizePattern = "1KB";

    var underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "size:" + sizePattern,
      PROPERTY_MAX_FILES, "-2")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern);

    verifySizeRollingPolicy(underTest, logDir, logPattern, sizePattern, 100_000);
  }

  @Test
  public void configureGlobalFileLog_throws_MessageException_when_property_is_not_supported() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);
    String invalidPropertyValue = randomAlphanumeric(3);
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, invalidPropertyValue)
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern);

    assertThatThrownBy(underTest::build)
      .isInstanceOf(MessageException.class)
      .hasMessage("Unsupported value for property " + ROLLING_POLICY_PROPERTY + ": " + invalidPropertyValue);
  }

  @Test
  public void configureGlobalFileLog_sets_properties_for_time_rolling_policy_with_max_7_files_when_property_starts_with_time_colon() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);
    String timePattern = randomAlphanumeric(6);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "time:" + timePattern)
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern);

    verifyTimeRollingPolicy(underTest, logDir, logPattern, timePattern, 7);
  }

  @Test
  public void configureGlobalFileLog_sets_properties_for_time_rolling_policy_when_property_starts_with_time_colon_and_specified_max_number_of_files() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);
    String timePattern = randomAlphanumeric(6);
    int maxFile = 1 + new Random().nextInt(10);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "time:" + timePattern,
      PROPERTY_MAX_FILES, valueOf(maxFile))
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern);

    verifyTimeRollingPolicy(underTest, logDir, logPattern, timePattern, maxFile);
  }

  @Test
  public void configureGlobalFileLog_sets_properties_for_size_rolling_policy_with_max_7_files_when_property_starts_with_size_colon() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);
    String sizePattern = randomAlphanumeric(6);
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "size:" + sizePattern)
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern);

    verifySizeRollingPolicy(underTest, logDir, logPattern, sizePattern, 7);
  }

  @Test
  public void configureGlobalFileLog_sets_properties_for_size_rolling_policy_when_property_starts_with_size_colon_and_specified_max_number_of_files() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);
    String sizePattern = randomAlphanumeric(6);
    int maxFile = 1 + new Random().nextInt(10);
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "size:" + sizePattern,
      PROPERTY_MAX_FILES, valueOf(maxFile))
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern);

    verifySizeRollingPolicy(underTest, logDir, logPattern, sizePattern, maxFile);
  }

  @Test
  public void configureGlobalFileLog_sets_properties_for_no_rolling_policy_when_property_is_none() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "none")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern);

    verifyProperties(underTest.build(),
      "appender.file_es.type", "File",
      "appender.file_es.name", "file_es",
      "appender.file_es.fileName", new File(logDir, "es.log").getAbsolutePath(),
      "appender.file_es.layout.type", "PatternLayout",
      "appender.file_es.layout.pattern", logPattern,
      "rootLogger.appenderRef.file_es.ref", "file_es");
  }

  @Test
  public void enable_all_logs_to_stdout_write_additionally_Console_appender() throws IOException {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(ROLLING_POLICY_PROPERTY, "none")
      .enableAllLogsToConsole(true)
      .rootLoggerConfig(esRootLoggerConfig)
      .logPattern(logPattern)
      .logDir(logDir);
    verifyProperties(underTest.build(),
      "appender.stdout.type", "Console",
      "appender.stdout.name", "stdout",
      "appender.stdout.layout.type", "PatternLayout",
      "appender.stdout.layout.pattern", logPattern,
      "rootLogger.appenderRef.stdout.ref", "stdout",
      "appender.file_es.layout.pattern", logPattern,
      "appender.file_es.layout.type", "PatternLayout",
      "appender.file_es.fileName", new File(logDir, "es.log").getAbsolutePath(),
      "appender.file_es.name", "file_es",
      "rootLogger.appenderRef.file_es.ref", "file_es",
      "appender.file_es.type", "File");
  }

  @Test
  public void enable_json_output_should_change_pattern_for_console_and_file_appender() throws IOException {
    File logDir = temporaryFolder.newFolder();

    String expectedPattern = "{\"process\": \"es\",\"timestamp\": \"%d{yyyy-MM-dd'T'HH:mm:ss.SSSZZ}\","
      + "\"severity\": \"%p\",\"logger\": \"%c{1.}\",\"message\": \"%notEmpty{%enc{%marker}{JSON} }%enc{%.-10000m}{JSON}\"%exceptionAsJson }" + System.lineSeparator();

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(ROLLING_POLICY_PROPERTY, "none")
      .enableAllLogsToConsole(true)
      .rootLoggerConfig(esRootLoggerConfig)
      .jsonOutput(true)
      .logDir(logDir);
    verifyProperties(underTest.build(),
      "appender.stdout.type", "Console",
      "appender.stdout.name", "stdout",
      "appender.stdout.layout.type", "PatternLayout",
      "appender.stdout.layout.pattern", expectedPattern,
      "rootLogger.appenderRef.stdout.ref", "stdout",
      "appender.file_es.layout.type", "PatternLayout",
      "appender.file_es.layout.pattern", expectedPattern,
      "appender.file_es.fileName", new File(logDir, "es.log").getAbsolutePath(),
      "appender.file_es.name", "file_es",
      "rootLogger.appenderRef.file_es.ref", "file_es",
      "appender.file_es.type", "File");
  }

  @Test
  public void enable_json_output_should_include_hostname_if_set() throws IOException {
    File logDir = temporaryFolder.newFolder();
    RootLoggerConfig esRootLoggerConfigWithHostname = newRootLoggerConfigBuilder()
      .setProcessId(ProcessId.ELASTICSEARCH)
      .setNodeNameField("my-node")
      .build();
    String expectedPattern = "{\"nodename\": \"my-node\",\"process\": \"es\",\"timestamp\": \"%d{yyyy-MM-dd'T'HH:mm:ss.SSSZZ}\","
      + "\"severity\": \"%p\",\"logger\": \"%c{1.}\",\"message\": \"%notEmpty{%enc{%marker}{JSON} }%enc{%.-10000m}{JSON}\"%exceptionAsJson }" + System.lineSeparator();

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(ROLLING_POLICY_PROPERTY, "none")
      .enableAllLogsToConsole(true)
      .rootLoggerConfig(esRootLoggerConfigWithHostname)
      .jsonOutput(true)
      .logDir(logDir);
    verifyProperties(underTest.build(),
      "appender.stdout.type", "Console",
      "appender.stdout.name", "stdout",
      "appender.stdout.layout.type", "PatternLayout",
      "appender.stdout.layout.pattern", expectedPattern,
      "rootLogger.appenderRef.stdout.ref", "stdout",
      "appender.file_es.layout.type", "PatternLayout",
      "appender.file_es.layout.pattern", expectedPattern,
      "appender.file_es.fileName", new File(logDir, "es.log").getAbsolutePath(),
      "appender.file_es.name", "file_es",
      "rootLogger.appenderRef.file_es.ref", "file_es",
      "appender.file_es.type", "File");
  }

  @Test
  public void apply_fails_with_IAE_if_LogLevelConfig_does_not_have_rootLoggerName_of_Log4J() throws IOException {
    LogLevelConfig logLevelConfig = LogLevelConfig.newBuilder(randomAlphanumeric(2)).build();
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder()
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(logLevelConfig);

    assertThatThrownBy(underTest::build)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Value of LogLevelConfig#rootLoggerName must be \"rootLogger\"");
  }

  @Test
  public void apply_fails_with_IAE_if_global_property_has_unsupported_level() throws IOException {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level", "ERROR")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(config);

    assertThatThrownBy(underTest::build)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("log level ERROR in property sonar.log.level is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");
  }

  @Test
  public void apply_fails_with_IAE_if_process_property_has_unsupported_level() throws IOException {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level.web", "ERROR")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(config);

    assertThatThrownBy(underTest::build)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("log level ERROR in property sonar.log.level.web is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");
  }

  @Test
  public void apply_sets_root_logger_to_INFO_if_no_property_is_set() throws IOException {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder()
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(config);

    underTest.build();

    verifyRootLoggerLevel(underTest, Level.INFO);
  }

  @Test
  public void apply_sets_root_logger_to_global_property_if_set() throws IOException {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level", "TRACE")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(config);

    verifyRootLoggerLevel(underTest, Level.TRACE);
  }

  @Test
  public void apply_sets_root_logger_to_process_property_if_set() throws IOException {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level.web", "DEBUG")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(config);

    verifyRootLoggerLevel(underTest, Level.DEBUG);
  }

  @Test
  public void apply_sets_root_logger_to_process_property_over_global_property_if_both_set() throws IOException {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level", "DEBUG",
      "sonar.log.level.web", "TRACE")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(config);

    verifyRootLoggerLevel(underTest, Level.TRACE);
  }

  @Test
  public void apply_sets_domain_property_over_process_and_global_property_if_all_set() throws IOException {
    LogLevelConfig config = newLogLevelConfig().levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.ES).build();
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      "sonar.log.level", "DEBUG",
      "sonar.log.level.web", "DEBUG",
      "sonar.log.level.web.es", "TRACE")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(config);

    verifyLoggerProperties(underTest.build(), "foo", Level.TRACE);
  }

  @Test
  public void apply_sets_domain_property_over_process_property_if_both_set() throws IOException {
    LogLevelConfig config = newLogLevelConfig().levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.ES).build();
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      "sonar.log.level.web", "DEBUG",
      "sonar.log.level.web.es", "TRACE")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(config);

    verifyLoggerProperties(underTest.build(), "foo", Level.TRACE);
  }

  @Test
  public void apply_sets_domain_property_over_global_property_if_both_set() throws IOException {
    LogLevelConfig config = newLogLevelConfig().levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.ES).build();
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      "sonar.log.level", "DEBUG",
      "sonar.log.level.web.es", "TRACE")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(config);

    verifyLoggerProperties(underTest.build(), "foo", Level.TRACE);
  }

  @Test
  public void apply_fails_with_IAE_if_domain_property_has_unsupported_level() throws IOException {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    LogLevelConfig config = newLogLevelConfig().levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.JMX).build();

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level.web.jmx", "ERROR")
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(config);

    assertThatThrownBy(underTest::build)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("log level ERROR in property sonar.log.level.web.jmx is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");
  }

  @Test
  @UseDataProvider("logbackLevels")
  public void apply_accepts_any_level_as_hardcoded_level(Level level) throws IOException {
    LogLevelConfig config = newLogLevelConfig().immutableLevel("bar", level).build();
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder()
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(config);

    verifyLoggerProperties(underTest.build(), "bar", level);
  }

  @Test
  public void apply_set_level_to_OFF_if_sonar_global_level_is_not_set() throws IOException {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder()
      .rootLoggerConfig(esRootLoggerConfig)
      .logLevelConfig(newLogLevelConfig().offUnlessTrace("fii").build())
      .logDir(logDir)
      .logPattern(logPattern);

    verifyLoggerProperties(underTest.build(), "fii", Level.OFF);
  }

  @Test
  public void fail_if_pattern_not_provided() {
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder()
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(new File("dir"));

    assertThrows(IllegalStateException.class, underTest::build);
  }

  @Test
  public void fail_if_root_logger_config_not_provided() {
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder()
      .logPattern("pattern")
      .logDir(new File("dir"));

    assertThrows(NullPointerException.class, underTest::build);
  }

  @Test
  public void fail_if_logDir_not_provided() {
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder()
      .logPattern("pattern")
      .rootLoggerConfig(esRootLoggerConfig);

    assertThrows(NullPointerException.class, underTest::build);
  }

  @Test
  public void apply_set_level_to_OFF_if_sonar_global_level_is_INFO() throws Exception {
    setLevelToOff(Level.INFO);
  }

  @Test
  public void apply_set_level_to_OFF_if_sonar_global_level_is_DEBUG() throws Exception {
    setLevelToOff(Level.DEBUG);
  }

  @Test
  public void apply_does_not_create_loggers_property_if_only_root_level_is_defined() throws IOException {
    LogLevelConfig logLevelConfig = newLogLevelConfig().rootLevelFor(ProcessId.APP).build();
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder()
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(logLevelConfig);

    assertThat(underTest.build().getProperty("loggers")).isNull();
  }

  @Test
  public void apply_creates_loggers_property_with_logger_names_ordered_but_root() throws IOException {
    LogLevelConfig config = newLogLevelConfig()
      .rootLevelFor(ProcessId.WEB_SERVER)
      .levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.JMX)
      .levelByDomain("bar", ProcessId.COMPUTE_ENGINE, LogDomain.ES)
      .immutableLevel("doh", Level.ERROR)
      .immutableLevel("pif", Level.TRACE)
      .offUnlessTrace("fii")
      .build();

    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder()
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(config);

    assertThat(underTest.build().getProperty("loggers")).isEqualTo("bar,doh,fii,foo,pif");
  }

  @Test
  public void apply_does_not_set_level_if_sonar_global_level_is_TRACE() throws IOException {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level", Level.TRACE.toString())
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(newLogLevelConfig().offUnlessTrace("fii").build());

    verifyNoLoggerProperties(underTest.build(), "fii");
  }

  private void setLevelToOff(Level globalLogLevel) throws IOException {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level", globalLogLevel.toString())
      .rootLoggerConfig(esRootLoggerConfig)
      .logDir(logDir)
      .logPattern(logPattern)
      .logLevelConfig(newLogLevelConfig().offUnlessTrace("fii").build());

    verifyLoggerProperties(underTest.build(), "fii", Level.OFF);
  }

  private static Log4JPropertiesBuilder newLog4JPropertiesBuilder(String... propertyKeysAndValues) {
    Properties properties = new Properties();
    assertThat(propertyKeysAndValues.length % 2).describedAs("propertyKeysAndValues must have even length").isZero();
    for (int i = 0; i < propertyKeysAndValues.length; i++) {
      properties.put(propertyKeysAndValues[i++], propertyKeysAndValues[i]);
    }
    return new Log4JPropertiesBuilder(new Props(properties));
  }

  private void verifyTimeRollingPolicy(Log4JPropertiesBuilder builder, File logDir, String logPattern, String datePattern, int maxFiles) {
    verifyProperties(builder.build(),
      "appender.file_es.type", "RollingFile",
      "appender.file_es.name", "file_es",
      "appender.file_es.filePattern", new File(logDir, "es.%d{" + datePattern + "}.log").getAbsolutePath(),
      "appender.file_es.fileName", new File(logDir, "es.log").getAbsolutePath(),
      "appender.file_es.layout.type", "PatternLayout",
      "appender.file_es.layout.pattern", logPattern,
      "appender.file_es.policies.type", "Policies",
      "appender.file_es.policies.time.type", "TimeBasedTriggeringPolicy",
      "appender.file_es.policies.time.interval", "1",
      "appender.file_es.policies.time.modulate", "true",
      "appender.file_es.strategy.type", "DefaultRolloverStrategy",
      "appender.file_es.strategy.fileIndex", "nomax",
      "appender.file_es.strategy.action.type", "Delete",
      "appender.file_es.strategy.action.basepath", logDir.getAbsolutePath(),
      "appender.file_es.strategy.action.maxDepth", "1",
      "appender.file_es.strategy.action.condition.type", "IfFileName",
      "appender.file_es.strategy.action.condition.glob", "es*",
      "appender.file_es.strategy.action.condition.nested_condition.type", "IfAccumulatedFileCount",
      "appender.file_es.strategy.action.condition.nested_condition.exceeds", valueOf(maxFiles),
      "rootLogger.appenderRef.file_es.ref", "file_es");
  }

  private void verifySizeRollingPolicy(Log4JPropertiesBuilder builder, File logDir, String logPattern, String sizePattern, int maxFiles) {
    verifyProperties(builder.build(),
      "appender.file_es.type", "RollingFile",
      "appender.file_es.name", "file_es",
      "appender.file_es.filePattern", new File(logDir, "es.%i.log").getAbsolutePath(),
      "appender.file_es.fileName", new File(logDir, "es.log").getAbsolutePath(),
      "appender.file_es.layout.type", "PatternLayout",
      "appender.file_es.layout.pattern", logPattern,
      "appender.file_es.policies.type", "Policies",
      "appender.file_es.policies.size.type", "SizeBasedTriggeringPolicy",
      "appender.file_es.policies.size.size", sizePattern,
      "appender.file_es.strategy.type", "DefaultRolloverStrategy",
      "appender.file_es.strategy.max", valueOf(maxFiles),
      "rootLogger.appenderRef.file_es.ref", "file_es");
  }

  private void verifyProperties(Properties properties, String... expectedPropertyKeysAndValuesOrdered) {
    assertThat(properties).containsEntry("status", "ERROR");
    if (expectedPropertyKeysAndValuesOrdered.length == 0) {
      assertThat(properties.size()).isOne();
    } else {
      assertThat(expectedPropertyKeysAndValuesOrdered.length % 2).describedAs("Number of parameters must be even").isZero();
      Set<String> keys = new HashSet<>(expectedPropertyKeysAndValuesOrdered.length / 2 + 1);
      keys.add("status");
      for (int i = 0; i < expectedPropertyKeysAndValuesOrdered.length; i++) {
        String key = expectedPropertyKeysAndValuesOrdered[i++];
        String value = expectedPropertyKeysAndValuesOrdered[i];
        assertThat(properties.get(key)).describedAs("Unexpected value for property " + key).isEqualTo(value);
        keys.add(key);
      }
      assertThat(properties).containsOnlyKeys(keys.toArray());
    }
  }

  private LogLevelConfig.Builder newLogLevelConfig() {
    return LogLevelConfig.newBuilder("rootLogger");
  }

  private void verifyLoggerProperties(Properties properties, String loggerName, Level expectedLevel) {
    assertThat(properties).containsEntry("logger." + loggerName + ".name", loggerName);
    assertThat(properties).containsEntry("logger." + loggerName + ".level", expectedLevel.toString());
  }

  private void verifyNoLoggerProperties(Properties properties, String loggerName) {
    assertThat(properties.get("logger." + loggerName + ".name")).isNull();
    assertThat(properties.get("logger." + loggerName + ".level")).isNull();
  }

  private void verifyRootLoggerLevel(Log4JPropertiesBuilder underTest, Level expectedLevel) {
    assertThat(underTest.build()).containsEntry("rootLogger.level", expectedLevel.toString());
  }

  @DataProvider
  public static Object[][] logbackLevels() {
    return new Object[][] {
      {Level.OFF},
      {Level.ERROR},
      {Level.WARN},
      {Level.INFO},
      {Level.DEBUG},
      {Level.TRACE},
      {Level.ALL}
    };
  }
}
