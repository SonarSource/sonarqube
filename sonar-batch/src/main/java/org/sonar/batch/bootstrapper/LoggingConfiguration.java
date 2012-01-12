/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.bootstrapper;

import org.apache.commons.lang.StringUtils;
import org.sonar.core.config.Logback;

import java.io.File;
import java.util.HashMap;
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
  public static final String LEVEL_SQL_VERBOSE = "DEBUG";
  public static final String LEVEL_SQL_DEFAULT = "WARN";

  public static final String FORMAT_DEFAULT = "%d{HH:mm:ss.SSS} %-5level - %msg%n";
  public static final String FORMAT_MAVEN = "[%level] [%d{HH:mm:ss.SSS}] %msg%n";

  private Map<String, String> substitutionVariables = new HashMap<String, String>();

  private LoggingConfiguration() {
    setVerbose(false);
    setShowSql(false);
    setFormat(FORMAT_DEFAULT);
  }

  public static LoggingConfiguration create() {
    return new LoggingConfiguration();
  }

  public LoggingConfiguration setProperties(Map<String, String> properties) {
    setShowSql("true".equals(properties.get("sonar.showSql")));
    setVerbose("true".equals(properties.get("sonar.verbose")));
    return this;
  }

  public LoggingConfiguration setVerbose(boolean verbose) {
    return setRootLevel(verbose ? LEVEL_ROOT_VERBOSE : LEVEL_ROOT_DEFAULT);
  }

  public LoggingConfiguration setShowSql(boolean showSql) {
    return setSqlLevel(showSql ? LEVEL_SQL_VERBOSE : LEVEL_SQL_DEFAULT);
  }

  public LoggingConfiguration setRootLevel(String level) {
    return addSubstitutionVariable(PROPERTY_ROOT_LOGGER_LEVEL, level);
  }

  public LoggingConfiguration setSqlLevel(String level) {
    return addSubstitutionVariable(PROPERTY_SQL_LOGGER_LEVEL, level);
  }

  public LoggingConfiguration setFormat(String format) {
    return addSubstitutionVariable(PROPERTY_FORMAT, StringUtils.defaultIfBlank(format, FORMAT_DEFAULT));
  }

  public LoggingConfiguration addSubstitutionVariable(String key, String value) {
    substitutionVariables.put(key, value);
    return this;
  }

  String getSubstitutionVariable(String key) {
    return substitutionVariables.get(key);
  }

  public LoggingConfiguration configure(String classloaderPath) {
    Logback.configure(classloaderPath, substitutionVariables);
    return this;
  }

  public LoggingConfiguration configure(File logbackFile) {
    Logback.configure(logbackFile, substitutionVariables);
    return this;
  }

  public LoggingConfiguration configure() {
    Logback.configure("/org/sonar/batch/bootstrapper/logback.xml", substitutionVariables);
    return this;
  }
}
