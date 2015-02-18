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
package org.sonar.api.utils.log;

import javax.annotation.Nullable;

import java.io.PrintStream;

import static org.sonar.api.utils.log.ConsoleFormatter.format;

/**
 * Slow implementation based on {@link java.lang.System#out}. It is not production-ready and it must be used
 * only for the tests that do not have logback dependency.
 * <p/>Implementation of message patterns is naive. It does not support escaped '{' and '}'
 * arguments.
 */
class ConsoleLogger extends BaseLogger {

  private final PrintStream stream;

  ConsoleLogger() {
    this.stream = System.out;
  }

  ConsoleLogger(PrintStream stream) {
    this.stream = stream;
  }

  @Override
  public boolean isTraceEnabled() {
    return Loggers.getFactory().getLevel() == LoggerLevel.TRACE;
  }

  @Override
  protected void doTrace(String msg) {
    if (isTraceEnabled()) {
      log("TRACE", msg);
    }
  }

  @Override
  protected void doTrace(String pattern, @Nullable Object arg) {
    if (isTraceEnabled()) {
      trace(format(pattern, arg));
    }
  }

  @Override
  protected void doTrace(String pattern, @Nullable Object arg1, @Nullable Object arg2) {
    if (isTraceEnabled()) {
      trace(format(pattern, arg1, arg2));
    }
  }

  @Override
  protected void doTrace(String pattern, Object... args) {
    if (isTraceEnabled()) {
      trace(format(pattern, args));
    }
  }

  @Override
  public boolean isDebugEnabled() {
    LoggerLevel level = Loggers.getFactory().getLevel();
    return level == LoggerLevel.TRACE || level == LoggerLevel.DEBUG;
  }

  @Override
  protected void doDebug(String msg) {
    if (isDebugEnabled()) {
      log("DEBUG", msg);
    }
  }

  @Override
  protected void doDebug(String pattern, @Nullable Object arg) {
    if (isDebugEnabled()) {
      debug(format(pattern, arg));
    }
  }

  @Override
  protected void doDebug(String pattern, @Nullable Object arg1, @Nullable Object arg2) {
    if (isDebugEnabled()) {
      debug(format(pattern, arg1, arg2));
    }
  }

  @Override
  protected void doDebug(String pattern, Object... args) {
    if (isDebugEnabled()) {
      debug(format(pattern, args));
    }
  }

  @Override
  protected void doInfo(String msg) {
    log("INFO ", msg);
  }

  @Override
  protected void doInfo(String pattern, @Nullable Object arg) {
    info(format(pattern, arg));
  }

  @Override
  protected void doInfo(String pattern, @Nullable Object arg1, @Nullable Object arg2) {
    info(format(pattern, arg1, arg2));
  }

  @Override
  protected void doInfo(String pattern, Object... args) {
    info(format(pattern, args));
  }

  @Override
  protected void doWarn(String msg) {
    log("WARN ", msg);
  }

  @Override
  protected void doWarn(String pattern, @Nullable Object arg) {
    warn(format(pattern, arg));
  }

  @Override
  protected void doWarn(String pattern, @Nullable Object arg1, @Nullable Object arg2) {
    warn(format(pattern, arg1, arg2));
  }

  @Override
  protected void doWarn(String pattern, Object... args) {
    warn(format(pattern, args));
  }

  @Override
  protected void doError(String msg) {
    log("ERROR", msg);
  }

  @Override
  protected void doError(String pattern, @Nullable Object arg) {
    error(format(pattern, arg));
  }

  @Override
  protected void doError(String pattern, @Nullable Object arg1, @Nullable Object arg2) {
    error(format(pattern, arg1, arg2));
  }

  @Override
  protected void doError(String pattern, Object... args) {
    error(format(pattern, args));
  }

  @Override
  public void doError(String msg, Throwable thrown) {
    doError(msg);
    thrown.printStackTrace();
  }

  @Override
  public boolean setLevel(LoggerLevel level) {
    return false;
  }

  private void log(String level, String msg) {
    this.stream.println(String.format("%s %s", level, msg));
  }
}
