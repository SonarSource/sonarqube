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
package org.sonar.process;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.LoggerFactory;

public class ProcessLogging {

  public void configure(Props props, String logbackXmlResource) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    try {
      JoranConfigurator configurator = new JoranConfigurator();
      configurator.setContext(context);
      context.reset();
      context.putProperty(ProcessConstants.PATH_LOGS, props.nonNullValue(ProcessConstants.PATH_LOGS));
      doConfigure(configurator, logbackXmlResource);
    } catch (JoranException ignored) {
      // StatusPrinter will handle this
    }
    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
  }

  public void addConsoleAppender() {
    Logger consoleLogger = (Logger) LoggerFactory.getLogger("console");
    Appender<ILoggingEvent> consoleAppender = consoleLogger.getAppender("CONSOLE");

    Logger gobblerLogger = (Logger) LoggerFactory.getLogger("gobbler");
    gobblerLogger.addAppender(consoleAppender);
  }

  /**
   * Extracted only for unit testing
   */
  void doConfigure(JoranConfigurator configurator, String logbackXmlResource) throws JoranException {
    configurator.doConfigure(getClass().getResource(logbackXmlResource));
  }
}
