/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.scanner.bootstrap;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScannerLogbackEncoderTest {

  ScannerLogbackEncoder underTest = new ScannerLogbackEncoder();

  @Test
  void no_headers_and_footers() {
    assertThat(underTest.headerBytes()).isEmpty();
    assertThat(underTest.footerBytes()).isEmpty();
  }

  @Test
  void should_encode_when_no_level_and_no_stacktrace() {
    var logEvent = mock(ILoggingEvent.class);
    when(logEvent.getLevel()).thenReturn(null);
    when(logEvent.getFormattedMessage()).thenReturn("message");

    var bytes = underTest.encode(logEvent);

    assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("{\"message\":\"message\"}\n");
  }

  @Test
  void should_encode_when_no_stacktrace() {
    var logEvent = mock(ILoggingEvent.class);
    when(logEvent.getLevel()).thenReturn(Level.DEBUG);
    when(logEvent.getFormattedMessage()).thenReturn("message");

    var bytes = underTest.encode(logEvent);

    assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo("{\"level\":\"DEBUG\",\"message\":\"message\"}\n");
  }

  @Test
  void should_encode_with_stacktrace() {
    var logEvent = mock(ILoggingEvent.class);
    when(logEvent.getLevel()).thenReturn(Level.DEBUG);
    when(logEvent.getFormattedMessage()).thenReturn("message");
    when(logEvent.getThrowableProxy()).thenReturn(new ThrowableProxy(new IllegalArgumentException("foo")));

    var bytes = underTest.encode(logEvent);

    String encodedLog = new String(bytes, StandardCharsets.UTF_8);
    assertThat(encodedLog).contains("\"level\":\"DEBUG\"")
      .contains("\"message\":\"message\"")
      .contains("\"stacktrace\":\"java.lang.IllegalArgumentException: foo")
      .contains("at org.sonar.scanner.bootstrap.ScannerLogbackEncoderTest.should_encode_with_stacktrace");
  }

}
