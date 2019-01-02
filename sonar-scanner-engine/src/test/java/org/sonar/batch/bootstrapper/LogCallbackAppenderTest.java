/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class LogCallbackAppenderTest {
  private LogOutput listener;
  private LogCallbackAppender appender;
  private ILoggingEvent event;

  @Before
  public void setUp() {
    listener = mock(LogOutput.class);
    appender = new LogCallbackAppender(listener);
  }

  @Test
  public void testLevelTranslation() {
    testMessage("test", Level.INFO, LogOutput.Level.INFO);
    testMessage("test", Level.DEBUG, LogOutput.Level.DEBUG);
    testMessage("test", Level.ERROR, LogOutput.Level.ERROR);
    testMessage("test", Level.TRACE, LogOutput.Level.TRACE);
    testMessage("test", Level.WARN, LogOutput.Level.WARN);

    // this should never happen
    testMessage("test", Level.OFF, LogOutput.Level.DEBUG);
  }

  private void testMessage(String msg, Level level, LogOutput.Level translatedLevel) {
    reset(listener);
    event = mock(ILoggingEvent.class);
    when(event.getFormattedMessage()).thenReturn(msg);
    when(event.getLevel()).thenReturn(level);

    appender.append(event);

    verify(event).getFormattedMessage();
    verify(event).getLevel();
    verify(event).getThrowableProxy();
    verify(listener).log(msg, translatedLevel);
    verifyNoMoreInteractions(event, listener);
  }

  @Test
  public void testChangeTarget() {
    listener = mock(LogOutput.class);
    appender.setTarget(listener);
    testLevelTranslation();
  }
}
