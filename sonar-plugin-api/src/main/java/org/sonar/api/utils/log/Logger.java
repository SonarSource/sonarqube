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

import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * SonarQube plugins are not coupled with external logging libraries like SLF4J or Logback.
 * <p>
 * Example:
 * <pre>
 * public class MyClass {
 *   private static final Logger LOGGER = Loggers.get("logger_name");
 *
 *   public void doSomething() {
 *     LOGGER.info("something valuable for production environment");
 *     LOGGER.warn("message with arguments {} and {}", "foo", 42);
 *   }
 * }
 * </pre>
 * <p>
 * Message arguments are defined with <code>{}</code>, but not with {@link java.util.Formatter} syntax.
 *
 * <p>
 * INFO, WARN and ERROR levels are always enabled. They can't be disabled by users.
 * DEBUG and TRACE levels are enabled on demand with the property <code>sonar.log.level</code>.
 * <p>
 * See {@link org.sonar.api.utils.log.LogTester} for testing facilities.
 * @since 5.1
 */
public interface Logger {

  boolean isTraceEnabled();

  /**
   * Logs a TRACE message.
   * <p>
   * TRACE messages must
   * be valuable for diagnosing production problems. They must not be used for development debugging.
   * They can significantly slow down performances. The standard use-case is logging of
   * SQL and Elasticsearch requests.
   */
  void trace(String msg);

  /**
   * Logs a TRACE message, which is only to be constructed if the logging level
   * is such that the message will actually be logged.
   * <p>
   * TRACE messages must
   * be valuable for diagnosing production problems. They must not be used for development debugging.
   * They can significantly slow down performances. The standard use-case is logging of
   * SQL and Elasticsearch requests.
   * @param msgSupplier A function, which when called, produces the desired log message
   * @since 6.3
   */
  default void trace(Supplier<String> msgSupplier) {
    if (isTraceEnabled()) {
      trace(msgSupplier.get());
    }
  }

  /**
   * Logs an TRACE parameterized message according to the specified format and argument. Example:
   * <code>trace("Value is {}", value)</code>
   * <p>
   * TRACE messages must
   * be valuable for diagnosing production problems. They must not be used for development debugging.
   * They can significantly slow down performances. The standard use-case is logging of
   * SQL and Elasticsearch requests.
   */
  void trace(String pattern, @Nullable Object arg);

  /**
   * Logs an TRACE parameterized message according to the specified format and arguments. Example:
   * <code>trace("Values are {} and {}", value1, value2)</code>
   * <p>
   * TRACE messages must
   * be valuable for diagnosing production problems. They must not be used for development debugging.
   * They can significantly slow down performances. The standard use-case is logging of
   * SQL and Elasticsearch requests.
   */
  void trace(String msg, @Nullable Object arg1, @Nullable Object arg2);

  /**
   * Logs an TRACE parameterized message according to the specified format and arguments. Example:
   * <code>trace("Values are {} and {}", value1, value2)</code>
   * <p>
   * TRACE messages must
   * be valuable for diagnosing production problems. They must not be used for development debugging.
   * They can significantly slow down performances. The standard use-case is logging of
   * SQL and Elasticsearch requests.
   * <p>
   * This variant incurs the hidden cost of creating an Object[] before invoking the method.
   * The variants taking one and two arguments exist solely in order to avoid this hidden cost. See
   * {@link #trace(String, Object)} and {@link #trace(String, Object, Object)}
   */
  void trace(String msg, Object... args);

  boolean isDebugEnabled();

  /**
   * Logs a DEBUG message. 
   * <p>
   * DEBUG messages must
   * be valuable for diagnosing production problems. They must not be used for development debugging.
   */
  void debug(String msg);

  /**
   * Logs a DEBUG message which is only to be constructed if the logging level
   * is such that the message will actually be logged.
   * <p>
   * DEBUG messages must
   * be valuable for diagnosing production problems. They must not be used for development debugging.
   * @param msgSupplier A function, which when called, produces the desired log message
   * @since 6.3
   */
  default void debug(Supplier<String> msgSupplier) {
    if (isDebugEnabled()) {
      debug(msgSupplier.get());
    }
  }

  /**
   * Logs an DEBUG parameterized message according to the specified format and argument. Example:
   * <code>debug("Value is {}", value)</code>
   * <p>
   * Debug messages must
   * be valuable for diagnosing production problems. They must not be used for development debugging.
   */
  void debug(String pattern, @Nullable Object arg);

  /**
   * Logs an DEBUG parameterized message according to the specified format and arguments. Example:
   * <code>debug("Values are {} and {}", value1, value2)</code>
   * <p>
   * Debug messages must
   * be valuable for diagnosing production problems. They must not be used for development debugging.
   */
  void debug(String msg, @Nullable Object arg1, @Nullable Object arg2);

  /**
   * Logs an DEBUG parameterized message according to the specified format and arguments. Example:
   * <code>debug("Values are {}, {} and {}", value1, value2, value3)</code>
   * <p>
   * Debug messages must
   * be valuable for diagnosing production problems. They must not be used for development debugging.
   * * <p>
   * This variant incurs the hidden cost of creating an Object[] before invoking the method.
   * The variants taking one and two arguments exist solely in order to avoid this hidden cost. See
   * {@link #debug(String, Object)} and {@link #debug(String, Object, Object)}
   */
  void debug(String msg, Object... args);

  /**
   * Logs an INFO level message.
   */
  void info(String msg);

  /**
   * Logs an INFO parameterized message according to the specified format and argument. Example:
   * <code>info("Value is {}", value)</code>
   */
  void info(String msg, @Nullable Object arg);

  /**
   * Logs an INFO parameterized message according to the specified format and arguments. Example:
   * <code>info("Values are {} and {}", value1, value2)</code>
   */
  void info(String msg, @Nullable Object arg1, @Nullable Object arg2);

  /**
   * Logs an INFO parameterized message according to the specified format and arguments. Example:
   * <code>info("Values are {}, {} and {}", value1, value2, value3)</code>
   * <p>
   * This variant incurs the hidden cost of creating an Object[] before invoking the method.
   * The variants taking one and two arguments exist solely in order to avoid this hidden cost. See
   * {@link #info(String, Object)} and {@link #info(String, Object, Object)}
   */
  void info(String msg, Object... args);

  /**
   * Logs a WARN level message.
   */
  void warn(String msg);

  /**
   * Logs an exception at the WARN level with an accompanying message.
   */
  void warn(String msg, Throwable throwable);

  /**
   * Logs a WARN parameterized message according to the specified format and argument. Example:
   * <code>warn("Value is {}", value)</code>
   */
  void warn(String msg, @Nullable Object arg);

  /**
   * Logs a WARN parameterized message according to the specified format and arguments. Example:
   * <code>warn("Values are {} and {}", value1, value2)</code>
   */
  void warn(String msg, @Nullable Object arg1, @Nullable Object arg2);

  /**
   * Logs a WARN parameterized message according to the specified format and arguments. Example:
   * <code>warn("Values are {}, {} and {}", value1, value2, value3)</code>
   * <p>
   * This variant incurs the hidden cost of creating an Object[] before invoking the method.
   * The variants taking one and two arguments exist solely in order to avoid this hidden cost. See
   * {@link #warn(String, Object)} and {@link #warn(String, Object, Object)}
   */
  void warn(String msg, Object... args);

  /**
   * Logs an ERROR level message.
   */
  void error(String msg);

  /**
   * Logs an ERROR parameterized message according to the specified format and argument. Example:
   * <code>error("Value is {}", value)</code>
   */
  void error(String msg, @Nullable Object arg);

  /**
   * Logs a ERROR parameterized message according to the specified format and arguments. Example:
   * <code>error("Values are {} and {}", value1, value2)</code>
   */
  void error(String msg, @Nullable Object arg1, @Nullable Object arg2);

  /**
   * Logs a ERROR parameterized message according to the specified format and arguments. Example:
   * <code>error("Values are {}, {} and {}", value1, value2, value3)</code>
   * <p>
   * This variant incurs the hidden cost of creating an Object[] before invoking the method.
   * The variants taking one and two arguments exist solely in order to avoid this hidden cost. See
   * {@link #error(String, Object)} and {@link #error(String, Object, Object)}
   */
  void error(String msg, Object... args);

  /**
   * Logs an exception at the ERROR level with an accompanying message.
   */
  void error(String msg, Throwable thrown);

  /**
   * Attempt to change logger level. Return true if it succeeded, false if
   * the underlying logging facility does not allow to change level at
   * runtime.
   * <p>
   * This method must not be used to enable DEBUG or TRACE logs in tests. Use
   * {@link org.sonar.api.utils.log.LogTester#setLevel(LoggerLevel)} instead.
   * <p>
   * The standard use-case is to customize logging of embedded 3rd-party
   * libraries.
   */
  boolean setLevel(LoggerLevel level);

  LoggerLevel getLevel();
}
