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
package org.sonar.application;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import java.io.File;
import java.util.Properties;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.LogbackHelper;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.process.monitor.StreamGobbler.LOGGER_GOBBLER;

public class AppLoggingTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private File logDir;

  private Props props = new Props(new Properties());
  private AppLogging underTest = new AppLogging();

  @Before
  public void setUp() throws Exception {
    logDir = temp.newFolder();
    props.set(ProcessProperties.PATH_LOGS, logDir.getAbsolutePath());
  }

  @AfterClass
  public static void resetLogback() throws Exception {
    new LogbackHelper().resetFromXml("/logback-test.xml");
  }

  @Test
  public void configure_defaults() {
    LoggerContext ctx = underTest.configure(props);

    Logger gobbler = ctx.getLogger(LOGGER_GOBBLER);
    Appender<ILoggingEvent> appender = gobbler.getAppender(AppLogging.GOBBLER_APPENDER);
    assertThat(appender).isInstanceOf(RollingFileAppender.class);

    // gobbler is not copied to console
    assertThat(gobbler.getAppender(AppLogging.GOBBLER_APPENDER)).isNotNull();
    assertThat(gobbler.getAppender(AppLogging.CONSOLE_APPENDER)).isNull();
  }

  @Test
  public void configure_no_rotation() {
    props.set("sonar.log.rollingPolicy", "none");

    LoggerContext ctx = underTest.configure(props);

    Logger gobbler = ctx.getLogger(LOGGER_GOBBLER);
    Appender<ILoggingEvent> appender = gobbler.getAppender(AppLogging.GOBBLER_APPENDER);
    assertThat(appender).isNotInstanceOf(RollingFileAppender.class).isInstanceOf(FileAppender.class);
  }

  @Test
  public void copyGobblerToConsole() {
    props.set("sonar.log.console", "true");

    LoggerContext ctx = underTest.configure(props);
    Logger gobbler = ctx.getLogger(LOGGER_GOBBLER);
    assertThat(gobbler.getAppender(AppLogging.GOBBLER_APPENDER)).isNotNull();
    assertThat(gobbler.getAppender(AppLogging.CONSOLE_APPENDER)).isNotNull();
  }

  @Test
  public void default_level_for_root_logger_is_INFO() {
    LoggerContext ctx = underTest.configure(props);
    Logger rootLogger = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
    assertThat(rootLogger.getLevel()).isEqualTo(Level.INFO);
  }

  @Test
  public void root_logger_level_can_be_changed_with_a_property() {
    props.set("sonar.app.log.level", "TRACE");

    LoggerContext ctx = underTest.configure(props);
    Logger rootLogger = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
    assertThat(rootLogger.getLevel()).isEqualTo(Level.TRACE);
  }

  @Test
  public void property_changing_root_logger_level_is_case_insensitive() {
    props.set("sonar.app.log.level", "trace");

    LoggerContext ctx = underTest.configure(props);
    Logger rootLogger = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
    assertThat(rootLogger.getLevel()).isEqualTo(Level.TRACE);
  }

  @Test
  public void default_to_INFO_if_property_changing_root_logger_level_has_invalid_value() {
    props.set("sonar.app.log.level", "DodoDouh!");

    LoggerContext ctx = underTest.configure(props);
    Logger rootLogger = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
    assertThat(rootLogger.getLevel()).isEqualTo(Level.INFO);
  }
}
