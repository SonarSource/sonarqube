/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.measure;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.json.simple.parser.ParseException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;

import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

public class MeasureFilterEngineTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void should_decode_json_and_execute_filter() throws Exception {
    MeasureFilterDecoder decoder = mock(MeasureFilterDecoder.class);
    MeasureFilter filter = new MeasureFilter();
    when(decoder.decode("{}")).thenReturn(filter);
    MeasureFilterExecutor executor = mock(MeasureFilterExecutor.class);
    Logger logger = mock(Logger.class);
    when(logger.isDebugEnabled()).thenReturn(true);

    MeasureFilterEngine engine = new MeasureFilterEngine(decoder, null, executor);

    final long userId = 50L;
    engine.execute("{}", userId, logger);
    verify(executor).execute(refEq(filter), argThat(new BaseMatcher<MeasureFilterContext>() {
      public boolean matches(Object o) {
        MeasureFilterContext context = (MeasureFilterContext) o;
        return "{}".equals(context.getJson()) && context.getUserId() == userId;
      }

      public void describeTo(Description description) {
      }
    }));
    verify(logger).debug(anyString());
  }

  @Test
  public void throw_definition_of_filter_on_error() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("filter=<xml>");

    MeasureFilterDecoder decoder = mock(MeasureFilterDecoder.class);
    when(decoder.decode("<xml>")).thenThrow(new ParseException(0));
    MeasureFilterExecutor executor = mock(MeasureFilterExecutor.class);

    MeasureFilterEngine engine = new MeasureFilterEngine(decoder, null, executor);
    engine.execute("<xml>", 50L);
  }
}
