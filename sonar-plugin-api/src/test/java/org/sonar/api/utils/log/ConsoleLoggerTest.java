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
package org.sonar.api.utils.log;

import java.io.PrintStream;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

public class ConsoleLoggerTest {

  PrintStream stream = mock(PrintStream.class);
  ConsoleLogger underTest = new ConsoleLogger(stream);

  @Rule
  public LogTester tester = new LogTester();

  @Test
  public void debug_enabled() {
    tester.setLevel(LoggerLevel.DEBUG);
    assertThat(underTest.isDebugEnabled()).isTrue();
    assertThat(underTest.isTraceEnabled()).isFalse();
    underTest.debug("message");
    underTest.debug("message {}", "foo");
    underTest.debug("message {} {}", "foo", "bar");
    underTest.debug("message {} {} {}", "foo", "bar", "baz");
    verify(stream, times(4)).println(anyString());
  }

  @Test
  public void debug_disabled() {
    tester.setLevel(LoggerLevel.INFO);
    assertThat(underTest.isDebugEnabled()).isFalse();
    assertThat(underTest.isTraceEnabled()).isFalse();
    underTest.debug("message");
    underTest.debug("message {}", "foo");
    underTest.debug("message {} {}", "foo", "bar");
    underTest.debug("message {} {} {}", "foo", "bar", "baz");
    verifyZeroInteractions(stream);
  }

  @Test
  public void trace_enabled() {
    tester.setLevel(LoggerLevel.TRACE);
    assertThat(underTest.isDebugEnabled()).isTrue();
    assertThat(underTest.isTraceEnabled()).isTrue();
    underTest.trace("message");
    underTest.trace("message {}", "foo");
    underTest.trace("message {} {}", "foo", "bar");
    underTest.trace("message {} {} {}", "foo", "bar", "baz");
    verify(stream, times(4)).println(anyString());
  }

  @Test
  public void trace_disabled() {
    tester.setLevel(LoggerLevel.DEBUG);
    assertThat(underTest.isTraceEnabled()).isFalse();
    underTest.trace("message");
    underTest.trace("message {}", "foo");
    underTest.trace("message {} {}", "foo", "bar");
    underTest.trace("message {} {} {}", "foo", "bar", "baz");
    verifyZeroInteractions(stream);
  }

  @Test
  public void log_info() {
    underTest.info("message");
    underTest.info("message {}", "foo");
    underTest.info("message {} {}", "foo", "bar");
    underTest.info("message {} {} {}", "foo", "bar", "baz");
    verify(stream, times(4)).println(startsWith("INFO "));
  }

  @Test
  public void log_warn() {
    Throwable throwable = mock(Throwable.class);

    underTest.warn("message");
    underTest.warn("message {}", "foo");
    underTest.warn("message {} {}", "foo", "bar");
    underTest.warn("message {} {} {}", "foo", "bar", "baz");
    underTest.warn("message", throwable);
    ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
    verify(stream, times(5)).println(captor.capture());
    for (String msg : captor.getAllValues()) {
      assertThat(msg).startsWith("WARN ");
    }
    verify(throwable).printStackTrace();
  }

  @Test
  public void log_error() {
    underTest.error("message");
    underTest.error("message {}", "foo");
    underTest.error("message {} {}", "foo", "bar");
    underTest.error("message {} {} {}", "foo", "bar", "baz");
    underTest.error("message", new IllegalArgumentException());
    verify(stream, times(5)).println(startsWith("ERROR "));
  }

  @Test
  public void level_change_not_implemented_yet() {
    assertThat(underTest.setLevel(LoggerLevel.DEBUG)).isFalse();
  }
}
