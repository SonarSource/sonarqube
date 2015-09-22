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
package org.sonar.server.computation.log;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.sift.MDCBasedDiscriminator;
import ch.qos.logback.classic.sift.SiftingAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.sift.AppenderFactory;
import java.io.File;
import org.sonar.process.ProcessProperties;
import org.sonar.process.Props;

import static java.lang.String.format;

/**
 * Created a Logback file appender for each Compute Engine task
 */
public class CeFileAppenderFactory<E> implements AppenderFactory<E> {

  static final String MDC_TASK_UUID = "ceTaskUuid";
  private static final String ENCODER_PATTERN = "%d{yyyy.MM.dd HH:mm:ss} %-5level %msg%n";

  private final File ceLogsDir;

  private CeFileAppenderFactory(File ceLogsDir) {
    this.ceLogsDir = ceLogsDir;
  }

  /**
   * @param context
   * @param discriminatingValue UUID of the CE task
   */
  @Override
  public Appender<E> buildAppender(Context context, String discriminatingValue) {
    PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
    consoleEncoder.setContext(context);
    consoleEncoder.setPattern(ENCODER_PATTERN);
    consoleEncoder.start();
    FileAppender appender = new FileAppender<>();
    appender.setContext(context);
    appender.setEncoder(consoleEncoder);
    appender.setName(format("ce-%s", discriminatingValue));
    appender.setFile(new File(ceLogsDir, logFilenameForTaskUuid(discriminatingValue)).getAbsolutePath());
    appender.start();
    return appender;
  }

  /**
   * Create a Logback sifting appender dedicated to Compute Engine logs.
   * A new log file is created for each task. It is based on MDC as long
   * as Compute Engine is not executed in its
   * own process but in the same process as web server.
   */
  public static Appender<ILoggingEvent> createSiftingAppender(LoggerContext ctx, Props processProps) {
    File dataDir = new File(processProps.nonNullValue(ProcessProperties.PATH_DATA));
    File logsDir = logsDirFromDataDir(dataDir);

    SiftingAppender siftingAppender = new SiftingAppender();
    siftingAppender.addFilter(new CeLogFilter(true));
    MDCBasedDiscriminator mdcDiscriminator = new MDCBasedDiscriminator();
    mdcDiscriminator.setContext(ctx);
    mdcDiscriminator.setKey(MDC_TASK_UUID);
    mdcDiscriminator.setDefaultValue("error");
    mdcDiscriminator.start();
    siftingAppender.setContext(ctx);
    siftingAppender.setDiscriminator(mdcDiscriminator);
    siftingAppender.setAppenderFactory(new CeFileAppenderFactory(logsDir));
    siftingAppender.setName("ce");
    siftingAppender.start();
    return siftingAppender;
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

  static String logFilenameForTaskUuid(String taskUuid) {
    return format("%s.log", taskUuid);
  }
}
