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
package org.sonar.application;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.process.LogbackHelper;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import java.io.File;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class AppLoggingTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  Props props = new Props(new Properties());
  AppLogging sut = new AppLogging();

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
  public void configure_defaults() throws Exception {
    LoggerContext ctx = sut.configure(props);

    Logger gobbler = ctx.getLogger(AppLogging.GOBBLER_LOGGER);
    Appender<ILoggingEvent> appender = gobbler.getAppender(AppLogging.GOBBLER_APPENDER);
    assertThat(appender).isInstanceOf(RollingFileAppender.class);

    // gobbler is not copied to console
    assertThat(gobbler.getAppender(AppLogging.GOBBLER_APPENDER)).isNotNull();
    assertThat(gobbler.getAppender(AppLogging.CONSOLE_APPENDER)).isNull();
  }

  @Test
  public void configure_no_rotation() throws Exception {
    props.set("sonar.log.rollingPolicy", "none");

    LoggerContext ctx = sut.configure(props);

    Logger gobbler = ctx.getLogger(AppLogging.GOBBLER_LOGGER);
    Appender<ILoggingEvent> appender = gobbler.getAppender(AppLogging.GOBBLER_APPENDER);
    assertThat(appender).isNotInstanceOf(RollingFileAppender.class).isInstanceOf(FileAppender.class);
  }

  @Test
  public void copyGobblerToConsole() throws Exception {
    props.set("sonar.log.console", "true");

    LoggerContext ctx = sut.configure(props);
    Logger gobbler = ctx.getLogger(AppLogging.GOBBLER_LOGGER);
    assertThat(gobbler.getAppender(AppLogging.GOBBLER_APPENDER)).isNotNull();
    assertThat(gobbler.getAppender(AppLogging.CONSOLE_APPENDER)).isNotNull();
  }
}
