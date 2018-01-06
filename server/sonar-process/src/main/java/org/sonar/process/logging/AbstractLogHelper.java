/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.process.logging;

import ch.qos.logback.classic.Level;
import java.util.Arrays;
import javax.annotation.CheckForNull;
import org.sonar.process.Props;

import static java.lang.String.format;

public abstract class AbstractLogHelper {
  static final Level[] ALLOWED_ROOT_LOG_LEVELS = new Level[] {Level.TRACE, Level.DEBUG, Level.INFO};
  static final String SONAR_LOG_LEVEL_PROPERTY = "sonar.log.level";
  static final String ROLLING_POLICY_PROPERTY = "sonar.log.rollingPolicy";
  static final String MAX_FILES_PROPERTY = "sonar.log.maxFiles";

  private static final String PROCESS_NAME_PLACEHOLDER = "XXXX";
  private static final String THREAD_ID_PLACEHOLDER = "ZZZZ";
  private static final String LOGGER_NAME_PLACEHOLDER = "YYYY";
  private static final String LOG_FORMAT = "%d{yyyy.MM.dd HH:mm:ss} %-5level " + PROCESS_NAME_PLACEHOLDER + "[" + THREAD_ID_PLACEHOLDER + "][YYYY] %msg%n";
  private final String loggerNamePattern;

  protected AbstractLogHelper(String loggerNamePattern) {
    this.loggerNamePattern = loggerNamePattern;
  }

  public abstract String getRootLoggerName();

  public String buildLogPattern(RootLoggerConfig config) {
    return LOG_FORMAT
      .replace(PROCESS_NAME_PLACEHOLDER, config.getProcessId().getKey())
      .replace(THREAD_ID_PLACEHOLDER, config.getThreadIdFieldPattern())
      .replace(LOGGER_NAME_PLACEHOLDER, loggerNamePattern);
  }

  /**
   * Resolve a log level reading the value of specified properties.
   * <p>
   * To compute the applied log level the following rules will be followed:
   * <ul>the last property with a defined and valid value in the order of the {@code propertyKeys} argument will be applied</ul>
   * <ul>if there is none, {@link Level#INFO INFO} will be returned</ul>
   * </p>
   *
   * @throws IllegalArgumentException if the value of the specified property is not one of {@link #ALLOWED_ROOT_LOG_LEVELS}
   */
  static Level resolveLevel(Props props, String... propertyKeys) {
    Level newLevel = Level.INFO;
    for (String propertyKey : propertyKeys) {
      Level level = getPropertyValueAsLevel(props, propertyKey);
      if (level != null) {
        newLevel = level;
      }
    }
    return newLevel;
  }

  @CheckForNull
  static Level getPropertyValueAsLevel(Props props, String propertyKey) {
    String value = props.value(propertyKey);
    if (value == null) {
      return null;
    }

    Level level = Level.toLevel(value, Level.INFO);
    if (!isAllowed(level)) {
      throw new IllegalArgumentException(format("log level %s in property %s is not a supported value (allowed levels are %s)",
        level, propertyKey, Arrays.toString(ALLOWED_ROOT_LOG_LEVELS)));
    }
    return level;
  }

  static boolean isAllowed(Level level) {
    for (Level allowedRootLogLevel : ALLOWED_ROOT_LOG_LEVELS) {
      if (level.equals(allowedRootLogLevel)) {
        return true;
      }
    }
    return false;
  }
}
