/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.process;

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
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import java.io.File;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;

/**
 * Helps to configure Logback in a programmatic way, without using XML.
 */
public class LogbackHelper {

  public static final String ROLLING_POLICY_PROPERTY = "sonar.log.rollingPolicy";
  public static final String MAX_FILES_PROPERTY = "sonar.log.maxFiles";
  private static final String THREAD_ID_PLACEHOLDER = "ZZZZ";
  private static final String LOG_FORMAT = "%d{yyyy.MM.dd HH:mm:ss} %-5level [" + THREAD_ID_PLACEHOLDER + "][%logger{20}] %msg%n";

  public LoggerContext getRootContext() {
    org.slf4j.Logger logger;
    while (!((logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)) instanceof Logger)) {
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
    LevelChangePropagator propagator = new LevelChangePropagator();
    propagator.setContext(loggerContext);
    propagator.start();
    loggerContext.addListener(propagator);
    return propagator;
  }

  public void configureRootLogger(LoggerContext ctx, Props props, String threadIdFieldPattern, String fileName) {
    String logFormat = LOG_FORMAT.replace(THREAD_ID_PLACEHOLDER, threadIdFieldPattern);
    // configure appender
    LogbackHelper.RollingPolicy rollingPolicy = createRollingPolicy(ctx, props, fileName);
    FileAppender<ILoggingEvent> fileAppender = rollingPolicy.createAppender("file");
    fileAppender.setContext(ctx);
    PatternLayoutEncoder fileEncoder = new PatternLayoutEncoder();
    fileEncoder.setContext(ctx);
    fileEncoder.setPattern(logFormat);
    fileEncoder.start();
    fileAppender.setEncoder(fileEncoder);
    fileAppender.start();

    // configure logger
    Logger rootLogger = ctx.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
    rootLogger.addAppender(fileAppender);
    rootLogger.detachAppender("console");
  }

  public ConsoleAppender newConsoleAppender(Context loggerContext, String name, String pattern, Filter... filters) {
    PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
    consoleEncoder.setContext(loggerContext);
    consoleEncoder.setPattern(pattern);
    consoleEncoder.start();
    ConsoleAppender consoleAppender = new ConsoleAppender();
    consoleAppender.setContext(loggerContext);
    consoleAppender.setEncoder(consoleEncoder);
    consoleAppender.setName(name);
    consoleAppender.setTarget("System.out");
    for (Filter filter : filters) {
      consoleAppender.addFilter(filter);
    }
    consoleAppender.start();
    return consoleAppender;
  }

  public Logger configureLogger(String loggerName, Level level) {
    Logger logger = getRootContext().getLogger(loggerName);
    logger.setLevel(level);
    return logger;
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
    File logsDir = props.nonNullValueAsFile(ProcessProperties.PATH_LOGS);

    if (rollingPolicy.startsWith("time:")) {
      return new TimeRollingPolicy(ctx, filenamePrefix, logsDir, maxFiles, StringUtils.substringAfter(rollingPolicy, "time:"));

    } else if (rollingPolicy.startsWith("size:")) {
      return new SizeRollingPolicy(ctx, filenamePrefix, logsDir, maxFiles, StringUtils.substringAfter(rollingPolicy, "size:"));

    } else if ("none".equals(rollingPolicy)) {
      return new NoRollingPolicy(ctx, filenamePrefix, logsDir, maxFiles);

    } else {
      throw new MessageException(String.format("Unsupported value for property %s: %s", ROLLING_POLICY_PROPERTY, rollingPolicy));
    }
  }

  public abstract static class RollingPolicy {
    protected final Context context;
    protected final String filenamePrefix;
    protected final File logsDir;
    protected final int maxFiles;

    public RollingPolicy(Context context, String filenamePrefix, File logsDir, int maxFiles) {
      this.context = context;
      this.filenamePrefix = filenamePrefix;
      this.logsDir = logsDir;
      this.maxFiles = maxFiles;
    }

    public abstract FileAppender createAppender(String appenderName);
  }

  /**
   * Log files are not rotated, for example for unix command logrotate is in place.
   */
  static class NoRollingPolicy extends RollingPolicy {
    NoRollingPolicy(Context context, String filenamePrefix, File logsDir, int maxFiles) {
      super(context, filenamePrefix, logsDir, maxFiles);
    }

    @Override
    public FileAppender createAppender(String appenderName) {
      FileAppender appender = new FileAppender<>();
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
  static class TimeRollingPolicy extends RollingPolicy {
    private final String datePattern;

    public TimeRollingPolicy(Context context, String filenamePrefix, File logsDir, int maxFiles, String datePattern) {
      super(context, filenamePrefix, logsDir, maxFiles);
      this.datePattern = datePattern;
    }

    @Override
    public FileAppender createAppender(String appenderName) {
      RollingFileAppender appender = new RollingFileAppender();
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
  static class SizeRollingPolicy extends RollingPolicy {
    private final String size;

    SizeRollingPolicy(Context context, String filenamePrefix, File logsDir, int maxFiles, String parameter) {
      super(context, filenamePrefix, logsDir, maxFiles);
      this.size = parameter;
    }

    @Override
    public FileAppender createAppender(String appenderName) {
      RollingFileAppender appender = new RollingFileAppender();
      appender.setContext(context);
      appender.setName(appenderName);
      String filePath = new File(logsDir, filenamePrefix + ".log").getAbsolutePath();
      appender.setFile(filePath);

      SizeBasedTriggeringPolicy trigger = new SizeBasedTriggeringPolicy(size);
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
