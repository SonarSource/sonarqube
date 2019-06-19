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
package org.sonar.batch.bootstrapper;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;

/**
 * @since 2.14
 */
public final class LoggingConfiguration {

  public static final String PROPERTY_ROOT_LOGGER_LEVEL = "ROOT_LOGGER_LEVEL";
  /**
   * @deprecated since 5.2 there is no more db access from scanner side
   */
  @Deprecated
  public static final String PROPERTY_SQL_LOGGER_LEVEL = "SQL_LOGGER_LEVEL";

  public static final String PROPERTY_FORMAT = "FORMAT";

  public static final String LEVEL_ROOT_VERBOSE = "DEBUG";
  public static final String LEVEL_ROOT_DEFAULT = "INFO";

  static final String FORMAT_DEFAULT = "%d{HH:mm:ss.SSS} %-5level - %msg%n";
  static final String FORMAT_MAVEN = "[%level] [%d{HH:mm:ss.SSS}] %msg%n";

  private Map<String, String> substitutionVariables = new HashMap<>();
  private LogOutput logOutput = null;
  private boolean verbose;

  public LoggingConfiguration() {
    this(null);
  }

  public LoggingConfiguration(@Nullable EnvironmentInformation environment) {
    setVerbose(false);
    if (environment != null && "maven".equalsIgnoreCase(environment.getKey())) {
      setFormat(FORMAT_MAVEN);
    } else {
      setFormat(FORMAT_DEFAULT);
    }
  }

  public LoggingConfiguration setProperties(Map<String, String> properties) {
    setVerbose(properties);
    return this;
  }

  public LoggingConfiguration setLogOutput(@Nullable LogOutput listener) {
    this.logOutput = listener;
    return this;
  }

  public LoggingConfiguration setVerbose(boolean verbose) {
    return setRootLevel(verbose ? LEVEL_ROOT_VERBOSE : LEVEL_ROOT_DEFAULT);
  }

  public boolean isVerbose() {
    return verbose;
  }

  public LoggingConfiguration setVerbose(Map<String, String> props) {
    String logLevel = props.get("sonar.log.level");
    String deprecatedProfilingLevel = props.get("sonar.log.profilingLevel");
    verbose = "true".equals(props.get("sonar.verbose")) ||
      "DEBUG".equals(logLevel) || "TRACE".equals(logLevel) ||
      "BASIC".equals(deprecatedProfilingLevel) || "FULL".equals(deprecatedProfilingLevel);

    return setVerbose(verbose);
  }

  @CheckForNull
  private static String getFallback(String key, Map<String, String> properties, @Nullable Map<String, String> fallback) {
    if (properties.containsKey(key)) {
      return properties.get(key);
    }

    if (fallback != null) {
      return fallback.get(key);
    }

    return null;
  }

  public LoggingConfiguration setRootLevel(String level) {
    return addSubstitutionVariable(PROPERTY_ROOT_LOGGER_LEVEL, level);
  }

  LoggingConfiguration setFormat(String format) {
    return addSubstitutionVariable(PROPERTY_FORMAT, StringUtils.defaultIfBlank(format, FORMAT_DEFAULT));
  }

  public LoggingConfiguration addSubstitutionVariable(String key, String value) {
    substitutionVariables.put(key, value);
    return this;
  }

  String getSubstitutionVariable(String key) {
    return substitutionVariables.get(key);
  }

  Map<String, String> getSubstitutionVariables() {
    return substitutionVariables;
  }

  LogOutput getLogOutput() {
    return logOutput;
  }
}
