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
package org.sonar.ce.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import java.util.function.Supplier;
import org.apache.log4j.MDC;
import org.sonar.api.utils.log.Logger;
import org.sonar.ce.queue.CeTask;
import org.sonar.process.LogbackHelper;
import org.sonar.process.Props;

/**
 * Manages the logs written by Compute Engine:
 * <ul>
 *   <li>access to existing logs</li>
 *   <li>configure logback when CE worker starts and stops processing a task</li>
 * </ul>
 */
public class CeLogging {

  private static final String CE_ACTIVITY_APPENDER_NAME = "ce_activity";
  private static final String CE_ACTIVITY_FILE_NAME_PREFIX = "ce_activity";
  private static final String CE_ACTIVITY_ENCODER_PATTERN = "%d{yyyy.MM.dd HH:mm:ss} %-5level [%X{ceTaskUuid}][%logger{20}] %msg%n";

  static final String MDC_CE_ACTIVITY_FLAG = "ceActivityFlag";
  static final String MDC_CE_TASK_UUID = "ceTaskUuid";
  public static final String MAX_LOGS_PROPERTY = "sonar.ce.maxLogsPerTask";

  public void initForTask(CeTask task) {
    MDC.put(MDC_CE_TASK_UUID, task.getUuid());
  }

  public void clearForTask() {
    MDC.remove(MDC_CE_TASK_UUID);
  }

  public void logCeActivity(Logger logger, Runnable logProducer) {
    MDC.put(MDC_CE_ACTIVITY_FLAG, computeCeActivityFlag(logger));
    try {
      logProducer.run();
    } finally {
      MDC.remove(MDC_CE_ACTIVITY_FLAG);
    }
  }

  public <T> T logCeActivity(Logger logger, Supplier<T> logProducer) {
    MDC.put(MDC_CE_ACTIVITY_FLAG, computeCeActivityFlag(logger));
    try {
      return logProducer.get();
    } finally {
      MDC.remove(MDC_CE_ACTIVITY_FLAG);
    }
  }

  private static String computeCeActivityFlag(Logger logger) {
    return logger.isDebugEnabled() || logger.isTraceEnabled() ? "all" : "ce_only";
  }

  public static Appender<ILoggingEvent> createCeActivityAppenderConfiguration(LogbackHelper helper, LoggerContext ctx, Props processProps) {
    LogbackHelper.RollingPolicy rollingPolicy = helper.createRollingPolicy(ctx, processProps, CE_ACTIVITY_FILE_NAME_PREFIX);
    FileAppender<ILoggingEvent> fileAppender = rollingPolicy.createAppender(CE_ACTIVITY_APPENDER_NAME);
    fileAppender.setContext(ctx);

    PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
    consoleEncoder.setContext(ctx);
    consoleEncoder.setPattern(CE_ACTIVITY_ENCODER_PATTERN);
    consoleEncoder.start();
    fileAppender.setEncoder(consoleEncoder);
    fileAppender.addFilter(new CeActivityLogAcceptFilter<>());
    fileAppender.start();

    return fileAppender;
  }
}
