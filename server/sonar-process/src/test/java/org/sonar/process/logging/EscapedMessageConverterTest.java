/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.process.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EscapedMessageConverterTest {

  private final EscapedMessageConverter underTest = new EscapedMessageConverter();

  @Test
  public void convert_null_message() {
    ILoggingEvent event = createILoggingEvent(null);
    assertThat(underTest.convert(event)).isNull();
  }

  @Test
  public void convert_simple_message() {
    ILoggingEvent event = createILoggingEvent("simple message");
    assertThat(underTest.convert(event)).isEqualTo("simple message");
  }

  @Test
  public void convert_message_with_CR() {
    ILoggingEvent event = createILoggingEvent("simple\r message\r with\r CR\r");
    assertThat(underTest.convert(event)).isEqualTo("simple\\r message\\r with\\r CR\\r");
  }

  @Test
  public void convert_message_with_LF() {
    ILoggingEvent event = createILoggingEvent("simple\n message\n with\n LF");
    assertThat(underTest.convert(event)).isEqualTo("simple\\n message\\n with\\n LF");
  }

  @Test
  public void convert_message_with_CRLF() {
    ILoggingEvent event = createILoggingEvent("simple\n\r\n message\r with\r\n CR LF");
    assertThat(underTest.convert(event)).isEqualTo("simple\\n\\r\\n message\\r with\\r\\n CR LF");
  }

  private static ILoggingEvent createILoggingEvent(String message) {
    return new TestILoggingEvent(message);
  }

}
