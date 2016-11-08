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
package org.sonar.server.app;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import java.util.Properties;
import org.junit.AfterClass;
import org.junit.Test;
import org.sonar.process.LogbackHelper;
import org.sonar.process.Props;

import static org.assertj.core.api.Assertions.assertThat;

public class WebServerProcessLoggingTest {
  private static final String LOG_LEVEL_PROPERTY = "sonar.log.level";

  private WebServerProcessLogging underTest = new WebServerProcessLogging();

  private Props props = new Props(new Properties());

  @AfterClass
  public static void resetLogback() throws JoranException {
    new LogbackHelper().resetFromXml("/logback-test.xml");
  }

  @Test
  public void log_to_console() {
    LoggerContext ctx = underTest.configure(props);

    Logger root = ctx.getLogger(Logger.ROOT_LOGGER_NAME);
    Appender appender = root.getAppender("CONSOLE");
    assertThat(appender).isInstanceOf(ConsoleAppender.class);

    // default level is INFO
    assertThat(ctx.getLogger(Logger.ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.INFO);
    // change level of some loggers
    assertThat(ctx.getLogger("java.sql").getLevel()).isEqualTo(Level.WARN);
  }

  @Test
  public void enable_debug_logs() {
    props.set(LOG_LEVEL_PROPERTY, "DEBUG");
    LoggerContext ctx = underTest.configure(props);
    assertThat(ctx.getLogger(Logger.ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.DEBUG);
  }

  @Test
  public void enable_trace_logs() {
    props.set(LOG_LEVEL_PROPERTY, "TRACE");
    LoggerContext ctx = underTest.configure(props);
    assertThat(ctx.getLogger(Logger.ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.TRACE);
  }

}
