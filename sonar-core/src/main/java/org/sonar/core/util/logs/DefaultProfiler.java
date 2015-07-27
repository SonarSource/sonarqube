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
    this.startTime = System2.INSTANCE.now();
    this.startMessage = message;
    StringBuilder sb = new StringBuilder();
    sb.append(message);
    appendContext(sb);
    logger.trace(sb.toString());
    return this;
  }

  @Override
  public Profiler startDebug(String message) {
    this.startTime = System2.INSTANCE.now();
    this.startMessage = message;
    StringBuilder sb = new StringBuilder();
    sb.append(message);
    appendContext(sb);
    logger.debug(sb.toString());
    return this;
  }

  @Override
  public Profiler startInfo(String message) {
    this.startTime = System2.INSTANCE.now();
    this.startMessage = message;
    StringBuilder sb = new StringBuilder();
    sb.append(message);
    appendContext(sb);
    logger.info(sb.toString());
    return this;
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

  private long doStopWithoutMessage(LoggerLevel level) {
    if (startMessage == null) {
      throw new IllegalStateException("Profiler#stopXXX() can't be called without any message defined in start methods");
    }
    return doStop(level, startMessage, " (done)");
  }

  @Override
  public long stopTrace(String message) {
    return doStop(LoggerLevel.TRACE, message, "");
  }

  @Override
  public long stopDebug(String message) {
    return doStop(LoggerLevel.DEBUG, message, "");
  }

  @Override
  public long stopInfo(String message) {
    return doStop(LoggerLevel.INFO, message, "");
  }

  private long doStop(LoggerLevel level, @Nullable String message, String messageSuffix) {
    if (startTime == 0L) {
      throw new IllegalStateException("Profiler must be started before being stopped");
    }
    long duration = System2.INSTANCE.now() - startTime;
    StringBuilder sb = new StringBuilder();
    if (!StringUtils.isEmpty(message)) {
      sb.append(message);
      sb.append(messageSuffix);
      sb.append(CONTEXT_SEPARATOR);
    }
    sb.append("time=").append(duration).append("ms");
    appendContext(sb);
    log(level, sb.toString());
    startTime = 0L;
    startMessage = null;
    context.clear();
    return duration;
  }

  void log(LoggerLevel level, String msg) {
    switch (level) {
      case TRACE:
        logger.trace(msg);
        break;
      case DEBUG:
        logger.debug(msg);
        break;
      case INFO:
        logger.info(msg);
        break;
      case WARN:
        logger.warn(msg);
        break;
      case ERROR:
        logger.error(msg);
        break;
      default:
        throw new IllegalArgumentException("Unsupported LoggerLevel value: " + level);
    }
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
