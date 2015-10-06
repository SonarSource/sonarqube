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
package org.sonar.server.app;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import java.util.logging.LogManager;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.process.LogbackHelper;
import org.sonar.process.Props;
import org.sonar.server.computation.log.CeLogDenyFilter;
import org.sonar.server.computation.log.CeLogging;
import org.sonar.server.platform.ServerLogging;

/**
 * Configure logback for web server process. Logs must be written to console, which is
 * forwarded to file logs/sonar.log by the app master process.
 */
class ServerProcessLogging {

  private static final String LOG_FORMAT = "%d{yyyy.MM.dd HH:mm:ss} %-5level web[%logger{20}] %msg%n";
  public static final String LOG_LEVEL_PROPERTY = "sonar.log.level";

  private final LogbackHelper helper = new LogbackHelper();

  LoggerContext configure(Props props) {
    LoggerContext ctx = helper.getRootContext();
    ctx.reset();

    helper.enableJulChangePropagation(ctx);
    configureAppender(ctx, props);
    configureLevels(props);

    // Configure java.util.logging, used by Tomcat, in order to forward to slf4j
    LogManager.getLogManager().reset();
    SLF4JBridgeHandler.install();
    return ctx;
  }

  private void configureAppender(LoggerContext ctx, Props props) {
    ConsoleAppender<ILoggingEvent> consoleAppender = helper.newConsoleAppender(ctx, "CONSOLE", LOG_FORMAT, new CeLogDenyFilter<ILoggingEvent>());
    ctx.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(consoleAppender);
    ctx.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(CeLogging.createAppenderConfiguration(ctx, props));

  }

  private void configureLevels(Props props) {
    String levelCode = props.value(LOG_LEVEL_PROPERTY, "INFO");
    LoggerLevel level;
    if ("TRACE".equals(levelCode)) {
      level = LoggerLevel.TRACE;
    } else if ("DEBUG".equals(levelCode)) {
      level = LoggerLevel.DEBUG;
    } else if ("INFO".equals(levelCode)) {
      level = LoggerLevel.INFO;
    } else {
      throw MessageException.of(String.format("Unsupported log level: %s. Please check property %s", levelCode, LOG_LEVEL_PROPERTY));
    }
    ServerLogging.configureLevels(helper, level);
  }
}
