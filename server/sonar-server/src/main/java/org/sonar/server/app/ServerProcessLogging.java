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

import ch.qos.logback.classic.LoggerContext;
import java.util.logging.LogManager;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.process.LogbackHelper;
import org.sonar.process.Props;
import org.sonar.server.platform.ServerLogging;

public abstract class ServerProcessLogging {
  private static final String LOG_LEVEL_PROPERTY = "sonar.log.level";
  private static final String PROCESS_NAME_PLACEHOLDER = "XXXX";
  private static final String THREAD_ID_PLACEHOLDER = "ZZZZ";
  private static final String LOG_FORMAT = "%d{yyyy.MM.dd HH:mm:ss} %-5level " + PROCESS_NAME_PLACEHOLDER + "[" + THREAD_ID_PLACEHOLDER + "][%logger{20}] %msg%n";
  private final String processName;
  private final String threadIdField;
  private final LogbackHelper helper = new LogbackHelper();

  protected ServerProcessLogging(String processName, String threadIdPattern) {
    this.processName = processName;
    this.threadIdField = threadIdPattern;
  }

  public LoggerContext configure(Props props) {
    LoggerContext ctx = helper.getRootContext();
    ctx.reset();

    helper.enableJulChangePropagation(ctx);
    configureAppenders(ctx, props);
    configureLevels(props);

    // Configure java.util.logging, used by Tomcat, in order to forward to slf4j
    LogManager.getLogManager().reset();
    SLF4JBridgeHandler.install();
    return ctx;
  }

  private void configureAppenders(LoggerContext ctx, Props props) {
    String logFormat = LOG_FORMAT.replace(PROCESS_NAME_PLACEHOLDER, processName).replace(THREAD_ID_PLACEHOLDER, threadIdField);
    configureAppenders(logFormat, ctx, helper, props);
  }

  protected abstract void configureAppenders(String logFormat, LoggerContext ctx, LogbackHelper helper, Props props);

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
