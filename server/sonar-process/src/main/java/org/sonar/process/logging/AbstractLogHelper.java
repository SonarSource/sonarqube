/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

  public static final String PREFIX_LOG_FORMAT = "%d{yyyy.MM.dd HH:mm:ss} %-5level ";
  public static final String SUFFIX_LOG_FORMAT = " %msg%n";
  private final String loggerNamePattern;

  protected AbstractLogHelper(String loggerNamePattern) {
    this.loggerNamePattern = loggerNamePattern;
  }

  public abstract String getRootLoggerName();

  public String buildLogPattern(RootLoggerConfig config) {
    return PREFIX_LOG_FORMAT
      + (config.getNodeNameField().isBlank() ? "" : (config.getNodeNameField() + " "))
      + config.getProcessId().getKey()
      + "[" + config.getThreadIdFieldPattern() + "]"
      + "[" + loggerNamePattern + "]"
      + SUFFIX_LOG_FORMAT;
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
