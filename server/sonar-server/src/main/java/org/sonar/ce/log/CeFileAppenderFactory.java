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

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.sift.AppenderFactory;
import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import org.sonar.ce.queue.CeTask;

import static java.lang.String.format;

/**
 * Creates a Logback appender for a Compute Engine task. See
 * http://logback.qos.ch/manual/loggingSeparation.html
 */
public class CeFileAppenderFactory<E> implements AppenderFactory<E> {

  private static final String ENCODER_PATTERN = "%d{yyyy.MM.dd HH:mm:ss} %-5level [%logger{20}] %msg%n";

  private final File ceLogsDir;

  @VisibleForTesting
  CeFileAppenderFactory(File ceLogsDir) {
    this.ceLogsDir = ceLogsDir;
  }

  /**
   * @param context
   * @param discriminatingValue path of log file relative to the directory data/ce/logs
   * @see CeLogging#initForTask(CeTask)
   */
  @Override
  public FileAppender<E> buildAppender(Context context, String discriminatingValue) {
    PatternLayoutEncoder consoleEncoder = new PatternLayoutEncoder();
    consoleEncoder.setContext(context);
    consoleEncoder.setPattern(ENCODER_PATTERN);
    consoleEncoder.start();
    FileAppender appender = new FileAppender<>();
    appender.setContext(context);
    appender.setEncoder(consoleEncoder);
    appender.setName(format("ce-%s", discriminatingValue));
    appender.setFile(new File(ceLogsDir, discriminatingValue).getAbsolutePath());
    appender.start();
    return appender;
  }

}
