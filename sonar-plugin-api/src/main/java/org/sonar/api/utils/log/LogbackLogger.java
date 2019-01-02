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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import javax.annotation.Nullable;

/**
 * Logback is used in production.
 */
class LogbackLogger extends BaseLogger {

  private final Logger logback;

  LogbackLogger(Logger logback) {
    this.logback = logback;
  }

  @Override
  public boolean isTraceEnabled() {
    return logback.isTraceEnabled();
  }

  @Override
  void doTrace(String msg) {
    logback.trace(msg);
  }

  @Override
  void doTrace(String msg, @Nullable Object arg) {
    logback.trace(msg, arg);
  }

  @Override
  void doTrace(String msg, @Nullable Object arg1, @Nullable Object arg2) {
    logback.trace(msg, arg1, arg2);
  }

  @Override
  void doTrace(String msg, Object... args) {
    logback.trace(msg, args);
  }

  @Override
  public boolean isDebugEnabled() {
    return logback.isDebugEnabled();
  }


  @Override
  protected void doDebug(String msg) {
    logback.debug(msg);
  }

  @Override
  protected void doDebug(String msg, @Nullable Object arg) {
    logback.debug(msg, arg);
  }

  @Override
  protected void doDebug(String msg, @Nullable Object arg1, @Nullable Object arg2) {
    logback.debug(msg, arg1, arg2);
  }

  @Override
  protected void doDebug(String msg, Object... args) {
    logback.debug(msg, args);
  }

  @Override
  protected void doInfo(String msg) {
    logback.info(msg);
  }

  @Override
  protected void doInfo(String msg, @Nullable Object arg) {
    logback.info(msg, arg);
  }

  @Override
  protected void doInfo(String msg, @Nullable Object arg1, @Nullable Object arg2) {
    logback.info(msg, arg1, arg2);
  }

  @Override
  protected void doInfo(String msg, Object... args) {
    logback.info(msg, args);
  }

  @Override
  protected void doWarn(String msg) {
    logback.warn(msg);
  }

  @Override
  void doWarn(String msg, Throwable thrown) {
    logback.warn(msg, thrown);
  }

  @Override
  protected void doWarn(String msg, @Nullable Object arg) {
    logback.warn(msg, arg);
  }

  @Override
  protected void doWarn(String msg, @Nullable Object arg1, @Nullable Object arg2) {
    logback.warn(msg, arg1, arg2);
  }

  @Override
  protected void doWarn(String msg, Object... args) {
    logback.warn(msg, args);
  }

  @Override
  protected void doError(String msg) {
    logback.error(msg);
  }

  @Override
  protected void doError(String msg, @Nullable Object arg) {
    logback.error(msg, arg);
  }

  @Override
  protected void doError(String msg, @Nullable Object arg1, @Nullable Object arg2) {
    logback.error(msg, arg1, arg2);
  }

  @Override
  protected void doError(String msg, Object... args) {
    logback.error(msg, args);
  }

  @Override
  protected void doError(String msg, Throwable thrown) {
    logback.error(msg, thrown);
  }

  @Override
  public boolean setLevel(LoggerLevel level) {
    switch (level) {
      case TRACE:
        logback.setLevel(Level.TRACE);
        break;
      case DEBUG:
        logback.setLevel(Level.DEBUG);
        break;
      case INFO:
        logback.setLevel(Level.INFO);
        break;
      case WARN:
        logback.setLevel(Level.WARN);
        break;
      case ERROR:
        logback.setLevel(Level.ERROR);
        break;
      default:
        throw new IllegalArgumentException("Only TRACE, DEBUG, INFO , WARN, ERROR logging levels are supported. Got: " + level);
    }
    return true;
  }

  @Override
  public LoggerLevel getLevel() {
    return LoggerLevel.valueOf(logback.getEffectiveLevel().levelStr);
  }

  Logger logbackLogger() {
    return logback;
  }
}
