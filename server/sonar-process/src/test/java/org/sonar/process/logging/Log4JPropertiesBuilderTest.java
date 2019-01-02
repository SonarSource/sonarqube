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
package org.sonar.process.logging;

import ch.qos.logback.classic.Level;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.process.MessageException;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;

import static java.lang.String.valueOf;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.logging.RootLoggerConfig.newRootLoggerConfigBuilder;

@RunWith(DataProviderRunner.class)
public class Log4JPropertiesBuilderTest {
  private static final String ROLLING_POLICY_PROPERTY = "sonar.log.rollingPolicy";
  private static final String PROPERTY_MAX_FILES = "sonar.log.maxFiles";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private final RootLoggerConfig esRootLoggerConfig = newRootLoggerConfigBuilder().setProcessId(ProcessId.ELASTICSEARCH).build();
  private final Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder();

  @Test
  public void constructor_fails_with_NPE_if_Props_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Props can't be null");

    new Log4JPropertiesBuilder(null);
  }

  @Test
  public void constructor_sets_status_to_ERROR() {
    Properties properties = underTest.get();

    assertThat(properties.getProperty("status")).isEqualTo("ERROR");
  }

  @Test
  public void getRootLoggerName_returns_rootLogger() {
    assertThat(underTest.getRootLoggerName()).isEqualTo("rootLogger");
  }

  @Test
  public void get_always_returns_a_new_object() {
    Properties previous = underTest.get();
    for (int i = 0; i < 2 + new Random().nextInt(5); i++) {
      Properties properties = underTest.get();
      assertThat(properties).isNotSameAs(previous);
      previous = properties;
    }
  }

  @Test
  public void buildLogPattern_puts_process_key_as_process_id() {
    String pattern = underTest.buildLogPattern(newRootLoggerConfigBuilder()
      .setProcessId(ProcessId.ELASTICSEARCH)
      .build());

    assertThat(pattern).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level es[][%logger{1.}] %msg%n");
  }

  @Test
  public void buildLogPattern_puts_threadIdFieldPattern_from_RootLoggerConfig_non_null() {
    String threadIdFieldPattern = RandomStringUtils.randomAlphabetic(5);

    String pattern = underTest.buildLogPattern(
      newRootLoggerConfigBuilder()
        .setProcessId(ProcessId.APP)
        .setThreadIdFieldPattern(threadIdFieldPattern)
        .build());

    assertThat(pattern).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level app[" + threadIdFieldPattern + "][%logger{1.}] %msg%n");
  }

  @Test
  public void buildLogPattern_does_not_put_threadIdFieldPattern_from_RootLoggerConfig_is_null() {
    String pattern = underTest.buildLogPattern(
      newRootLoggerConfigBuilder()
        .setProcessId(ProcessId.COMPUTE_ENGINE)
        .build());

    assertThat(pattern).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level ce[][%logger{1.}] %msg%n");
  }

  @Test
  public void buildLogPattern_does_not_put_threadIdFieldPattern_from_RootLoggerConfig_is_empty() {
    String pattern = underTest.buildLogPattern(
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
    underTest.configureGlobalFileLog(esRootLoggerConfig, logDir, logPattern);

    verifyTimeRollingPolicy(underTest, logDir, logPattern, "yyyy-MM-dd", 7);
  }

  @Test
  public void time_rolling_policy_has_large_max_files_if_property_is_zero() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = "foo";
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "time:yyyy-MM-dd",
      PROPERTY_MAX_FILES, "0");
    underTest.configureGlobalFileLog(esRootLoggerConfig, logDir, logPattern);

    verifyTimeRollingPolicy(underTest, logDir, logPattern, "yyyy-MM-dd", 100_000);
  }

  @Test
  public void time_rolling_policy_has_large_max_files_if_property_is_negative() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = "foo";
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "time:yyyy-MM-dd",
      PROPERTY_MAX_FILES, "-2");
    underTest.configureGlobalFileLog(esRootLoggerConfig, logDir, logPattern);

    verifyTimeRollingPolicy(underTest, logDir, logPattern, "yyyy-MM-dd", 100_000);
  }

  @Test
  public void size_rolling_policy_has_large_max_files_if_property_is_zero() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = "foo";
    String sizePattern = "1KB";
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "size:" + sizePattern,
      PROPERTY_MAX_FILES, "0");
    underTest.configureGlobalFileLog(esRootLoggerConfig, logDir, logPattern);

    verifySizeRollingPolicy(underTest, logDir, logPattern, sizePattern, 100_000);
  }

  @Test
  public void size_rolling_policy_has_large_max_files_if_property_is_negative() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = "foo";
    String sizePattern = "1KB";
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "size:" + sizePattern,
      PROPERTY_MAX_FILES, "-2");
    underTest.configureGlobalFileLog(esRootLoggerConfig, logDir, logPattern);

    verifySizeRollingPolicy(underTest, logDir, logPattern, sizePattern, 100_000);
  }

  @Test
  public void configureGlobalFileLog_throws_MessageException_when_property_is_not_supported() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);
    String invalidPropertyValue = randomAlphanumeric(3);
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, invalidPropertyValue);

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Unsupported value for property " + ROLLING_POLICY_PROPERTY + ": " + invalidPropertyValue);

    underTest.configureGlobalFileLog(esRootLoggerConfig, logDir, logPattern);
  }

  @Test
  public void configureGlobalFileLog_sets_properties_for_time_rolling_policy_with_max_7_files_when_property_starts_with_time_colon() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);
    String timePattern = randomAlphanumeric(6);
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "time:" + timePattern);
    underTest.configureGlobalFileLog(esRootLoggerConfig, logDir, logPattern);

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
      PROPERTY_MAX_FILES, valueOf(maxFile));
    underTest.configureGlobalFileLog(esRootLoggerConfig, logDir, logPattern);

    verifyTimeRollingPolicy(underTest, logDir, logPattern, timePattern, maxFile);
  }

  @Test
  public void configureGlobalFileLog_sets_properties_for_size_rolling_policy_with_max_7_files_when_property_starts_with_size_colon() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);
    String sizePattern = randomAlphanumeric(6);
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "size:" + sizePattern);
    underTest.configureGlobalFileLog(esRootLoggerConfig, logDir, logPattern);

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
      PROPERTY_MAX_FILES, valueOf(maxFile));
    underTest.configureGlobalFileLog(esRootLoggerConfig, logDir, logPattern);

    verifySizeRollingPolicy(underTest, logDir, logPattern, sizePattern, maxFile);
  }

  @Test
  public void configureGlobalFileLog_sets_properties_for_no_rolling_policy_when_property_is_none() throws Exception {
    File logDir = temporaryFolder.newFolder();
    String logPattern = randomAlphanumeric(15);
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      ROLLING_POLICY_PROPERTY, "none");
    underTest.configureGlobalFileLog(esRootLoggerConfig, logDir, logPattern);

    verifyPropertiesForConfigureGlobalFileLog(underTest.get(),
      "appender.file_es.type", "File",
      "appender.file_es.name", "file_es",
      "appender.file_es.fileName", new File(logDir, "es.log").getAbsolutePath(),
      "appender.file_es.layout.type", "PatternLayout",
      "appender.file_es.layout.pattern", logPattern,
      "rootLogger.appenderRef.file_es.ref", "file_es");
  }

  @Test
  public void apply_fails_with_IAE_if_LogLevelConfig_does_not_have_rootLoggerName_of_Log4J() {
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder();
    LogLevelConfig logLevelConfig = LogLevelConfig.newBuilder(randomAlphanumeric(2)).build();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of LogLevelConfig#rootLoggerName must be \"rootLogger\"");

    underTest.apply(logLevelConfig);
  }

  @Test
  public void apply_fails_with_IAE_if_global_property_has_unsupported_level() {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level", "ERROR");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("log level ERROR in property sonar.log.level is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.apply(config);
  }

  @Test
  public void apply_fails_with_IAE_if_process_property_has_unsupported_level() {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level.web", "ERROR");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("log level ERROR in property sonar.log.level.web is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.apply(config);
  }

  @Test
  public void apply_sets_root_logger_to_INFO_if_no_property_is_set() {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();

    underTest.apply(config);

    verifyRootLoggerLevel(underTest, Level.INFO);
  }

  @Test
  public void apply_sets_root_logger_to_global_property_if_set() {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level", "TRACE");

    underTest.apply(config);

    verifyRootLoggerLevel(underTest, Level.TRACE);
  }

  @Test
  public void apply_sets_root_logger_to_process_property_if_set() {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level.web", "DEBUG");

    underTest.apply(config);

    verifyRootLoggerLevel(underTest, Level.DEBUG);
  }

  @Test
  public void apply_sets_root_logger_to_process_property_over_global_property_if_both_set() {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level", "DEBUG",
      "sonar.log.level.web", "TRACE");

    underTest.apply(config);

    verifyRootLoggerLevel(underTest, Level.TRACE);
  }

  @Test
  public void apply_sets_domain_property_over_process_and_global_property_if_all_set() {
    LogLevelConfig config = newLogLevelConfig().levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.ES).build();
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      "sonar.log.level", "DEBUG",
      "sonar.log.level.web", "DEBUG",
      "sonar.log.level.web.es", "TRACE");

    underTest.apply(config);

    verifyLoggerProperties(underTest.get(), "foo", Level.TRACE);
  }

  @Test
  public void apply_sets_domain_property_over_process_property_if_both_set() {
    LogLevelConfig config = newLogLevelConfig().levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.ES).build();
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      "sonar.log.level.web", "DEBUG",
      "sonar.log.level.web.es", "TRACE");

    underTest.apply(config);

    verifyLoggerProperties(underTest.get(), "foo", Level.TRACE);
  }

  @Test
  public void apply_sets_domain_property_over_global_property_if_both_set() {
    LogLevelConfig config = newLogLevelConfig().levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.ES).build();
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder(
      "sonar.log.level", "DEBUG",
      "sonar.log.level.web.es", "TRACE");

    underTest.apply(config);

    verifyLoggerProperties(underTest.get(), "foo", Level.TRACE);
  }

  @Test
  public void apply_fails_with_IAE_if_domain_property_has_unsupported_level() {
    LogLevelConfig config = newLogLevelConfig().levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.JMX).build();

    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level.web.jmx", "ERROR");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("log level ERROR in property sonar.log.level.web.jmx is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.apply(config);
  }

  @Test
  @UseDataProvider("logbackLevels")
  public void apply_accepts_any_level_as_hardcoded_level(Level level) {
    LogLevelConfig config = newLogLevelConfig().immutableLevel("bar", level).build();

    underTest.apply(config);

    verifyLoggerProperties(underTest.get(), "bar", level);
  }

  @Test
  public void apply_set_level_to_OFF_if_sonar_global_level_is_not_set() {
    underTest.apply(newLogLevelConfig().offUnlessTrace("fii").build());

    verifyLoggerProperties(underTest.get(), "fii", Level.OFF);
  }

  @Test
  public void apply_set_level_to_OFF_if_sonar_global_level_is_INFO() {
    setLevelToOff(Level.INFO);
  }

  @Test
  public void apply_set_level_to_OFF_if_sonar_global_level_is_DEBUG() {
    setLevelToOff(Level.DEBUG);
  }

  @Test
  public void apply_does_not_create_loggers_property_if_only_root_level_is_defined() {
    LogLevelConfig logLevelConfig = newLogLevelConfig().rootLevelFor(ProcessId.APP).build();

    underTest.apply(logLevelConfig);

    assertThat(underTest.get().getProperty("loggers")).isNull();
  }

  @Test
  public void apply_creates_loggers_property_with_logger_names_ordered_but_root() {
    LogLevelConfig config = newLogLevelConfig()
      .rootLevelFor(ProcessId.WEB_SERVER)
      .levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.JMX)
      .levelByDomain("bar", ProcessId.COMPUTE_ENGINE, LogDomain.ES)
      .immutableLevel("doh", Level.ERROR)
      .immutableLevel("pif", Level.TRACE)
      .offUnlessTrace("fii")
      .build();

    underTest.apply(config);

    assertThat(underTest.get().getProperty("loggers")).isEqualTo("bar,doh,fii,foo,pif");
  }

  private void setLevelToOff(Level globalLogLevel) {
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level", globalLogLevel.toString());

    underTest.apply(newLogLevelConfig().offUnlessTrace("fii").build());

    verifyLoggerProperties(underTest.get(), "fii", Level.OFF);
  }

  @Test
  public void apply_does_not_set_level_if_sonar_global_level_is_TRACE() {
    Log4JPropertiesBuilder underTest = newLog4JPropertiesBuilder("sonar.log.level", Level.TRACE.toString());

    underTest.apply(newLogLevelConfig().offUnlessTrace("fii").build());

    verifyNoLoggerProperties(underTest.get(), "fii");
  }

  private static Log4JPropertiesBuilder newLog4JPropertiesBuilder(String... propertyKeysAndValues) {
    Properties properties = new Properties();
    assertThat(propertyKeysAndValues.length % 2).describedAs("propertyKeysAndValues must have even length").isEqualTo(0);
    for (int i = 0; i < propertyKeysAndValues.length; i++) {
      properties.put(propertyKeysAndValues[i++], propertyKeysAndValues[i]);
    }
    return new Log4JPropertiesBuilder(new Props(properties));
  }

  private void verifyTimeRollingPolicy(Log4JPropertiesBuilder builder, File logDir, String logPattern, String datePattern, int maxFiles) {
    verifyPropertiesForConfigureGlobalFileLog(builder.get(),
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
    verifyPropertiesForConfigureGlobalFileLog(builder.get(),
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

  private void verifyPropertiesForConfigureGlobalFileLog(Properties properties, String... expectedPropertyKeysAndValuesOrdered) {
    assertThat(properties.get("status")).isEqualTo("ERROR");
    if (expectedPropertyKeysAndValuesOrdered.length == 0) {
      assertThat(properties.size()).isEqualTo(1);
    } else {
      assertThat(expectedPropertyKeysAndValuesOrdered.length % 2).describedAs("Number of parameters must be even").isEqualTo(0);
      Set<String> keys = new HashSet<>(expectedPropertyKeysAndValuesOrdered.length / 2 + 1);
      keys.add("status");
      for (int i = 0; i < expectedPropertyKeysAndValuesOrdered.length; i++) {
        String key = expectedPropertyKeysAndValuesOrdered[i++];
        String value = expectedPropertyKeysAndValuesOrdered[i];
        assertThat(properties.get(key)).describedAs("Unexpected value for property " + key).isEqualTo(value);
        keys.add(key);
      }
      assertThat(properties.keySet()).containsOnly(keys.toArray());
    }
  }

  private LogLevelConfig.Builder newLogLevelConfig() {
    return LogLevelConfig.newBuilder("rootLogger");
  }

  private void verifyLoggerProperties(Properties properties, String loggerName, Level expectedLevel) {
    assertThat(properties.get("logger." + loggerName + ".name")).isEqualTo(loggerName);
    assertThat(properties.get("logger." + loggerName + ".level")).isEqualTo(expectedLevel.toString());
  }

  private void verifyNoLoggerProperties(Properties properties, String loggerName) {
    assertThat(properties.get("logger." + loggerName + ".name")).isNull();
    assertThat(properties.get("logger." + loggerName + ".level")).isNull();
  }

  private void verifyRootLoggerLevel(Log4JPropertiesBuilder underTest, Level expectedLevel) {
    assertThat(underTest.get().get("rootLogger.level")).isEqualTo(expectedLevel.toString());
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
