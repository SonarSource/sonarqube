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

  private final LinkedHashMap<String, Object> context = new LinkedHashMap<>();
  private final Logger logger;

  private long startTime = 0L;
  private String startMessage = null;
  private Object[] args = null;

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
    return doStop(LoggerLevel.TRACE, message, null, "");
  }

  @Override
  public long stopTrace(String message, Object... args) {
    return doStop(LoggerLevel.TRACE, message, args, "");
  }

  @Override
  public long stopDebug(String message) {
    return doStop(LoggerLevel.DEBUG, message, null, "");
  }

  @Override
  public long stopDebug(String message, Object... args) {
    return doStop(LoggerLevel.DEBUG, message, args, "");
  }

  @Override
  public long stopInfo(String message) {
    return doStop(LoggerLevel.INFO, message, null, "");
  }

  @Override
  public long stopInfo(String message, Object... args) {
    return doStop(LoggerLevel.INFO, message, args, "");
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
      logger.trace(sb.toString(), args);
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
        sb.append(CONTEXT_SEPARATOR);
      }
      sb.append("time=").append(duration).append("ms");
      appendContext(sb);
      log(level, sb.toString(), args);
    }
    reset();
    return duration;
  }

  void log(LoggerLevel level, String msg, @Nullable Object[] args) {
    switch (level) {
      case TRACE:
        if (args == null) {
          logger.trace(msg);
        } else {
          logger.trace(msg, args);
        }
        break;
      case DEBUG:
        if (args == null) {
          logger.debug(msg);
        } else {
          logger.debug(msg, args);
        }
        break;
      case INFO:
        if (args == null) {
          logger.info(msg);
        } else {
          logger.info(msg, args);
        }
        break;
      case WARN:
        if (args == null) {
          logger.warn(msg);
        } else {
          logger.warn(msg, args);
        }
        break;
      case ERROR:
        if (args == null) {
          logger.error(msg);
        } else {
          logger.error(msg, args);
        }
        break;
      default:
        throw new IllegalArgumentException("Unsupported LoggerLevel value: " + level);
    }
  }

  private static boolean shouldLog(Logger logger, LoggerLevel level) {
    if (level == LoggerLevel.TRACE && !logger.isTraceEnabled()) {
      return false;
    }
    if (level == LoggerLevel.DEBUG && !logger.isDebugEnabled()) {
      return false;
    }
    return true;
  }

  private void appendContext(StringBuilder sb) {
    for (Map.Entry<String, Object> entry : context.entrySet()) {
      if (sb.length() > 0) {
        sb.append(CONTEXT_SEPARATOR);
      }
      sb.append(entry.getKey()).append("=").append(Objects.toString(entry.getValue()));
    }
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

}
