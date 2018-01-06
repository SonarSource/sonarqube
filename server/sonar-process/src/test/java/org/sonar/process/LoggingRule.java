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
package org.sonar.process;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.joran.spi.JoranException;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.rules.ExternalResource;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.sonar.process.logging.LogbackHelper;

public class LoggingRule extends ExternalResource {

  private final Class loggerClass;

  public LoggingRule(Class loggerClass) {
    this.loggerClass = loggerClass;
  }

  @Override
  protected void before() throws Throwable {
    new LogbackHelper().resetFromXml("/org/sonar/process/logback-test.xml");
    TestLogbackAppender.events.clear();
    setLevel(Level.INFO);
  }

  @Override
  protected void after() {
    TestLogbackAppender.events.clear();
    setLevel(Level.INFO);
    try {
      new LogbackHelper().resetFromXml("/logback-test.xml");
    } catch (JoranException e) {
      e.printStackTrace();
    }
  }

  public LoggingRule setLevel(Level level) {
    Logger logbackLogger = (Logger) LoggerFactory.getLogger(loggerClass);
    ch.qos.logback.classic.Level l = ch.qos.logback.classic.Level.valueOf(level.name());
    logbackLogger.setLevel(l);
    return this;
  }

  public List<String> getLogs() {
    return TestLogbackAppender.events.stream()
      .filter(e -> e.getLoggerName().equals(loggerClass.getName()))
      .map(LoggingEvent::getFormattedMessage)
      .collect(Collectors.toList());
  }

  public List<String> getLogs(Level level) {
    return TestLogbackAppender.events.stream()
      .filter(e -> e.getLoggerName().equals(loggerClass.getName()))
      .filter(e -> e.getLevel().levelStr.equals(level.name()))
      .map(LoggingEvent::getFormattedMessage)
      .collect(Collectors.toList());
  }

  public boolean hasLog(Level level, String message) {
    return TestLogbackAppender.events.stream()
      .filter(e -> e.getLoggerName().equals(loggerClass.getName()))
      .filter(e -> e.getLevel().levelStr.equals(level.name()))
      .anyMatch(e -> e.getFormattedMessage().equals(message));
  }

  public boolean hasLog(String message) {
    return TestLogbackAppender.events.stream()
      .filter(e -> e.getLoggerName().equals(loggerClass.getName()))
      .anyMatch(e -> e.getFormattedMessage().equals(message));
  }
}
