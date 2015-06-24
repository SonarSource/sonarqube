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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import static org.mockito.Mockito.reset;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.Test;
import org.sonar.home.log.LogListener;
import org.junit.Before;

public class LogCallbackAppenderTest {
  private LogListener listener;
  private LogCallbackAppender appender;
  private ILoggingEvent event;

  @Before
  public void setUp() {
    listener = mock(LogListener.class);
    appender = new LogCallbackAppender(listener);
  }

  
  @Test
  public void testLevelTranslation() {
    testMessage("test", Level.INFO, LogListener.Level.INFO);
    testMessage("test", Level.DEBUG, LogListener.Level.DEBUG);
    testMessage("test", Level.ERROR, LogListener.Level.ERROR);
    testMessage("test", Level.TRACE, LogListener.Level.TRACE);
    testMessage("test", Level.WARN, LogListener.Level.WARN);
    
    // this should never happen
    testMessage("test", Level.OFF, LogListener.Level.DEBUG);
  }
  
  private void testMessage(String msg, Level level, LogListener.Level translatedLevel) {
    reset(listener);
    event = mock(ILoggingEvent.class);
    when(event.getMessage()).thenReturn(msg);
    when(event.getLevel()).thenReturn(level);
    
    appender.append(event);

    verify(event).getMessage();
    verify(event).getLevel();
    verify(listener).log(msg, translatedLevel);
    verifyNoMoreInteractions(event, listener);
  }

  @Test
  public void testChangeTarget() {
    listener = mock(LogListener.class);
    appender.setTarget(listener);
    testLevelTranslation();
  }
}
