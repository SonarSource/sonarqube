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
package org.sonar.process.logging;

import ch.qos.logback.classic.Level;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.sonar.process.MessageException;
import org.sonar.process.Props;

import static java.lang.String.format;
import static java.lang.String.valueOf;

public class Log4JPropertiesBuilder extends AbstractLogHelper {
  private static final String ROOT_LOGGER_NAME = "rootLogger";
  private static final int UNLIMITED_MAX_FILES = 100_000;

  private final Properties log4j2Properties = new Properties();
  private final Props props;

  public Log4JPropertiesBuilder(Props props) {
    super("%logger{1.}");
    this.props = Objects.requireNonNull(props, "Props can't be null");
    internalLogLevel(Level.ERROR);
  }

  @Override
  public String getRootLoggerName() {
    return ROOT_LOGGER_NAME;
  }

  public Properties get() {
    Properties res = new Properties();
    res.putAll(log4j2Properties);
    return res;
  }

  public void internalLogLevel(Level level) {
    putProperty("status", level.toString());
  }

  private void putProperty(String key, String value) {
    log4j2Properties.put(key, value);
  }

  /**
   * Make log4j2 configuration for a process to push all its logs to a log file.
   * <p>
   * <ul>
   * <li>the file's name will use the prefix defined in {@link RootLoggerConfig#getProcessId()#getLogFilenamePrefix()}.</li>
   * <li>the file will follow the rotation policy defined in property {@link #ROLLING_POLICY_PROPERTY} and
   * the max number of files defined in property {@link #MAX_FILES_PROPERTY}</li>
   * <li>the logs will follow the specified log pattern</li>
   * </ul>
   * </p>
   *
   * @see #buildLogPattern(RootLoggerConfig)
   */
  public void configureGlobalFileLog(RootLoggerConfig config, File logDir, String logPattern) {
    String appenderRef = writeFileAppender(config, logDir, logPattern);

    putProperty(ROOT_LOGGER_NAME + ".appenderRef." + appenderRef + ".ref", appenderRef);
  }

  private String writeFileAppender(RootLoggerConfig config, File logDir, String logPattern) {
    String appenderName = "file_" + config.getProcessId().getLogFilenamePrefix();
    RollingPolicy rollingPolicy = createRollingPolicy(logDir, config.getProcessId().getLogFilenamePrefix());
    Appender appender = new Appender(appenderName, rollingPolicy, logPattern);
    appender.writeAppenderProperties();
    return appender.getAppenderRef();
  }

  private RollingPolicy createRollingPolicy(File logDir, String filenamePrefix) {
    String rollingPolicy = props.value(ROLLING_POLICY_PROPERTY, "time:yyyy-MM-dd");
    int maxFiles = props.valueAsInt(MAX_FILES_PROPERTY, 7);
    if (maxFiles <= 0) {
      maxFiles = UNLIMITED_MAX_FILES;
    }

    if (rollingPolicy.startsWith("time:")) {
      return new TimeRollingPolicy(filenamePrefix, logDir, maxFiles, StringUtils.substringAfter(rollingPolicy, "time:"));
    } else if (rollingPolicy.startsWith("size:")) {
      return new SizeRollingPolicy(filenamePrefix, logDir, maxFiles, StringUtils.substringAfter(rollingPolicy, "size:"));
    } else if ("none".equals(rollingPolicy)) {
      return new NoRollingPolicy(filenamePrefix, logDir);
    } else {
      throw new MessageException(format("Unsupported value for property %s: %s", ROLLING_POLICY_PROPERTY, rollingPolicy));
    }
  }

  public void apply(LogLevelConfig logLevelConfig) {
    if (!ROOT_LOGGER_NAME.equals(logLevelConfig.getRootLoggerName())) {
      throw new IllegalArgumentException("Value of LogLevelConfig#rootLoggerName must be \"" + ROOT_LOGGER_NAME + "\"");
    }

    Level propertyValueAsLevel = getPropertyValueAsLevel(props, SONAR_LOG_LEVEL_PROPERTY);
    boolean traceGloballyEnabled = propertyValueAsLevel == Level.TRACE;

    List<String> loggerNames = Stream.of(
      logLevelConfig.getConfiguredByProperties().keySet().stream(),
      logLevelConfig.getConfiguredByHardcodedLevel().keySet().stream(),
      logLevelConfig.getOffUnlessTrace().stream().filter(k -> !traceGloballyEnabled))
      .flatMap(s -> s)
      .filter(loggerName -> !ROOT_LOGGER_NAME.equals(loggerName))
      .distinct()
      .sorted()
      .collect(Collectors.toList());
    if (!loggerNames.isEmpty()) {
      putProperty("loggers", loggerNames.stream().collect(Collectors.joining(",")));
    }

    logLevelConfig.getConfiguredByProperties().forEach((loggerName, value) -> applyLevelByProperty(props, loggerName, value));
    logLevelConfig.getConfiguredByHardcodedLevel().forEach(this::applyHardcodedLevel);
    logLevelConfig.getOffUnlessTrace().stream().filter(k -> !traceGloballyEnabled).forEach(logger -> applyHardcodedLevel(logger, Level.OFF));
  }

  private void applyLevelByProperty(Props props, String loggerKey, List<String> properties) {
    putLevel(loggerKey, resolveLevel(props, properties.stream().toArray(String[]::new)));
  }

  private void applyHardcodedLevel(String loggerName, Level newLevel) {
    putLevel(loggerName, newLevel);
  }

  private void putLevel(String loggerName, Level level) {
    if (loggerName.equals(ROOT_LOGGER_NAME)) {
      putProperty(loggerName + ".level", level.toString());
    } else {
      putProperty("logger." + loggerName + ".name", loggerName);
      putProperty("logger." + loggerName + ".level", level.toString());
    }
  }

  private class Appender {
    private final String prefix;
    private final String appenderName;
    private final RollingPolicy rollingPolicy;
    private final String logPattern;

    private Appender(String appenderName, RollingPolicy rollingPolicy, String logPattern) {
      this.prefix = "appender." + appenderName + ".";
      this.appenderName = appenderName;
      this.rollingPolicy = rollingPolicy;
      this.logPattern = logPattern;
    }

    void writeAppenderProperties() {
      put("name", appenderName);
      put("layout.type", "PatternLayout");
      put("layout.pattern", logPattern);

      rollingPolicy.writePolicy(this.prefix);
    }

    void put(String key, String value) {
      Log4JPropertiesBuilder.this.putProperty(this.prefix + key, value);
    }

    String getAppenderRef() {
      return appenderName;
    }
  }

  private abstract class RollingPolicy {
    final String filenamePrefix;
    final File logsDir;

    RollingPolicy(String filenamePrefix, File logsDir) {
      this.filenamePrefix = filenamePrefix;
      this.logsDir = logsDir;
    }

    abstract void writePolicy(String propertyPrefix);

    void writeTypeProperty(String propertyPrefix, String type) {
      putProperty(propertyPrefix + "type", type);
    }

    void writeFileNameProperty(String propertyPrefix) {
      putProperty(propertyPrefix + "fileName", new File(logsDir, filenamePrefix + ".log").getAbsolutePath());
    }

    void writeFilePatternProperty(String propertyPrefix, String pattern) {
      putProperty(propertyPrefix + "filePattern", new File(logsDir, filenamePrefix + "." + pattern + ".log").getAbsolutePath());
    }

  }

  /**
   * Log files are not rotated, for example when unix command logrotate is in place.
   */
  private class NoRollingPolicy extends RollingPolicy {
    private NoRollingPolicy(String filenamePrefix, File logsDir) {
      super(filenamePrefix, logsDir);
    }

    @Override
    public void writePolicy(String propertyPrefix) {
      writeTypeProperty(propertyPrefix, "File");
      writeFileNameProperty(propertyPrefix);
    }
  }

  private class TimeRollingPolicy extends RollingPolicy {
    private final String datePattern;
    private final int maxFiles;

    private TimeRollingPolicy(String filenamePrefix, File logsDir, int maxFiles, String datePattern) {
      super(filenamePrefix, logsDir);
      this.datePattern = datePattern;
      this.maxFiles = maxFiles;
    }

    @Override
    public void writePolicy(String propertyPrefix) {
      writeTypeProperty(propertyPrefix, "RollingFile");
      writeFileNameProperty(propertyPrefix);
      writeFilePatternProperty(propertyPrefix, "%d{" + datePattern + "}");

      putProperty(propertyPrefix + "policies.type", "Policies");
      putProperty(propertyPrefix + "policies.time.type", "TimeBasedTriggeringPolicy");
      putProperty(propertyPrefix + "policies.time.interval", "1");
      putProperty(propertyPrefix + "policies.time.modulate", "true");

      putProperty(propertyPrefix + "strategy.type", "DefaultRolloverStrategy");
      putProperty(propertyPrefix + "strategy.fileIndex", "nomax");
      putProperty(propertyPrefix + "strategy.action.type", "Delete");
      putProperty(propertyPrefix + "strategy.action.basepath", logsDir.getAbsolutePath());
      putProperty(propertyPrefix + "strategy.action.maxDepth", valueOf(1));
      putProperty(propertyPrefix + "strategy.action.condition.type", "IfFileName");
      putProperty(propertyPrefix + "strategy.action.condition.glob", filenamePrefix + "*");
      putProperty(propertyPrefix + "strategy.action.condition.nested_condition.type", "IfAccumulatedFileCount");
      putProperty(propertyPrefix + "strategy.action.condition.nested_condition.exceeds", valueOf(maxFiles));
    }
  }

  private class SizeRollingPolicy extends RollingPolicy {
    private final String size;
    private final int maxFiles;

    private SizeRollingPolicy(String filenamePrefix, File logsDir, int maxFiles, String size) {
      super(filenamePrefix, logsDir);
      this.size = size;
      this.maxFiles = maxFiles;
    }

    @Override
    public void writePolicy(String propertyPrefix) {
      writeTypeProperty(propertyPrefix, "RollingFile");
      writeFileNameProperty(propertyPrefix);
      writeFilePatternProperty(propertyPrefix, "%i");

      putProperty(propertyPrefix + "policies.type", "Policies");
      putProperty(propertyPrefix + "policies.size.type", "SizeBasedTriggeringPolicy");
      putProperty(propertyPrefix + "policies.size.size", size);

      putProperty(propertyPrefix + "strategy.type", "DefaultRolloverStrategy");
      putProperty(propertyPrefix + "strategy.max", valueOf(maxFiles));
    }
  }
}
