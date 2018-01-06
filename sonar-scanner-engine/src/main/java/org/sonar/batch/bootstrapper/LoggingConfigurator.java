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
package org.sonar.batch.bootstrapper;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import java.io.File;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.core.config.Logback;

public class LoggingConfigurator {
  private static final String CUSTOM_APPENDER_NAME = "custom_stream";

  private LoggingConfigurator() {
  }

  public static void apply(LoggingConfiguration conf, File logbackFile) {
    Logback.configure(logbackFile, conf.getSubstitutionVariables());

    if (conf.getLogOutput() != null) {
      setCustomRootAppender(conf);
    }
  }

  public static void apply(LoggingConfiguration conf) {
    apply(conf, "/org/sonar/batch/bootstrapper/logback.xml");
  }

  public static void apply(LoggingConfiguration conf, String classloaderPath) {
    Logback.configure(classloaderPath, conf.getSubstitutionVariables());

    // if not set, keep default behavior (configured to stdout through the file in classpath)
    if (conf.getLogOutput() != null) {
      setCustomRootAppender(conf);
    }
  }

  private static void setCustomRootAppender(LoggingConfiguration conf) {
    Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    String level = StringUtils.defaultIfBlank(conf.getSubstitutionVariables().get(LoggingConfiguration.PROPERTY_ROOT_LOGGER_LEVEL), LoggingConfiguration.LEVEL_ROOT_DEFAULT);

    if (logger.getAppender(CUSTOM_APPENDER_NAME) == null) {
      logger.detachAndStopAllAppenders();
      logger.addAppender(createAppender(conf.getLogOutput()));
    }
    logger.setLevel(Level.toLevel(level));
  }

  private static Appender<ILoggingEvent> createAppender(LogOutput target) {
    LogCallbackAppender appender = new LogCallbackAppender(target);
    appender.setName(CUSTOM_APPENDER_NAME);
    appender.start();

    return appender;
  }
}
