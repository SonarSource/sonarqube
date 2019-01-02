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
import org.slf4j.LoggerFactory;

/**
 * Note that this is not "Slf4jLoggers" as there's a coupling on Logback
 * in order to change level of root logger.
 */
class LogbackLoggers extends Loggers {

  @Override
  protected Logger newInstance(String name) {
    // logback is accessed through SLF4J
    return new LogbackLogger((ch.qos.logback.classic.Logger) LoggerFactory.getLogger(name));
  }

  @Override
  protected LoggerLevel getLevel() {
    ch.qos.logback.classic.Logger logback = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    switch (logback.getLevel().levelInt) {
      case Level.TRACE_INT:
        return LoggerLevel.TRACE;
      case Level.DEBUG_INT:
        return LoggerLevel.DEBUG;
      default:
        return LoggerLevel.INFO;
    }
  }

  @Override
  protected void setLevel(LoggerLevel level) {
    ch.qos.logback.classic.Logger logback = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    new LogbackLogger(logback).setLevel(level);
  }
}
