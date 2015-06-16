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

import ch.qos.logback.core.encoder.EchoEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.mockito.Matchers;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.Context;
import org.junit.Test;
import org.junit.Before;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

public class PrintStreamTest {
  private static final String TEST_STR = "foo";

  private ByteArrayOutputStream os;
  private PrintStream stream;
  private PrintStreamAppender<ILoggingEvent> appender;
  private Context context = mock(Context.class);

  private Encoder<ILoggingEvent> encoder = mock(Encoder.class);
  private ILoggingEvent event = mock(ILoggingEvent.class);

  @Before
  public void setUp() {
    os = new ByteArrayOutputStream();
    stream = new PrintStream(os);

    appender = new PrintStreamAppender<ILoggingEvent>(stream);
    when(event.getMessage()).thenReturn(TEST_STR);
    when(event.toString()).thenReturn(TEST_STR);
  }

  @Test
  public void testNullStream() {
    appender.setContext(mock(Context.class));
    appender.setEncoder(encoder);
    appender.setTarget(null);
    appender.start();
    appender.doAppend(event);

    verifyNoMoreInteractions(encoder);
  }

  @Test
  public void testEncoder() throws IOException {
    appender.setContext(mock(Context.class));
    appender.setEncoder(encoder);
    appender.start();
    appender.doAppend(event);

    verify(encoder, times(1)).init(Matchers.notNull(OutputStream.class));
    verify(encoder, times(1)).doEncode(event);

  }

  @Test
  public void testWrite() {
    encoder = new EchoEncoder<>();
    encoder.setContext(context);
    encoder.start();

    appender.setContext(mock(Context.class));
    appender.setEncoder(encoder);
    appender.setTarget(stream);
    appender.start();

    appender.doAppend(event);

    assertThat(os.toString()).isEqualTo(TEST_STR + System.lineSeparator());
  }
}
