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
package org.sonar.application.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;
import org.sonar.process.logging.LogbackHelper;

public final class ListAppender extends AppenderBase<ILoggingEvent> {
  private final List<ILoggingEvent> logs = new ArrayList<>();

  @Override
  protected void append(ILoggingEvent eventObject) {
    logs.add(eventObject);
  }

  public List<ILoggingEvent> getLogs() {
    return logs;
  }

  public static <T> ListAppender attachMemoryAppenderToLoggerOf(Class<T> loggerClass) {
    ListAppender listAppender = new ListAppender();
    new LogbackHelper().getRootContext().getLogger(loggerClass)
      .addAppender(listAppender);
    listAppender.start();
    return listAppender;
  }

  public static <T> void detachMemoryAppenderToLoggerOf(Class<T> loggerClass, ListAppender listAppender) {
    listAppender.stop();
    new LogbackHelper().getRootContext().getLogger(loggerClass)
      .detachAppender(listAppender);
  }
}
