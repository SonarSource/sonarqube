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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.process.ProcessId;

import static java.util.Objects.requireNonNull;

public final class LogLevelConfig {
  private static final String SONAR_LOG_LEVEL_PROPERTY = "sonar.log.level";
  private static final String PROCESS_NAME_PLACEHOLDER = "XXXX";
  private static final String SONAR_PROCESS_LOG_LEVEL_PROPERTY = "sonar.log.level." + PROCESS_NAME_PLACEHOLDER;

  private final Map<String, List<String>> configuredByProperties;
  private final Map<String, Level> configuredByHardcodedLevel;
  private final Set<String> offUnlessTrace;
  private final String rootLoggerName;

  private LogLevelConfig(Builder builder) {
    this.configuredByProperties = Collections.unmodifiableMap(builder.configuredByProperties);
    this.configuredByHardcodedLevel = Collections.unmodifiableMap(builder.configuredByHardcodedLevel);
    this.offUnlessTrace = Collections.unmodifiableSet(builder.offUnlessTrace);
    this.rootLoggerName = builder.rootLoggerName;
  }

  Map<String, List<String>> getConfiguredByProperties() {
    return configuredByProperties;
  }

  Map<String, Level> getConfiguredByHardcodedLevel() {
    return configuredByHardcodedLevel;
  }

  Set<String> getOffUnlessTrace() {
    return offUnlessTrace;
  }

  String getRootLoggerName() {
    return rootLoggerName;
  }

  public static Builder newBuilder(String rootLoggerName) {
    return new Builder(rootLoggerName);
  }

  public static final class Builder {
    private final Map<String, List<String>> configuredByProperties = new HashMap<>();
    private final Map<String, Level> configuredByHardcodedLevel = new HashMap<>();
    private final Set<String> offUnlessTrace = new HashSet<>();
    private final String rootLoggerName;

    private Builder(String rootLoggerName) {
      this.rootLoggerName = requireNonNull(rootLoggerName, "rootLoggerName can't be null");
    }

    /**
     * Configure the log level of the root logger to be read from the value of properties {@link #SONAR_LOG_LEVEL_PROPERTY} and
     * {@link #SONAR_PROCESS_LOG_LEVEL_PROPERTY}.
     */
    public Builder rootLevelFor(ProcessId processId) {
      checkProcessId(processId);

      levelByProperty(rootLoggerName, SONAR_LOG_LEVEL_PROPERTY, SONAR_PROCESS_LOG_LEVEL_PROPERTY.replace(PROCESS_NAME_PLACEHOLDER, processId.getKey()));
      return this;
    }

    /**
     * Configure the log level of the logger with the specified name to be read from the value of properties
     * {@code sonar.log.level}, {@code sonar.log.level.[process_name]} and {@code sonar.log.level.[process_name].[LogDomain#getKey()]}.
     */
    public Builder levelByDomain(String loggerName, ProcessId processId, LogDomain domain) {
      checkLoggerName(loggerName);
      checkProcessId(processId);
      requireNonNull(domain, "LogDomain can't be null");
      String processProperty = SONAR_PROCESS_LOG_LEVEL_PROPERTY.replace(PROCESS_NAME_PLACEHOLDER, processId.getKey());
      levelByProperty(loggerName, SONAR_LOG_LEVEL_PROPERTY, processProperty, processProperty + "." + domain.getKey());
      return this;
    }

    private void levelByProperty(String loggerName, String property, String... otherProperties) {
      ensureUniqueConfiguration(loggerName);
      configuredByProperties.put(loggerName, Stream.concat(Stream.of(property), Arrays.stream(otherProperties)).collect(Collectors.toList()));
    }

    /**
     * Configure the log level of the logger with the specified name to be the specified one and it should never be
     * changed.
     */
    public Builder immutableLevel(String loggerName, Level level) {
      checkLoggerName(loggerName);
      requireNonNull(level, "level can't be null");
      ensureUniqueConfiguration(loggerName);
      configuredByHardcodedLevel.put(loggerName, level);
      return this;
    }

    private void ensureUniqueConfiguration(String loggerName) {
      if (configuredByProperties.containsKey(loggerName)) {
        throw new IllegalStateException("Configuration by property already registered for " + loggerName);
      }
      if (configuredByHardcodedLevel.containsKey(loggerName)) {
        throw new IllegalStateException("Configuration hardcoded level already registered for " + loggerName);
      }
      if (offUnlessTrace.contains(loggerName)) {
        throw new IllegalStateException("Configuration off unless TRACE already registered for " + loggerName);
      }
    }

    private static void checkProcessId(ProcessId processId) {
      requireNonNull(processId, "ProcessId can't be null");
    }

    private static void checkLoggerName(String loggerName) {
      requireNonNull(loggerName, "loggerName can't be null");
      if (loggerName.isEmpty()) {
        throw new IllegalArgumentException("loggerName can't be empty");
      }
    }

    public Builder offUnlessTrace(String loggerName) {
      checkLoggerName(loggerName);
      ensureUniqueConfiguration(loggerName);
      offUnlessTrace.add(loggerName);
      return this;
    }

    public LogLevelConfig build() {
      return new LogLevelConfig(this);
    }
  }
}
