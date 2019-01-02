/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.utils.log;

import java.io.PrintStream;
import javax.annotation.Nullable;

import static org.sonar.api.utils.log.ConsoleFormatter.format;

/**
 * Slow implementation based on {@link java.lang.System#out}. It is not production-ready and it must be used
 * only for the tests that do not have logback dependency.
 * <br>Implementation of message patterns is naive. It does not support escaped '{' and '}'
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
      doTrace(format(pattern, arg));
    }
  }

  @Override
  protected void doTrace(String pattern, @Nullable Object arg1, @Nullable Object arg2) {
    if (isTraceEnabled()) {
      doTrace(format(pattern, arg1, arg2));
    }
  }

  @Override
  protected void doTrace(String pattern, Object... args) {
    if (isTraceEnabled()) {
      doTrace(format(pattern, args));
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
      doDebug(format(pattern, arg));
    }
  }

  @Override
  protected void doDebug(String pattern, @Nullable Object arg1, @Nullable Object arg2) {
    if (isDebugEnabled()) {
      doDebug(format(pattern, arg1, arg2));
    }
  }

  @Override
  protected void doDebug(String pattern, Object... args) {
    if (isDebugEnabled()) {
      doDebug(format(pattern, args));
    }
  }

  @Override
  protected void doInfo(String msg) {
    log("INFO ", msg);
  }

  @Override
  protected void doInfo(String pattern, @Nullable Object arg) {
    doInfo(format(pattern, arg));
  }

  @Override
  protected void doInfo(String pattern, @Nullable Object arg1, @Nullable Object arg2) {
    doInfo(format(pattern, arg1, arg2));
  }

  @Override
  protected void doInfo(String pattern, Object... args) {
    doInfo(format(pattern, args));
  }

  @Override
  protected void doWarn(String msg) {
    log("WARN ", msg);
  }

  @Override
  void doWarn(String msg, Throwable thrown) {
    doWarn(msg);
    thrown.printStackTrace();
  }

  @Override
  protected void doWarn(String pattern, @Nullable Object arg) {
    doWarn(format(pattern, arg));
  }

  @Override
  protected void doWarn(String pattern, @Nullable Object arg1, @Nullable Object arg2) {
    doWarn(format(pattern, arg1, arg2));
  }

  @Override
  protected void doWarn(String pattern, Object... args) {
    doWarn(format(pattern, args));
  }

  @Override
  protected void doError(String msg) {
    log("ERROR", msg);
  }

  @Override
  protected void doError(String pattern, @Nullable Object arg) {
    doError(format(pattern, arg));
  }

  @Override
  protected void doError(String pattern, @Nullable Object arg1, @Nullable Object arg2) {
    doError(format(pattern, arg1, arg2));
  }

  @Override
  protected void doError(String pattern, Object... args) {
    doError(format(pattern, args));
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

  @Override
  public LoggerLevel getLevel() {
    return Loggers.getFactory().getLevel();
  }

  private void log(String level, String msg) {
    this.stream.println(String.format("%s %s", level, msg));
  }
}
