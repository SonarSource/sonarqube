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
package org.sonar.search;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ConsoleAppender;

import org.sonar.process.LogbackHelper;

public class SearchLogging {

  private static final String LOG_FORMAT = "%d{yyyy.MM.dd HH:mm:ss} %-5level  es[%logger{20}] %X %msg%n";

  private LogbackHelper helper = new LogbackHelper();

  public LoggerContext configure() {
    LoggerContext ctx = helper.getRootContext();
    ctx.reset();

    ConsoleAppender<ILoggingEvent> consoleAppender = helper.newConsoleAppender(ctx, "CONSOLE", LOG_FORMAT);
    Logger rootLogger = helper.configureLogger(ctx, Logger.ROOT_LOGGER_NAME, Level.INFO);
    rootLogger.addAppender(consoleAppender);
    return ctx;
  }
}
