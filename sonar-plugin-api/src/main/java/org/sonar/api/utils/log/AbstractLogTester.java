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

import java.util.List;

class AbstractLogTester<G extends AbstractLogTester> {

  protected void before() {
    // this shared instance breaks compatibility with parallel execution of tests
    LogInterceptors.set(new ListInterceptor());
    setLevel(LoggerLevel.INFO);
  }

  protected void after() {
    LogInterceptors.set(NullInterceptor.NULL_INSTANCE);
    setLevel(LoggerLevel.INFO);
  }

  LoggerLevel getLevel() {
    return Loggers.getFactory().getLevel();
  }

  /**
   * Enable/disable debug logs. Info, warn and error logs are always enabled.
   * By default INFO logs are enabled when LogTester is started.
   */
  public G setLevel(LoggerLevel level) {
    Loggers.getFactory().setLevel(level);
    return (G) this;
  }

  /**
   * Logs in chronological order (item at index 0 is the oldest one)
   */
  public List<String> logs() {
    return ((ListInterceptor) LogInterceptors.get()).logs();
  }

  /**
   * Logs in chronological order (item at index 0 is the oldest one) for
   * a given level
   */
  public List<String> logs(LoggerLevel level) {
    return ((ListInterceptor) LogInterceptors.get()).logs(level);
  }

  /**
   * Logs with arguments in chronological order (item at index 0 is the oldest one)
   */
  public List<LogAndArguments> getLogs() {
    return ((ListInterceptor) LogInterceptors.get()).getLogs();
  }

  /**
   * Logs with arguments in chronological order (item at index 0 is the oldest one) for
   * a given level
   */
  public List<LogAndArguments> getLogs(LoggerLevel level) {
    return ((ListInterceptor) LogInterceptors.get()).getLogs(level);
  }

  public G clear() {
    ((ListInterceptor) LogInterceptors.get()).clear();
    return (G) this;
  }
}
