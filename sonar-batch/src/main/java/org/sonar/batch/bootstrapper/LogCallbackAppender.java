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
package org.sonar.batch.bootstrapper;

import org.sonar.home.log.LogListener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class LogCallbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
  protected LogListener target;

  public LogCallbackAppender(LogListener target) {
    setTarget(target);
  }

  public void setTarget(LogListener target) {
    this.target = target;
  }

  @Override
  protected void append(ILoggingEvent event) {
    target.log(event.getFormattedMessage(), translate(event.getLevel()));
  }
  
  private LogListener.Level translate(Level level) {
    switch(level.toInt()) {
      case Level.ERROR_INT:
        return LogListener.Level.ERROR;
      case Level.WARN_INT:
        return LogListener.Level.WARN;
      case Level.INFO_INT:
        return LogListener.Level.INFO;
      case Level.DEBUG_INT:
        return LogListener.Level.DEBUG;
      case Level.TRACE_INT:
        return LogListener.Level.TRACE;
      default:
        return LogListener.Level.DEBUG;
    }
  }
}
