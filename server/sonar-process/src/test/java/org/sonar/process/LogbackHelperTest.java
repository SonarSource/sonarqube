/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.process;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class LogbackHelperTest {

  Props props = new Props(new Properties());
  LogbackHelper sut = new LogbackHelper();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    File dir = temp.newFolder();
    props.set(ProcessProperties.PATH_LOGS, dir.getAbsolutePath());
  }

  @AfterClass
  public static void resetLogback() throws Exception {
    new LogbackHelper().resetFromXml("/logback-test.xml");
  }

  @Test
  public void getRootContext() {
    assertThat(sut.getRootContext()).isNotNull();
  }

  @Test
  public void enableJulChangePropagation() {
    LoggerContext ctx = sut.getRootContext();
    int countListeners = ctx.getCopyOfListenerList().size();

    LoggerContextListener propagator = sut.enableJulChangePropagation(ctx);
    assertThat(ctx.getCopyOfListenerList().size()).isEqualTo(countListeners + 1);

    ctx.removeListener(propagator);
  }

  @Test
  public void newConsoleAppender() throws Exception {
    LoggerContext ctx = sut.getRootContext();
    ConsoleAppender appender = sut.newConsoleAppender(ctx, "MY_APPENDER", "%msg%n");

    assertThat(appender.getName()).isEqualTo("MY_APPENDER");
    assertThat(appender.getContext()).isSameAs(ctx);
    assertThat(appender.isStarted()).isTrue();
    assertThat(((PatternLayoutEncoder) appender.getEncoder()).getPattern()).isEqualTo("%msg%n");
  }

  @Test
  public void configureLogger() throws Exception {
    LoggerContext ctx = sut.getRootContext();

    Logger logger = sut.configureLogger(ctx, "my_logger", Level.WARN);

    assertThat(logger.getLevel()).isEqualTo(Level.WARN);
    assertThat(logger.getName()).isEqualTo("my_logger");
  }

  @Test
  public void createRollingPolicy_defaults() throws Exception {
    LoggerContext ctx = sut.getRootContext();
    LogbackHelper.RollingPolicy policy = sut.createRollingPolicy(ctx, props, "sonar");
    FileAppender appender = policy.createAppender("SONAR_FILE");
    assertThat(appender).isInstanceOf(RollingFileAppender.class);

    // max 5 daily files
    RollingFileAppender fileAppender = (RollingFileAppender) appender;
    TimeBasedRollingPolicy triggeringPolicy = (TimeBasedRollingPolicy) fileAppender.getTriggeringPolicy();
    assertThat(triggeringPolicy.getMaxHistory()).isEqualTo(7);
    assertThat(triggeringPolicy.getFileNamePattern()).endsWith("sonar.%d{yyyy-MM-dd}.log");
  }

  @Test
  public void createRollingPolicy_none() throws Exception {
    props.set("sonar.log.rollingPolicy", "none");
    LoggerContext ctx = sut.getRootContext();
    LogbackHelper.RollingPolicy policy = sut.createRollingPolicy(ctx, props, "sonar");

    Appender appender = policy.createAppender("SONAR_FILE");
    assertThat(appender).isNotInstanceOf(RollingFileAppender.class).isInstanceOf(FileAppender.class);
  }

  @Test
  public void createRollingPolicy_size() throws Exception {
    props.set("sonar.log.rollingPolicy", "size:1MB");
    props.set("sonar.log.maxFiles", "20");
    LoggerContext ctx = sut.getRootContext();
    LogbackHelper.RollingPolicy policy = sut.createRollingPolicy(ctx, props, "sonar");

    Appender appender = policy.createAppender("SONAR_FILE");
    assertThat(appender).isInstanceOf(RollingFileAppender.class);

    // max 20 files of 1Mb
    RollingFileAppender fileAppender = (RollingFileAppender) appender;
    FixedWindowRollingPolicy rollingPolicy = (FixedWindowRollingPolicy) fileAppender.getRollingPolicy();
    assertThat(rollingPolicy.getMaxIndex()).isEqualTo(20);
    assertThat(rollingPolicy.getFileNamePattern()).endsWith("sonar.%i.log");
    SizeBasedTriggeringPolicy triggeringPolicy = (SizeBasedTriggeringPolicy) fileAppender.getTriggeringPolicy();
    assertThat(triggeringPolicy.getMaxFileSize()).isEqualTo("1MB");
  }

  @Test
  public void createRollingPolicy_time() throws Exception {
    props.set("sonar.log.rollingPolicy", "time:yyyy-MM");
    props.set("sonar.log.maxFiles", "20");

    LoggerContext ctx = sut.getRootContext();
    LogbackHelper.RollingPolicy policy = sut.createRollingPolicy(ctx, props, "sonar");

    RollingFileAppender appender = (RollingFileAppender) policy.createAppender("SONAR_FILE");

    // max 5 monthly files
    TimeBasedRollingPolicy triggeringPolicy = (TimeBasedRollingPolicy) appender.getTriggeringPolicy();
    assertThat(triggeringPolicy.getMaxHistory()).isEqualTo(20);
    assertThat(triggeringPolicy.getFileNamePattern()).endsWith("sonar.%d{yyyy-MM}.log");
  }

  @Test
  public void createRollingPolicy_fail_if_unknown_policy() throws Exception {
    props.set("sonar.log.rollingPolicy", "unknown:foo");
    try {
      LoggerContext ctx = sut.getRootContext();
      sut.createRollingPolicy(ctx, props, "sonar");
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Unsupported value for property sonar.log.rollingPolicy: unknown:foo");
    }
  }
}
