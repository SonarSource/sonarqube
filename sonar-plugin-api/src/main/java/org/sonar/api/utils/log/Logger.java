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

/**
 * SonarQube plugins are not coupled with external logging libraries like SLF4J or Logback.
 *
 * Example:
 * <pre>
 * public class MyClass {
 *   private final Logger logger = Loggers.get("logger_name");
 *
 *   public void doSomething() {
 *     logger.info("something valuable for production environment");
 *     logger.warn("message with arguments {} and {}", "foo", 42);
 *   }
 * }
 * </pre>
 *
 * See {@link org.sonar.api.utils.log.LogTester} for testing facilities.
 * @since 5.1
 */
public interface Logger {

  boolean isDebugEnabled();

  /**
   * Logs a DEBUG level message. Debug messages must
   * be valuable for production environments and are not for development debugging.
   */
  void debug(String msg);

  void debug(String pattern, @Nullable Object arg);

  void debug(String msg, @Nullable Object arg1, @Nullable Object arg2);

  void debug(String msg, Object... args);

  /**
   * Logs an INFO level message.
   */
  void info(String msg);

  void info(String msg, @Nullable Object arg);

  void info(String msg, @Nullable Object arg1, @Nullable Object arg2);

  void info(String msg, Object... args);

  /**
   * Logs a WARN level message.
   */
  void warn(String msg);

  void warn(String msg, @Nullable Object arg);

  void warn(String msg, @Nullable Object arg1, @Nullable Object arg2);

  void warn(String msg, Object... args);

  /**
   * Logs an ERROR level message.
   */
  void error(String msg);

  void error(String msg, @Nullable Object arg);

  void error(String msg, @Nullable Object arg1, @Nullable Object arg2);

  void error(String msg, Object... args);

  /**
   * Logs an ERROR level message.
   */
  void error(String msg, Throwable thrown);
}
