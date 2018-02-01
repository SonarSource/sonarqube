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
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.jul.LevelChangePropagator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.LogManager;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.sonar.process.MessageException;
import org.sonar.process.Props;

import static java.lang.String.format;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static org.sonar.process.ProcessProperties.Property.PATH_LOGS;

/**
 * Helps to configure Logback in a programmatic way, without using XML.
 */
public class LogbackHelper extends AbstractLogHelper {

  private static final String ALL_LOGS_TO_CONSOLE_PROPERTY = "sonar.log.console";
  private static final String LOGBACK_LOGGER_NAME_PATTERN = "%logger{20}";

  public LogbackHelper() {
    super(LOGBACK_LOGGER_NAME_PATTERN);
  }

  public static Collection<Level> allowedLogLevels() {
    return Arrays.asList(ALLOWED_ROOT_LOG_LEVELS);
  }

  @Override
  public String getRootLoggerName() {
    return ROOT_LOGGER_NAME;
  }

  public LoggerContext getRootContext() {
    org.slf4j.Logger logger;
    while (!((logger = LoggerFactory.getLogger(ROOT_LOGGER_NAME)) instanceof Logger)) {
      // It occurs when the initialization step is still not finished because of a race condition
      // on ILoggerFactory.getILoggerFactory
      // http://jira.qos.ch/browse/SLF4J-167
      // Substitute loggers are used.
      // http://www.slf4j.org/codes.html#substituteLogger
      // Bug is not fixed in SLF4J 1.7.14.
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    return ((Logger) logger).getLoggerContext();
  }

  public LoggerContextListener enableJulChangePropagation(LoggerContext loggerContext) {
    LogManager.getLogManager().reset();
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();

    LevelChangePropagator propagator = new LevelChangePropagator();
    propagator.setContext(loggerContext);
    propagator.setResetJUL(true);
    propagator.start();
    loggerContext.addListener(propagator);
    return propagator;
  }

  /**
   * Applies the specified {@link LogLevelConfig} reading the specified {@link Props}.
   *
   * @throws IllegalArgumentException if the any level specified in a property is not one of {@link #ALLOWED_ROOT_LOG_LEVELS}
   */
  public LoggerContext apply(LogLevelConfig logLevelConfig, Props props) {
    if (!ROOT_LOGGER_NAME.equals(logLevelConfig.getRootLoggerName())) {
      throw new IllegalArgumentException("Value of LogLevelConfig#rootLoggerName must be \"" + ROOT_LOGGER_NAME + "\"");
    }

    LoggerContext rootContext = getRootContext();
    logLevelConfig.getConfiguredByProperties().forEach((key, value) -> applyLevelByProperty(props, rootContext.getLogger(key), value));
    logLevelConfig.getConfiguredByHardcodedLevel().forEach((key, value) -> applyHardcodedLevel(rootContext, key, value));
    Level propertyValueAsLevel = getPropertyValueAsLevel(props, SONAR_LOG_LEVEL_PROPERTY);
    boolean traceGloballyEnabled = propertyValueAsLevel == Level.TRACE;
    logLevelConfig.getOffUnlessTrace().forEach(logger -> applyHardUnlessTrace(rootContext, logger, traceGloballyEnabled));
    return rootContext;
  }

  private void applyLevelByProperty(Props props, Logger logger, List<String> properties) {
    logger.setLevel(resolveLevel(props, properties.stream().toArray(String[]::new)));
  }

  private static void applyHardcodedLevel(LoggerContext rootContext, String loggerName, Level newLevel) {
    rootContext.getLogger(loggerName).setLevel(newLevel);
  }

  private static void applyHardUnlessTrace(LoggerContext rootContext, String logger, boolean traceGloballyEnabled) {
    if (!traceGloballyEnabled) {
      rootContext.getLogger(logger).setLevel(Level.OFF);
    }
  }

  public void changeRoot(LogLevelConfig logLevelConfig, Level newLevel) {
    ensureSupportedLevel(newLevel);
    LoggerContext rootContext = getRootContext();
    rootContext.getLogger(ROOT_LOGGER_NAME).setLevel(newLevel);
    logLevelConfig.getConfiguredByProperties().forEach((key, value) -> rootContext.getLogger(key).setLevel(newLevel));
  }

  private static void ensureSupportedLevel(Level newLevel) {
    if (!isAllowed(newLevel)) {
      throw new IllegalArgumentException(format("%s log level is not supported (allowed levels are %s)", newLevel, Arrays.toString(ALLOWED_ROOT_LOG_LEVELS)));
    }
  }

  /**
   * Creates a new {@link ConsoleAppender} to {@code System.out} with the specified name and log pattern.
   *
   * @see #buildLogPattern(RootLoggerConfig)
   */
  public ConsoleAppender<ILoggingEvent> newConsoleAppender(Context loggerContext, String name, String logPattern) {
    PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
    consoleEncoder.setContext(loggerContext);
    consoleEncoder.setPattern(logPattern);
    consoleEncoder.start();
    ConsoleAppender<ILoggingEvent> consoleAppender = new ConsoleAppender<>();
    consoleAppender.setContext(loggerContext);
    consoleAppender.setEncoder(consoleEncoder);
    consoleAppender.setName(name);
    consoleAppender.setTarget("System.out");
    consoleAppender.start();
    return consoleAppender;
  }

  /**
   * Make logback configuration for a process to push all its logs to a log file.
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
  public FileAppender<ILoggingEvent> configureGlobalFileLog(Props props, RootLoggerConfig config, String logPattern) {
    LoggerContext ctx = getRootContext();
    Logger rootLogger = ctx.getLogger(ROOT_LOGGER_NAME);
    FileAppender<ILoggingEvent> fileAppender = newFileAppender(ctx, props, config, logPattern);
    rootLogger.addAppender(fileAppender);
    return fileAppender;
  }

  public FileAppender<ILoggingEvent> newFileAppender(LoggerContext ctx, Props props, RootLoggerConfig config, String logPattern) {
    RollingPolicy rollingPolicy = createRollingPolicy(ctx, props, config.getProcessId().getLogFilenamePrefix());
    FileAppender<ILoggingEvent> fileAppender = rollingPolicy.createAppender("file_" + config.getProcessId().getLogFilenamePrefix());
    fileAppender.setContext(ctx);
    PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
    fileEncoder.setContext(ctx);
    fileEncoder.setPattern(logPattern);
    fileEncoder.start();
    fileAppender.setEncoder(fileEncoder);
    fileAppender.start();
    return fileAppender;
  }

  /**
   * Make the logback configuration for a sub process to correctly push all its logs to be read by a stream gobbler
   * on the sub process's System.out.
   *
   * @see #buildLogPattern(RootLoggerConfig)
   */
  public void configureForSubprocessGobbler(Props props, String logPattern) {
    if (isAllLogsToConsoleEnabled(props)) {
      LoggerContext ctx = getRootContext();
      ctx.getLogger(ROOT_LOGGER_NAME).addAppender(newConsoleAppender(ctx, "root_console", logPattern));
    }
  }

  /**
   * Finds out whether we are in testing environment (usually ITs) and logs of all processes must be forward to
   * App's System.out. This is specified by the value of property {@link #ALL_LOGS_TO_CONSOLE_PROPERTY}.
   */
  public boolean isAllLogsToConsoleEnabled(Props props) {
    return props.valueAsBoolean(ALL_LOGS_TO_CONSOLE_PROPERTY, false);
  }

  public Level getLoggerLevel(String loggerName) {
    return getRootContext().getLogger(loggerName).getLevel();
  }

  /**
   * Generally used to reset logback in logging tests
   */
  public void resetFromXml(String xmlResourcePath) throws JoranException {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator configurator = new JoranConfigurator();
    configurator.setContext(context);
    context.reset();
    configurator.doConfigure(LogbackHelper.class.getResource(xmlResourcePath));
  }

  public RollingPolicy createRollingPolicy(Context ctx, Props props, String filenamePrefix) {
    String rollingPolicy = props.value(ROLLING_POLICY_PROPERTY, "time:yyyy-MM-dd");
    int maxFiles = props.valueAsInt(MAX_FILES_PROPERTY, 7);
    File logsDir = props.nonNullValueAsFile(PATH_LOGS.getKey());

    if (rollingPolicy.startsWith("time:")) {
      return new TimeRollingPolicy(ctx, filenamePrefix, logsDir, maxFiles, StringUtils.substringAfter(rollingPolicy, "time:"));

    } else if (rollingPolicy.startsWith("size:")) {
      return new SizeRollingPolicy(ctx, filenamePrefix, logsDir, maxFiles, StringUtils.substringAfter(rollingPolicy, "size:"));

    } else if ("none".equals(rollingPolicy)) {
      return new NoRollingPolicy(ctx, filenamePrefix, logsDir, maxFiles);

    } else {
      throw new MessageException(format("Unsupported value for property %s: %s", ROLLING_POLICY_PROPERTY, rollingPolicy));
    }
  }

  public abstract static class RollingPolicy {
    protected final Context context;
    final String filenamePrefix;
    final File logsDir;
    final int maxFiles;

    RollingPolicy(Context context, String filenamePrefix, File logsDir, int maxFiles) {
      this.context = context;
      this.filenamePrefix = filenamePrefix;
      this.logsDir = logsDir;
      this.maxFiles = maxFiles;
    }

    public abstract FileAppender<ILoggingEvent> createAppender(String appenderName);
  }

  /**
   * Log files are not rotated, for example when unix command logrotate is in place.
   */
  private static class NoRollingPolicy extends RollingPolicy {
    private NoRollingPolicy(Context context, String filenamePrefix, File logsDir, int maxFiles) {
      super(context, filenamePrefix, logsDir, maxFiles);
    }

    @Override
    public FileAppender<ILoggingEvent> createAppender(String appenderName) {
      FileAppender<ILoggingEvent> appender = new FileAppender<>();
      appender.setContext(context);
      appender.setFile(new File(logsDir, filenamePrefix + ".log").getAbsolutePath());
      appender.setName(appenderName);
      return appender;
    }
  }

  /**
   * Log files are rotated according to time (one file per day, month or year).
   * See http://logback.qos.ch/manual/appenders.html#TimeBasedRollingPolicy
   */
  private static class TimeRollingPolicy extends RollingPolicy {
    private final String datePattern;

    private TimeRollingPolicy(Context context, String filenamePrefix, File logsDir, int maxFiles, String datePattern) {
      super(context, filenamePrefix, logsDir, maxFiles);
      this.datePattern = datePattern;
    }

    @Override
    public FileAppender<ILoggingEvent> createAppender(String appenderName) {
      RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
      appender.setContext(context);
      appender.setName(appenderName);
      String filePath = new File(logsDir, filenamePrefix + ".log").getAbsolutePath();
      appender.setFile(filePath);

      TimeBasedRollingPolicy rollingPolicy = new TimeBasedRollingPolicy();
      rollingPolicy.setContext(context);
      rollingPolicy.setFileNamePattern(StringUtils.replace(filePath, filenamePrefix + ".log", filenamePrefix + ".%d{" + datePattern + "}.log"));
      rollingPolicy.setMaxHistory(maxFiles);
      rollingPolicy.setParent(appender);
      rollingPolicy.start();
      appender.setRollingPolicy(rollingPolicy);

      return appender;
    }
  }

  /**
   * Log files are rotated according to their size.
   * See http://logback.qos.ch/manual/appenders.html#FixedWindowRollingPolicy
   */
  private static class SizeRollingPolicy extends RollingPolicy {
    private final String size;

    private SizeRollingPolicy(Context context, String filenamePrefix, File logsDir, int maxFiles, String parameter) {
      super(context, filenamePrefix, logsDir, maxFiles);
      this.size = parameter;
    }

    @Override
    public FileAppender<ILoggingEvent> createAppender(String appenderName) {
      RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
      appender.setContext(context);
      appender.setName(appenderName);
      String filePath = new File(logsDir, filenamePrefix + ".log").getAbsolutePath();
      appender.setFile(filePath);

      SizeBasedTriggeringPolicy<ILoggingEvent> trigger = new SizeBasedTriggeringPolicy<>();
      trigger.setMaxFileSize(FileSize.valueOf(size));
      trigger.setContext(context);
      trigger.start();
      appender.setTriggeringPolicy(trigger);

      FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
      rollingPolicy.setContext(context);
      rollingPolicy.setFileNamePattern(StringUtils.replace(filePath, filenamePrefix + ".log", filenamePrefix + ".%i.log"));
      rollingPolicy.setMinIndex(1);
      rollingPolicy.setMaxIndex(maxFiles);
      rollingPolicy.setParent(appender);
      rollingPolicy.start();
      appender.setRollingPolicy(rollingPolicy);

      return appender;
    }
  }

}
