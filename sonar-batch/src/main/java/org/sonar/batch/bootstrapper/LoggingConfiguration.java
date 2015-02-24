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
package org.sonar.batch.bootstrapper;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.core.config.Logback;

import javax.annotation.Nullable;

import java.io.File;
import java.util.Map;

/**
 * @since 2.14
 */
public final class LoggingConfiguration {

  public static final String PROPERTY_ROOT_LOGGER_LEVEL = "ROOT_LOGGER_LEVEL";
  public static final String PROPERTY_SQL_LOGGER_LEVEL = "SQL_LOGGER_LEVEL";
  public static final String PROPERTY_FORMAT = "FORMAT";
  public static final String LEVEL_ROOT_VERBOSE = "DEBUG";

  public static final String LEVEL_ROOT_DEFAULT = "INFO";
  @VisibleForTesting
  static final String FORMAT_DEFAULT = "%d{HH:mm:ss.SSS} %-5level - %msg%n";
  @VisibleForTesting
  static final String FORMAT_MAVEN = "[%level] [%d{HH:mm:ss.SSS}] %msg%n";

  private Map<String, String> substitutionVariables = Maps.newHashMap();

  private LoggingConfiguration(@Nullable EnvironmentInformation environment) {
    setVerbose(false);
    if (environment != null && "maven".equalsIgnoreCase(environment.getKey())) {
      setFormat(FORMAT_MAVEN);
    } else {
      setFormat(FORMAT_DEFAULT);
    }
  }

  static LoggingConfiguration create(@Nullable EnvironmentInformation environment) {
    return new LoggingConfiguration(environment);
  }

  public LoggingConfiguration setProperties(Map<String, String> properties) {
    String logLevel = properties.get("sonar.log.level");
    String deprecatedProfilingLevel = properties.get("sonar.log.profilingLevel");
    boolean verbose = "true".equals(properties.get("sonar.verbose")) ||
      "DEBUG".equals(logLevel) || "TRACE".equals(logLevel) ||
      "BASIC".equals(deprecatedProfilingLevel) || "FULL".equals(deprecatedProfilingLevel);
    boolean sql = "TRACE".equals(logLevel) || "FULL".equals(deprecatedProfilingLevel);

    setShowSql(sql);
    setVerbose(verbose);
    return this;
  }

  public LoggingConfiguration setVerbose(boolean verbose) {
    return setRootLevel(verbose ? LEVEL_ROOT_VERBOSE : LEVEL_ROOT_DEFAULT);
  }

  public LoggingConfiguration setRootLevel(String level) {
    return addSubstitutionVariable(PROPERTY_ROOT_LOGGER_LEVEL, level);
  }

  public LoggingConfiguration setShowSql(boolean showSql) {
    return addSubstitutionVariable(PROPERTY_SQL_LOGGER_LEVEL, showSql ? "TRACE" : "WARN");
  }

  @VisibleForTesting
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

  LoggingConfiguration configure(String classloaderPath) {
    Logback.configure(classloaderPath, substitutionVariables);
    return this;
  }

  LoggingConfiguration configure(File logbackFile) {
    Logback.configure(logbackFile, substitutionVariables);
    return this;
  }

  LoggingConfiguration configure() {
    Logback.configure("/org/sonar/batch/bootstrapper/logback.xml", substitutionVariables);
    return this;
  }
}
