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
 * <p/>
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
 * <p/>
 * Message arguments are defined with <code>{}</code>, but not with {@link java.util.Formatter} syntax.
 *
 * <p/>
 * INFO, WARN and ERROR levels are always enabled. They can't be disabled by users.
 * DEBUG level can be enabled with properties <code>sonar.log.debug</code> (on server, see sonar.properties)
 * and <code>sonar.verbose</code> (on batch)
 * <p/>
 * See {@link org.sonar.api.utils.log.LogTester} for testing facilities.
 * @since 5.1
 */
public interface Logger {

  boolean isDebugEnabled();

  /**
   * Logs a DEBUG message. Debug messages must
   * be valuable for diagnosing production problems. They must not be used for development debugging.
   */
  void debug(String msg);

  /**
   * @see #debug(String) 
   */
  void debug(String pattern, @Nullable Object arg);

  /**
   * @see #debug(String)
   */
  void debug(String msg, @Nullable Object arg1, @Nullable Object arg2);

  /**
   * @see #debug(String)
   */
  void debug(String msg, Object... args);

  /**
   * Logs an INFO level message.
   */
  void info(String msg);

  /**
   * @see #info(String)
   */
  void info(String msg, @Nullable Object arg);

  /**
   * @see #info(String)
   */
  void info(String msg, @Nullable Object arg1, @Nullable Object arg2);

  /**
   * @see #info(String)
   */
  void info(String msg, Object... args);

  /**
   * Logs a WARN level message.
   */
  void warn(String msg);

  /**
   * @see #warn(String)
   */
  void warn(String msg, @Nullable Object arg);

  /**
   * @see #warn(String)
   */
  void warn(String msg, @Nullable Object arg1, @Nullable Object arg2);

  /**
   * @see #warn(String)
   */
  void warn(String msg, Object... args);

  /**
   * Logs an ERROR level message.
   */
  void error(String msg);

  /**
   * @see #error(String)
   */
  void error(String msg, @Nullable Object arg);

  /**
   * @see #error(String)
   */
  void error(String msg, @Nullable Object arg1, @Nullable Object arg2);

  /**
   * @see #error(String)
   */
  void error(String msg, Object... args);

  /**
   * @see #error(String)
   */
  void error(String msg, Throwable thrown);

  /**
   * Attempt to change logger level. Return true if it succeeded, false if
   * the underlying logging facility does not allow to change level at
   * runtime.
   * <p/>
   * This method must not be used to enable debug logs in tests. Use
   * {@link org.sonar.api.utils.log.LogTester#enableDebug(boolean)}.
   * <p/>
   * The standard use-case is to customize logging of embedded 3rd-party
   * libraries.
   */
  boolean setLevel(LoggerLevel level);
}
