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
package org.sonar.ce.app;

import javax.annotation.Nullable;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.LoggerLevel;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * 
 */
public final class LogarithmicLogger implements Logger {
  private final Logger logger;
  private final long callRatio;
  private long callCounter = -1;
  private long logCounter = -1;

  private LogarithmicLogger(Builder builder) {
    this.logger = builder.logger;
    this.callRatio = builder.callRatio;
  }

  public static Builder from(Logger logger) {
    return new Builder(logger);
  }

  public static final class Builder {
    private final Logger logger;
    private long callRatio = 1;

    public Builder(Logger logger) {
      this.logger = logger;
    }

    public Builder applyingCallRatio(long callRatio) {
      checkArgument(callRatio >= 1, "callRatio must be => 1");
      this.callRatio = callRatio;
      return this;
    }

    public Logger build() {
      return new LogarithmicLogger(this);
    }
  }


  private boolean shouldLog() {
    callCounter++;
    long ratioed = callCounter / callRatio;
    long log = (long) Math.log(ratioed);
    if (log > logCounter) {
      logCounter = log;
      return true;
    }
    return false;
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public boolean setLevel(LoggerLevel level) {
    return logger.setLevel(level);
  }

  @Override
  public LoggerLevel getLevel() {
    return logger.getLevel();
  }

  @Override
  public void trace(String msg) {
    if (shouldLog()) {
      logger.trace(msg);
    }
  }

  @Override
  public void trace(String pattern, @Nullable Object arg) {
    if (shouldLog()) {
      logger.trace(pattern, arg);
    }
  }

  @Override
  public void trace(String msg, @Nullable Object arg1, @Nullable Object arg2) {
    if (shouldLog()) {
      logger.trace(msg, arg1, arg2);
    }
  }

  @Override
  public void trace(String msg, Object... args) {
    if (shouldLog()) {
      logger.trace(msg, args);
    }
  }

  @Override
  public void debug(String msg) {
    if (shouldLog()) {
      logger.debug(msg);
    }
  }

  @Override
  public void debug(String pattern, @Nullable Object arg) {
    if (shouldLog()) {
      logger.debug(pattern, arg);
    }
  }

  @Override
  public void debug(String msg, @Nullable Object arg1, @Nullable Object arg2) {
    if (shouldLog()) {
      logger.debug(msg, arg1, arg2);
    }
  }

  @Override
  public void debug(String msg, Object... args) {
    if (shouldLog()) {
      logger.debug(msg, args);
    }
  }

  @Override
  public void info(String msg) {
    if (shouldLog()) {
      logger.info(msg);
    }
  }

  @Override
  public void info(String msg, @Nullable Object arg) {
    if (shouldLog()) {
      logger.info(msg, arg);
    }
  }

  @Override
  public void info(String msg, @Nullable Object arg1, @Nullable Object arg2) {
    if (shouldLog()) {
      logger.info(msg, arg1, arg2);
    }
  }

  @Override
  public void info(String msg, Object... args) {
    if (shouldLog()) {
      logger.info(msg, args);
    }
  }

  @Override
  public void warn(String msg) {
    if (shouldLog()) {
      logger.warn(msg);
    }
  }

  @Override
  public void warn(String msg, Throwable throwable) {
    if (shouldLog()) {
      logger.warn(msg, throwable);
    }
  }

  @Override
  public void warn(String msg, @Nullable Object arg) {
    if (shouldLog()) {
      logger.warn(msg, arg);
    }
  }

  @Override
  public void warn(String msg, @Nullable Object arg1, @Nullable Object arg2) {
    if (shouldLog()) {
      logger.warn(msg, arg1, arg2);
    }
  }

  @Override
  public void warn(String msg, Object... args) {
    if (shouldLog()) {
      logger.warn(msg, args);
    }
  }

  @Override
  public void error(String msg) {
    if (shouldLog()) {
      logger.error(msg);
    }
  }

  @Override
  public void error(String msg, @Nullable Object arg) {
    if (shouldLog()) {
      logger.error(msg, arg);
    }
  }

  @Override
  public void error(String msg, @Nullable Object arg1, @Nullable Object arg2) {
    if (shouldLog()) {
      logger.error(msg, arg1, arg2);
    }
  }

  @Override
  public void error(String msg, Object... args) {
    if (shouldLog()) {
      logger.error(msg, args);
    }
  }

  @Override
  public void error(String msg, Throwable thrown) {
    if (shouldLog()) {
      logger.error(msg, thrown);
    }
  }
}
