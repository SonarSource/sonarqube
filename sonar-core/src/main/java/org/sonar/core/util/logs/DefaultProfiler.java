/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.core.util.logs;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.LoggerLevel;

class DefaultProfiler extends Profiler {

  private static final String CONTEXT_SEPARATOR = " | ";
  private static final String NO_MESSAGE_SUFFIX = "";

  private final LinkedHashMap<String, Object> context = new LinkedHashMap<>();
  private final Logger logger;

  private long startTime = 0L;
  private String startMessage = null;
  private Object[] args = null;
  private boolean logTimeLast = false;

  public DefaultProfiler(Logger logger) {
    this.logger = logger;
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public Profiler start() {
    this.startTime = System2.INSTANCE.now();
    this.startMessage = null;
    return this;
  }

  @Override
  public Profiler startTrace(String message) {
    return doStart(LoggerLevel.TRACE, message);
  }

  @Override
  public Profiler startTrace(String message, Object... args) {
    return doStart(LoggerLevel.TRACE, message, args);
  }

  @Override
  public Profiler startDebug(String message) {
    return doStart(LoggerLevel.DEBUG, message);
  }

  @Override
  public Profiler startDebug(String message, Object... args) {
    return doStart(LoggerLevel.DEBUG, message, args);
  }

  @Override
  public Profiler startInfo(String message) {
    return doStart(LoggerLevel.INFO, message);
  }

  @Override
  public Profiler startInfo(String message, Object... args) {
    return doStart(LoggerLevel.INFO, message, args);
  }

  @Override
  public long stopTrace() {
    return doStopWithoutMessage(LoggerLevel.TRACE);
  }

  @Override
  public long stopDebug() {
    return doStopWithoutMessage(LoggerLevel.DEBUG);
  }

  @Override
  public long stopInfo() {
    return doStopWithoutMessage(LoggerLevel.INFO);
  }

  @Override
  public long stopTrace(String message) {
    return doStop(LoggerLevel.TRACE, message, null, NO_MESSAGE_SUFFIX);
  }

  @Override
  public long stopTrace(String message, Object... args) {
    return doStop(LoggerLevel.TRACE, message, args, NO_MESSAGE_SUFFIX);
  }

  @Override
  public long stopDebug(String message) {
    return doStop(LoggerLevel.DEBUG, message, null, NO_MESSAGE_SUFFIX);
  }

  @Override
  public long stopDebug(String message, Object... args) {
    return doStop(LoggerLevel.DEBUG, message, args, NO_MESSAGE_SUFFIX);
  }

  @Override
  public long stopInfo(String message) {
    return doStop(LoggerLevel.INFO, message, null, NO_MESSAGE_SUFFIX);
  }

  @Override
  public long stopInfo(String message, Object... args) {
    return doStop(LoggerLevel.INFO, message, args, NO_MESSAGE_SUFFIX);
  }

  @Override
  public long stopError(String message, Object... args) {
    return doStop(LoggerLevel.ERROR, message, args, NO_MESSAGE_SUFFIX);
  }

  private Profiler doStart(LoggerLevel logLevel, String message, Object... args) {
    init(message, args);
    logStartMessage(logLevel, message, args);
    return this;
  }

  private void init(String message, Object... args) {
    this.startTime = System2.INSTANCE.now();
    this.startMessage = message;
    this.args = args;
  }

  private void reset() {
    this.startTime = 0L;
    this.startMessage = null;
    this.args = null;
    this.context.clear();
  }

  private void logStartMessage(LoggerLevel loggerLevel, String message, Object... args) {
    if (shouldLog(logger, loggerLevel)) {
      StringBuilder sb = new StringBuilder();
      sb.append(message);
      appendContext(sb);
      log(loggerLevel, sb.toString(), args);
    }
  }

  private long doStopWithoutMessage(LoggerLevel level) {
    if (startMessage == null) {
      throw new IllegalStateException("Profiler#stopXXX() can't be called without any message defined in start methods");
    }
    return doStop(level, startMessage, this.args, " (done)");
  }

  private long doStop(LoggerLevel level, @Nullable String message, @Nullable Object[] args, String messageSuffix) {
    if (startTime == 0L) {
      throw new IllegalStateException("Profiler must be started before being stopped");
    }
    long duration = System2.INSTANCE.now() - startTime;
    if (shouldLog(logger, level)) {
      StringBuilder sb = new StringBuilder();
      if (!StringUtils.isEmpty(message)) {
        sb.append(message);
        sb.append(messageSuffix);
      }
      if (logTimeLast) {
        appendContext(sb);
        appendTime(sb, duration);
      } else {
        appendTime(sb, duration);
        appendContext(sb);
      }
      log(level, sb.toString(), args);
    }
    reset();
    return duration;
  }

  private static void appendTime(StringBuilder sb, long duration) {
    if (sb.length() > 0) {
      sb.append(CONTEXT_SEPARATOR);
    }
    sb.append("time=").append(duration).append("ms");
  }

  private void appendContext(StringBuilder sb) {
    for (Map.Entry<String, Object> entry : context.entrySet()) {
      if (sb.length() > 0) {
        sb.append(CONTEXT_SEPARATOR);
      }
      sb.append(entry.getKey()).append("=").append(Objects.toString(entry.getValue()));
    }
  }

  void log(LoggerLevel level, String msg, @Nullable Object[] args) {
    switch (level) {
      case TRACE:
        logTrace(msg, args);
        break;
      case DEBUG:
        logDebug(msg, args);
        break;
      case INFO:
        logInfo(msg, args);
        break;
      case WARN:
        logWarn(msg, args);
        break;
      case ERROR:
        logError(msg, args);
        break;
      default:
        throw new IllegalArgumentException("Unsupported LoggerLevel value: " + level);
    }
  }

  private void logTrace(String msg, @Nullable Object[] args) {
    if (args == null) {
      logger.trace(msg);
    } else {
      logger.trace(msg, args);
    }
  }

  private void logDebug(String msg, @Nullable Object[] args) {
    if (args == null) {
      logger.debug(msg);
    } else {
      logger.debug(msg, args);
    }
  }

  private void logInfo(String msg, @Nullable Object[] args) {
    if (args == null) {
      logger.info(msg);
    } else {
      logger.info(msg, args);
    }
  }

  private void logWarn(String msg, @Nullable Object[] args) {
    if (args == null) {
      logger.warn(msg);
    } else {
      logger.warn(msg, args);
    }
  }

  private void logError(String msg, @Nullable Object[] args) {
    if (args == null) {
      logger.error(msg);
    } else {
      logger.error(msg, args);
    }
  }

  private static boolean shouldLog(Logger logger, LoggerLevel level) {
    if (level == LoggerLevel.TRACE && !logger.isTraceEnabled()) {
      return false;
    }
    return level != LoggerLevel.DEBUG || logger.isDebugEnabled();
  }

  @Override
  public Profiler addContext(String key, @Nullable Object value) {
    if (value == null) {
      context.remove(key);
    } else {
      context.put(key, value);
    }
    return this;
  }

  @Override
  public Profiler logTimeLast(boolean flag) {
    this.logTimeLast = flag;
    return this;
  }
}
