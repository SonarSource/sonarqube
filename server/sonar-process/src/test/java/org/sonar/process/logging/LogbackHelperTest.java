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
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.sonar.process.MessageException;
import org.sonar.process.ProcessId;
import org.sonar.process.Props;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;
import static org.sonar.process.logging.RootLoggerConfig.newRootLoggerConfigBuilder;

@RunWith(DataProviderRunner.class)
public class LogbackHelperTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private Props props = new Props(new Properties());
  private LogbackHelper underTest = new LogbackHelper();

  @Before
  public void setUp() throws Exception {
    File dir = temp.newFolder();
    props.set(PATH_LOGS.getKey(), dir.getAbsolutePath());
  }

  @After
  public void resetLogback() throws Exception {
    new LogbackHelper().resetFromXml("/org/sonar/process/logging/LogbackHelperTest/logback-test.xml");
  }

  @Test
  public void getRootContext() {
    assertThat(underTest.getRootContext()).isNotNull();
  }

  @Test
  public void buildLogPattern_puts_process_key_as_process_id() {
    String pattern = underTest.buildLogPattern(newRootLoggerConfigBuilder()
      .setProcessId(ProcessId.ELASTICSEARCH)
      .build());

    assertThat(pattern).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level es[][%logger{20}] %msg%n");
  }

  @Test
  public void buildLogPattern_puts_threadIdFieldPattern_from_RootLoggerConfig_non_null() {
    String threadIdFieldPattern = RandomStringUtils.randomAlphabetic(5);
    String pattern = underTest.buildLogPattern(
      newRootLoggerConfigBuilder()
        .setProcessId(ProcessId.APP)
        .setThreadIdFieldPattern(threadIdFieldPattern)
        .build());

    assertThat(pattern).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level app[" + threadIdFieldPattern + "][%logger{20}] %msg%n");
  }

  @Test
  public void buildLogPattern_does_not_put_threadIdFieldPattern_from_RootLoggerConfig_is_null() {
    String pattern = underTest.buildLogPattern(
      newRootLoggerConfigBuilder()
        .setProcessId(ProcessId.COMPUTE_ENGINE)
        .build());

    assertThat(pattern).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level ce[][%logger{20}] %msg%n");
  }

  @Test
  public void buildLogPattern_does_not_put_threadIdFieldPattern_from_RootLoggerConfig_is_empty() {
    String pattern = underTest.buildLogPattern(
      newRootLoggerConfigBuilder()
        .setProcessId(ProcessId.WEB_SERVER)
        .setThreadIdFieldPattern("")
        .build());

    assertThat(pattern).isEqualTo("%d{yyyy.MM.dd HH:mm:ss} %-5level web[][%logger{20}] %msg%n");
  }

  @Test
  public void enableJulChangePropagation() {
    LoggerContext ctx = underTest.getRootContext();
    int countListeners = ctx.getCopyOfListenerList().size();

    LoggerContextListener propagator = underTest.enableJulChangePropagation(ctx);
    assertThat(ctx.getCopyOfListenerList().size()).isEqualTo(countListeners + 1);

    ctx.removeListener(propagator);
  }

  @Test
  public void verify_jul_initialization() {
    LoggerContext ctx = underTest.getRootContext();
    String logbackRootLoggerName = underTest.getRootLoggerName();
    LogLevelConfig config = LogLevelConfig.newBuilder(logbackRootLoggerName)
      .levelByDomain(logbackRootLoggerName, ProcessId.WEB_SERVER, LogDomain.JMX).build();
    props.set("sonar.log.level.web", "TRACE");
    underTest.apply(config, props);

    MemoryAppender memoryAppender = new MemoryAppender();
    memoryAppender.start();
    underTest.getRootContext().getLogger(logbackRootLoggerName).addAppender(memoryAppender);

    java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger("com.ms.sqlserver.jdbc.DTV");
    julLogger.finest("Message1");
    julLogger.finer("Message1");
    julLogger.fine("Message1");
    julLogger.info("Message1");
    julLogger.warning("Message1");
    julLogger.severe("Message1");

    // JUL bridge has not been initialized, nothing in logs
    assertThat(memoryAppender.getLogs()).hasSize(0);

    // Enabling JUL bridge
    LoggerContextListener propagator = underTest.enableJulChangePropagation(ctx);

    julLogger.finest("Message2");
    julLogger.finer("Message2");
    julLogger.fine("Message2");
    julLogger.info("Message2");
    julLogger.warning("Message2");
    julLogger.severe("Message2");

    assertThat(julLogger.isLoggable(java.util.logging.Level.FINEST)).isTrue();
    assertThat(julLogger.isLoggable(java.util.logging.Level.FINER)).isTrue();
    assertThat(julLogger.isLoggable(java.util.logging.Level.FINE)).isTrue();
    assertThat(julLogger.isLoggable(java.util.logging.Level.INFO)).isTrue();
    assertThat(julLogger.isLoggable(java.util.logging.Level.SEVERE)).isTrue();
    assertThat(julLogger.isLoggable(java.util.logging.Level.WARNING)).isTrue();

    // We are expecting messages from info to severe
    assertThat(memoryAppender.getLogs()).hasSize(6);
    memoryAppender.clear();

    ctx.getLogger(logbackRootLoggerName).setLevel(Level.INFO);

    julLogger.finest("Message3");
    julLogger.finer("Message3");
    julLogger.fine("Message3");
    julLogger.info("Message3");
    julLogger.warning("Message3");
    julLogger.severe("Message3");

    // We are expecting messages from finest to severe in TRACE mode
    assertThat(memoryAppender.getLogs()).hasSize(3);
    memoryAppender.clear();
    memoryAppender.stop();

    ctx.removeListener(propagator);
  }

  @Test
  public void newConsoleAppender() {
    LoggerContext ctx = underTest.getRootContext();
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(ctx);
    encoder.setPattern("%msg%n");
    encoder.start();
    ConsoleAppender<?> appender = underTest.newConsoleAppender(ctx, "MY_APPENDER", encoder);

    assertThat(appender.getName()).isEqualTo("MY_APPENDER");
    assertThat(appender.getContext()).isSameAs(ctx);
    assertThat(appender.isStarted()).isTrue();
    assertThat(((PatternLayoutEncoder) appender.getEncoder()).getPattern()).isEqualTo("%msg%n");
    assertThat(appender.getCopyOfAttachedFiltersList()).isEmpty();
  }

  @Test
  public void createRollingPolicy_defaults() {
    LoggerContext ctx = underTest.getRootContext();
    LogbackHelper.RollingPolicy policy = underTest.createRollingPolicy(ctx, props, "sonar");
    FileAppender appender = policy.createAppender("SONAR_FILE");
    assertThat(appender).isInstanceOf(RollingFileAppender.class);

    // max 5 daily files
    RollingFileAppender fileAppender = (RollingFileAppender) appender;
    TimeBasedRollingPolicy triggeringPolicy = (TimeBasedRollingPolicy) fileAppender.getTriggeringPolicy();
    assertThat(triggeringPolicy.getMaxHistory()).isEqualTo(7);
    assertThat(triggeringPolicy.getFileNamePattern()).endsWith("sonar.%d{yyyy-MM-dd}.log");
  }

  @Test
  public void createRollingPolicy_none() {
    props.set("sonar.log.rollingPolicy", "none");
    LoggerContext ctx = underTest.getRootContext();
    LogbackHelper.RollingPolicy policy = underTest.createRollingPolicy(ctx, props, "sonar");

    Appender appender = policy.createAppender("SONAR_FILE");
    assertThat(appender).isNotInstanceOf(RollingFileAppender.class).isInstanceOf(FileAppender.class);
  }

  @Test
  public void createRollingPolicy_size() throws Exception {
    props.set("sonar.log.rollingPolicy", "size:1MB");
    props.set("sonar.log.maxFiles", "20");
    LoggerContext ctx = underTest.getRootContext();
    LogbackHelper.RollingPolicy policy = underTest.createRollingPolicy(ctx, props, "sonar");

    Appender appender = policy.createAppender("SONAR_FILE");
    assertThat(appender).isInstanceOf(RollingFileAppender.class);

    // max 20 files of 1Mb
    RollingFileAppender fileAppender = (RollingFileAppender) appender;
    FixedWindowRollingPolicy rollingPolicy = (FixedWindowRollingPolicy) fileAppender.getRollingPolicy();
    assertThat(rollingPolicy.getMaxIndex()).isEqualTo(20);
    assertThat(rollingPolicy.getFileNamePattern()).endsWith("sonar.%i.log");
    SizeBasedTriggeringPolicy triggeringPolicy = (SizeBasedTriggeringPolicy) fileAppender.getTriggeringPolicy();
    FileSize maxFileSize = (FileSize) FieldUtils.readField(triggeringPolicy, "maxFileSize", true);
    assertThat(maxFileSize.getSize()).isEqualTo(1024L * 1024);
  }

  @Test
  public void createRollingPolicy_time() {
    props.set("sonar.log.rollingPolicy", "time:yyyy-MM");
    props.set("sonar.log.maxFiles", "20");

    LoggerContext ctx = underTest.getRootContext();
    LogbackHelper.RollingPolicy policy = underTest.createRollingPolicy(ctx, props, "sonar");

    RollingFileAppender appender = (RollingFileAppender) policy.createAppender("SONAR_FILE");

    // max 5 monthly files
    TimeBasedRollingPolicy triggeringPolicy = (TimeBasedRollingPolicy) appender.getTriggeringPolicy();
    assertThat(triggeringPolicy.getMaxHistory()).isEqualTo(20);
    assertThat(triggeringPolicy.getFileNamePattern()).endsWith("sonar.%d{yyyy-MM}.log");
  }

  @Test
  public void createRollingPolicy_fail_if_unknown_policy() {
    props.set("sonar.log.rollingPolicy", "unknown:foo");
    try {
      LoggerContext ctx = underTest.getRootContext();
      underTest.createRollingPolicy(ctx, props, "sonar");
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Unsupported value for property sonar.log.rollingPolicy: unknown:foo");
    }
  }

  @Test
  public void apply_fails_with_IAE_if_LogLevelConfig_does_not_have_ROOT_LOGGER_NAME_of_LogBack() {
    LogLevelConfig logLevelConfig = LogLevelConfig.newBuilder(randomAlphanumeric(2)).build();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Value of LogLevelConfig#rootLoggerName must be \"ROOT\"");

    underTest.apply(logLevelConfig, props);
  }

  @Test
  public void apply_fails_with_IAE_if_global_property_has_unsupported_level() {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();

    props.set("sonar.log.level", "ERROR");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("log level ERROR in property sonar.log.level is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.apply(config, props);
  }

  @Test
  public void apply_fails_with_IAE_if_process_property_has_unsupported_level() {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();

    props.set("sonar.log.level.web", "ERROR");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("log level ERROR in property sonar.log.level.web is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.apply(config, props);
  }

  @Test
  public void apply_sets_logger_to_INFO_if_no_property_is_set() {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();

    LoggerContext context = underTest.apply(config, props);

    assertThat(context.getLogger(ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.INFO);
  }

  @Test
  public void apply_sets_logger_to_globlal_property_if_set() {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();

    props.set("sonar.log.level", "TRACE");

    LoggerContext context = underTest.apply(config, props);

    assertThat(context.getLogger(ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.TRACE);
  }

  @Test
  public void apply_sets_logger_to_process_property_if_set() {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();

    props.set("sonar.log.level.web", "DEBUG");

    LoggerContext context = underTest.apply(config, props);

    assertThat(context.getLogger(ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.DEBUG);
  }

  @Test
  public void apply_sets_logger_to_process_property_over_global_property_if_both_set() {
    LogLevelConfig config = newLogLevelConfig().rootLevelFor(ProcessId.WEB_SERVER).build();
    props.set("sonar.log.level", "DEBUG");
    props.set("sonar.log.level.web", "TRACE");

    LoggerContext context = underTest.apply(config, props);

    assertThat(context.getLogger(ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.TRACE);
  }

  @Test
  public void apply_sets_domain_property_over_process_and_global_property_if_all_set() {
    LogLevelConfig config = newLogLevelConfig().levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.ES).build();
    props.set("sonar.log.level", "DEBUG");
    props.set("sonar.log.level.web", "DEBUG");
    props.set("sonar.log.level.web.es", "TRACE");

    LoggerContext context = underTest.apply(config, props);

    assertThat(context.getLogger("foo").getLevel()).isEqualTo(Level.TRACE);
  }

  @Test
  public void apply_sets_domain_property_over_process_property_if_both_set() {
    LogLevelConfig config = newLogLevelConfig().levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.ES).build();
    props.set("sonar.log.level.web", "DEBUG");
    props.set("sonar.log.level.web.es", "TRACE");

    LoggerContext context = underTest.apply(config, props);

    assertThat(context.getLogger("foo").getLevel()).isEqualTo(Level.TRACE);
  }

  @Test
  public void apply_sets_domain_property_over_global_property_if_both_set() {
    LogLevelConfig config = newLogLevelConfig().levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.ES).build();
    props.set("sonar.log.level", "DEBUG");
    props.set("sonar.log.level.web.es", "TRACE");

    LoggerContext context = underTest.apply(config, props);

    assertThat(context.getLogger("foo").getLevel()).isEqualTo(Level.TRACE);
  }

  @Test
  public void apply_fails_with_IAE_if_domain_property_has_unsupported_level() {
    LogLevelConfig config = newLogLevelConfig().levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.JMX).build();

    props.set("sonar.log.level.web.jmx", "ERROR");

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("log level ERROR in property sonar.log.level.web.jmx is not a supported value (allowed levels are [TRACE, DEBUG, INFO])");

    underTest.apply(config, props);
  }

  @Test
  @UseDataProvider("logbackLevels")
  public void apply_accepts_any_level_as_hardcoded_level(Level level) {
    LogLevelConfig config = newLogLevelConfig().immutableLevel("bar", level).build();

    LoggerContext context = underTest.apply(config, props);

    assertThat(context.getLogger("bar").getLevel()).isEqualTo(level);
  }

  @Test
  public void changeRoot_sets_level_of_ROOT_and_all_loggers_with_a_config_but_the_hardcoded_one() {
    LogLevelConfig config = newLogLevelConfig()
      .rootLevelFor(ProcessId.WEB_SERVER)
      .levelByDomain("foo", ProcessId.WEB_SERVER, LogDomain.JMX)
      .levelByDomain("bar", ProcessId.COMPUTE_ENGINE, LogDomain.ES)
      .immutableLevel("doh", Level.ERROR)
      .immutableLevel("pif", Level.TRACE)
      .build();
    LoggerContext context = underTest.apply(config, props);
    assertThat(context.getLogger(ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.INFO);
    assertThat(context.getLogger("foo").getLevel()).isEqualTo(Level.INFO);
    assertThat(context.getLogger("bar").getLevel()).isEqualTo(Level.INFO);
    assertThat(context.getLogger("doh").getLevel()).isEqualTo(Level.ERROR);
    assertThat(context.getLogger("pif").getLevel()).isEqualTo(Level.TRACE);

    underTest.changeRoot(config, Level.DEBUG);

    assertThat(context.getLogger(ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.DEBUG);
    assertThat(context.getLogger("foo").getLevel()).isEqualTo(Level.DEBUG);
    assertThat(context.getLogger("bar").getLevel()).isEqualTo(Level.DEBUG);
    assertThat(context.getLogger("doh").getLevel()).isEqualTo(Level.ERROR);
    assertThat(context.getLogger("pif").getLevel()).isEqualTo(Level.TRACE);
  }

  @Test
  public void apply_set_level_to_OFF_if_sonar_global_level_is_not_set() {
    LoggerContext context = underTest.apply(newLogLevelConfig().offUnlessTrace("fii").build(), new Props(new Properties()));

    assertThat(context.getLogger("fii").getLevel()).isEqualTo(Level.OFF);
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
  public void apply_does_not_set_level_if_sonar_global_level_is_TRACE() {
    Properties properties = new Properties();
    properties.setProperty("sonar.log.level", Level.TRACE.toString());
    assertThat(underTest.getRootContext().getLogger("fii").getLevel()).isNull();

    LoggerContext context = underTest.apply(newLogLevelConfig().offUnlessTrace("fii").build(), new Props(properties));

    assertThat(context.getLogger("fii").getLevel()).isNull();
  }

  @Test
  public void createEncoder_uses_pattern_by_default() {
    RootLoggerConfig config = newRootLoggerConfigBuilder()
      .setProcessId(ProcessId.WEB_SERVER)
      .build();

    Encoder<ILoggingEvent> encoder = underTest.createEncoder(props, config, underTest.getRootContext());

    assertThat(encoder).isInstanceOf(PatternLayoutEncoder.class);
  }

  @Test
  public void createEncoder_uses_json_output() {
    props.set("sonar.log.useJsonOutput", "true");
    RootLoggerConfig config = newRootLoggerConfigBuilder()
      .setProcessId(ProcessId.WEB_SERVER)
      .build();

    Encoder<ILoggingEvent> encoder = underTest.createEncoder(props, config, underTest.getRootContext());

    assertThat(encoder).isInstanceOf(LayoutWrappingEncoder.class);
    Layout layout = ((LayoutWrappingEncoder) encoder).getLayout();
    assertThat(layout).isInstanceOf(LogbackJsonLayout.class);
    assertThat(((LogbackJsonLayout)layout).getProcessKey()).isEqualTo("web");
  }

  private LogLevelConfig.Builder newLogLevelConfig() {
    return LogLevelConfig.newBuilder(ROOT_LOGGER_NAME);
  }

  private void setLevelToOff(Level globalLogLevel) {
    Properties properties = new Properties();
    properties.setProperty("sonar.log.level", globalLogLevel.toString());

    LoggerContext context = underTest.apply(newLogLevelConfig().offUnlessTrace("fii").build(), new Props(properties));

    assertThat(context.getLogger("fii").getLevel()).isEqualTo(Level.OFF);
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
