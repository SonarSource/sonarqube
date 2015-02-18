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
package org.sonar.api.utils.log;

import org.junit.Rule;
import org.junit.Test;

import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ConsoleLoggerTest {

  PrintStream stream = mock(PrintStream.class);
  ConsoleLogger sut = new ConsoleLogger(stream);

  @Rule
  public LogTester tester = new LogTester();

  @Test
  public void debug_enabled() throws Exception {
    tester.setLevel(LoggerLevel.DEBUG);
    assertThat(sut.isDebugEnabled()).isTrue();
    assertThat(sut.isTraceEnabled()).isFalse();
    sut.debug("message");
    sut.debug("message {}", "foo");
    sut.debug("message {} {}", "foo", "bar");
    sut.debug("message {} {} {}", "foo", "bar", "baz");
    verify(stream, times(4)).println(anyString());
  }

  @Test
  public void debug_disabled() throws Exception {
    tester.setLevel(LoggerLevel.INFO);
    assertThat(sut.isDebugEnabled()).isFalse();
    assertThat(sut.isTraceEnabled()).isFalse();
    sut.debug("message");
    sut.debug("message {}", "foo");
    sut.debug("message {} {}", "foo", "bar");
    sut.debug("message {} {} {}", "foo", "bar", "baz");
    verifyZeroInteractions(stream);
  }

  @Test
  public void trace_enabled() throws Exception {
    tester.setLevel(LoggerLevel.TRACE);
    assertThat(sut.isDebugEnabled()).isTrue();
    assertThat(sut.isTraceEnabled()).isTrue();
    sut.trace("message");
    sut.trace("message {}", "foo");
    sut.trace("message {} {}", "foo", "bar");
    sut.trace("message {} {} {}", "foo", "bar", "baz");
    verify(stream, times(4)).println(anyString());
  }

  @Test
  public void trace_disabled() throws Exception {
    tester.setLevel(LoggerLevel.DEBUG);
    assertThat(sut.isTraceEnabled()).isFalse();
    sut.trace("message");
    sut.trace("message {}", "foo");
    sut.trace("message {} {}", "foo", "bar");
    sut.trace("message {} {} {}", "foo", "bar", "baz");
    verifyZeroInteractions(stream);
  }

  @Test
  public void log() throws Exception {
    sut.info("message");
    sut.info("message {}", "foo");
    sut.info("message {} {}", "foo", "bar");
    sut.info("message {} {} {}", "foo", "bar", "baz");
    verify(stream, times(4)).println(startsWith("INFO "));

    sut.warn("message");
    sut.warn("message {}", "foo");
    sut.warn("message {} {}", "foo", "bar");
    sut.warn("message {} {} {}", "foo", "bar", "baz");
    verify(stream, times(4)).println(startsWith("WARN "));

    sut.error("message");
    sut.error("message {}", "foo");
    sut.error("message {} {}", "foo", "bar");
    sut.error("message {} {} {}", "foo", "bar", "baz");
    sut.error("message", new IllegalArgumentException());
    verify(stream, times(5)).println(startsWith("ERROR "));
  }

  @Test
  public void level_change_not_implemented_yet() throws Exception {
    assertThat(sut.setLevel(LoggerLevel.DEBUG)).isFalse();
  }
}
