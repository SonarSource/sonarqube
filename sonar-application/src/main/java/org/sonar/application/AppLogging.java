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
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import org.slf4j.LoggerFactory;
import org.sonar.process.LogbackHelper;
import org.sonar.process.Props;

import static org.sonar.process.monitor.StreamGobbler.LOGGER_GOBBLER;

/**
 * Configure logback for the master process
 */
class AppLogging {

  static final String CONSOLE_LOGGER = "console";
  static final String CONSOLE_APPENDER = "CONSOLE";
  static final String GOBBLER_APPENDER = "GOBBLER";
  static final String APP_PATTERN = "%d{yyyy.MM.dd HH:mm:ss} %-5level app[][%logger{20}] %msg%n";

  private final LogbackHelper helper = new LogbackHelper();

  LoggerContext configure(Props props) {
    LoggerContext ctx = helper.getRootContext();
    ctx.reset();
    helper.enableJulChangePropagation(ctx);
    configureConsole(ctx);
    configureGobbler(props, ctx);
    configureRoot(props, ctx);
    if (props.valueAsBoolean("sonar.log.console", false)) {
      // used by SonarSource testing environment
      copyGobblerToConsole();
    }
    return ctx;
  }

  /**
   * Enable the copy in console of the logs written in logs/sonar.log
   */
  private static void copyGobblerToConsole() {
    Logger consoleLogger = (Logger) LoggerFactory.getLogger(CONSOLE_LOGGER);
    Appender<ILoggingEvent> consoleAppender = consoleLogger.getAppender(CONSOLE_APPENDER);

    Logger gobblerLogger = (Logger) LoggerFactory.getLogger(LOGGER_GOBBLER);
    gobblerLogger.addAppender(consoleAppender);
  }

  private void configureConsole(LoggerContext loggerContext) {
    ConsoleAppender<ILoggingEvent> consoleAppender = helper.newConsoleAppender(loggerContext, CONSOLE_APPENDER, "%msg%n");
    Logger consoleLogger = loggerContext.getLogger(CONSOLE_LOGGER);
    consoleLogger.setAdditive(false);
    consoleLogger.addAppender(consoleAppender);
  }

  private void configureGobbler(Props props, LoggerContext ctx) {
    // configure appender
    LogbackHelper.RollingPolicy rollingPolicy = helper.createRollingPolicy(ctx, props, "sonar");
    FileAppender<ILoggingEvent> fileAppender = rollingPolicy.createAppender(GOBBLER_APPENDER);
    fileAppender.setContext(ctx);
    PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
    fileEncoder.setContext(ctx);
    fileEncoder.setPattern("%msg%n");
    fileEncoder.start();
    fileAppender.setEncoder(fileEncoder);
    fileAppender.start();

    // configure logger
    Logger gobblerLogger = ctx.getLogger(LOGGER_GOBBLER);
    gobblerLogger.setAdditive(false);
    gobblerLogger.addAppender(fileAppender);
  }

  private void configureRoot(Props props, LoggerContext loggerContext) {
    ConsoleAppender<ILoggingEvent> consoleAppender = helper.newConsoleAppender(loggerContext, "ROOT_CONSOLE", APP_PATTERN);
    Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.setLevel(Level.toLevel(props.value("sonar.app.log.level", Level.INFO.toString()), Level.INFO));
    rootLogger.addAppender(consoleAppender);
  }
}
