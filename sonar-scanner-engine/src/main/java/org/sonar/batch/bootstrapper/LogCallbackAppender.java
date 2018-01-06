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
import ch.qos.logback.classic.pattern.ExtendedThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;

public class LogCallbackAppender extends UnsynchronizedAppenderBase<ILoggingEvent> {
  protected LogOutput target;

  public LogCallbackAppender(LogOutput target) {
    setTarget(target);
  }

  public void setTarget(LogOutput target) {
    this.target = target;
  }

  @Override
  protected void append(ILoggingEvent event) {
    if (event.getThrowableProxy() == null) {
      target.log(event.getFormattedMessage(), translate(event.getLevel()));
    } else {
      ExtendedThrowableProxyConverter throwableConverter = new ExtendedThrowableProxyConverter();
      throwableConverter.start();
      target.log(event.getFormattedMessage() + "\n" + throwableConverter.convert(event), translate(event.getLevel()));
      throwableConverter.stop();
    }
  }

  private static LogOutput.Level translate(Level level) {
    switch (level.toInt()) {
      case Level.ERROR_INT:
        return LogOutput.Level.ERROR;
      case Level.WARN_INT:
        return LogOutput.Level.WARN;
      case Level.INFO_INT:
        return LogOutput.Level.INFO;
      case Level.TRACE_INT:
        return LogOutput.Level.TRACE;
      case Level.DEBUG_INT:
      default:
        return LogOutput.Level.DEBUG;
    }
  }
}
