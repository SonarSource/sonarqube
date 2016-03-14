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
package org.sonar.ce.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.sift.MDCBasedDiscriminator;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.sift.AppenderTracker;
import ch.qos.logback.core.util.Duration;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.MDC;
import org.sonar.api.config.Settings;
import org.sonar.process.LogbackHelper;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;
import org.sonar.ce.queue.CeTask;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.String.format;

/**
 * Manages the logs written by Compute Engine:
 * <ul>
 *   <li>access to existing logs</li>
 *   <li>configure logback when CE worker starts and stops processing a task</li>
 * </ul>
 */
public class CeLogging {

  private static final long TIMEOUT_2_MINUTES = 1000 * 60 * 2L;
  private static final String CE_APPENDER_NAME = "ce";
  // using 0L as timestamp when retrieving appender to stop it will make it instantly eligible for removal
  private static final long STOPPING_TRACKER_TIMESTAMP = 0L;

  @VisibleForTesting
  static final String MDC_LOG_PATH = "ceLogPath";
  public static final String MAX_LOGS_PROPERTY = "sonar.ce.maxLogsPerTask";

  private final LogbackHelper helper = new LogbackHelper();
  private final File logsDir;
  private final Settings settings;

  public CeLogging(Settings settings) {
    String dataDir = settings.getString(ProcessProperties.PATH_DATA);
    checkArgument(dataDir != null, "Property %s is not set", ProcessProperties.PATH_DATA);
    this.logsDir = logsDirFromDataDir(new File(dataDir));
    this.settings = settings;
  }

  /**
   * Gets the log file of a given task. It may not exist if it
   * was purged or if the task does not exist.
   */
  public Optional<File> getFile(LogFileRef ref) {
    File logFile = new File(logsDir, ref.getRelativePath());
    if (logFile.exists()) {
      return Optional.of(logFile);
    }
    return Optional.absent();
  }

  public void deleteIfExists(LogFileRef ref) {
    File logFile = new File(logsDir, ref.getRelativePath());
    logFile.delete();
  }

  /**
   * Initialize logging of a Compute Engine task. Must be called
   * before first writing of log.
   * <p>After this method is executed, then Compute Engine logs are
   * written to a dedicated appender and are removed from sonar.log.</p>
   */
  public void initForTask(CeTask task) {
    LogFileRef ref = LogFileRef.from(task);
    // Logback SiftingAppender requires to use a String, so
    // the path is put but not the object LogFileRef
    MDC.put(MDC_LOG_PATH, ref.getRelativePath());
  }

  /**
   * Clean-up the logging of a task. Must be called after the last writing
   * of log.
   * <p>After this method is executed, then Compute Engine logs are
   * written to sonar.log only.</p>
   */
  public void clearForTask() {
    String relativePath = (String) MDC.get(MDC_LOG_PATH);
    MDC.remove(MDC_LOG_PATH);

    if (relativePath != null) {
      stopAppender(relativePath);
      purgeDir(new File(logsDir, relativePath).getParentFile());
    }
  }

  private void stopAppender(String relativePath) {
    Appender<ILoggingEvent> appender = helper.getRootContext().getLogger(Logger.ROOT_LOGGER_NAME).getAppender(CE_APPENDER_NAME);
    checkState(appender instanceof SiftingAppender, "Appender with name %s is null or not a SiftingAppender", CE_APPENDER_NAME);
    AppenderTracker<ILoggingEvent> ceAppender = ((SiftingAppender) appender).getAppenderTracker();
    ceAppender.getOrCreate(relativePath, STOPPING_TRACKER_TIMESTAMP).stop();
  }

  @VisibleForTesting
  void purgeDir(File dir) {
    if (dir.exists()) {
      int maxLogs = settings.getInt(MAX_LOGS_PROPERTY);
      if (maxLogs < 0) {
        throw new IllegalArgumentException(format("Property %s must be positive. Got: %d", MAX_LOGS_PROPERTY, maxLogs));
      }
      List<File> logFiles = newArrayList(FileUtils.listFiles(dir, FileFilterUtils.fileFileFilter(), FileFilterUtils.falseFileFilter()));
      if (logFiles.size() > maxLogs) {
        Collections.sort(logFiles, LastModifiedFileComparator.LASTMODIFIED_COMPARATOR);
        for (File logFile : from(logFiles).limit(logFiles.size() - maxLogs)) {
          logFile.delete();
        }
      }
    }
  }

  /**
   * Directory which contains all the compute engine logs.
   * Log files must be persistent among server restarts and upgrades, so they are
   * stored into directory data/ but not into directories logs/ or temp/.
   * @return the non-null directory. It may not exist at startup.
   */
  static File logsDirFromDataDir(File dataDir) {
    return new File(dataDir, "ce/logs");
  }

  /**
   * Create Logback configuration for enabling sift appender.
   * A new log file is created for each task. It is based on MDC as long
   * as Compute Engine is not executed in its
   * own process but in the same process as web server.
   */
  public static Appender<ILoggingEvent> createAppenderConfiguration(LoggerContext ctx, Props processProps) {
    File dataDir = new File(processProps.nonNullValue(ProcessProperties.PATH_DATA));
    File logsDir = logsDirFromDataDir(dataDir);
    return createAppenderConfiguration(ctx, logsDir);
  }

  static SiftingAppender createAppenderConfiguration(LoggerContext ctx, File logsDir) {
    SiftingAppender siftingAppender = new SiftingAppender();
    siftingAppender.addFilter(new CeLogAcceptFilter<ILoggingEvent>());
    MDCBasedDiscriminator mdcDiscriminator = new MDCBasedDiscriminator();
    mdcDiscriminator.setContext(ctx);
    mdcDiscriminator.setKey(MDC_LOG_PATH);
    mdcDiscriminator.setDefaultValue("error");
    mdcDiscriminator.start();
    siftingAppender.setContext(ctx);
    siftingAppender.setDiscriminator(mdcDiscriminator);
    siftingAppender.setAppenderFactory(new CeFileAppenderFactory(logsDir));
    siftingAppender.setName(CE_APPENDER_NAME);
    siftingAppender.setTimeout(new Duration(TIMEOUT_2_MINUTES));
    siftingAppender.start();
    return siftingAppender;
  }
}
