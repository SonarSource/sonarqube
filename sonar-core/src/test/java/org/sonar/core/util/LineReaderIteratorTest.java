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
package org.sonar.core.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LineReaderIteratorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void read_lines() {
    LineReaderIterator it = new LineReaderIterator(new StringReader("line1\nline2"));
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next()).isEqualTo("line1");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next()).isEqualTo("line2");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  public void ignore_last_newline() {
    LineReaderIterator it = new LineReaderIterator(new StringReader("line1\nline2\n"));
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next()).isEqualTo("line1");
    assertThat(it.hasNext()).isTrue();
    assertThat(it.next()).isEqualTo("line2");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  public void no_lines() {
    LineReaderIterator it = new LineReaderIterator(new StringReader(""));
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  public void do_not_wrap_BufferedReader_in_BufferedReader() {
    // TODO how to verify that constructor does not build new BufferedReader(BufferedReader) ?
    LineReaderIterator it = new LineReaderIterator(new BufferedReader(new StringReader("line")));
    assertThat(it.next()).isEqualTo("line");
    assertThat(it.hasNext()).isFalse();
  }

  @Test
  public void fail_if_cannot_read() throws IOException {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Fail to read line");

    BufferedReader reader = mock(BufferedReader.class);
    when(reader.readLine()).thenThrow(new IOException());
    LineReaderIterator it = new LineReaderIterator(reader);

    it.hasNext();
  }
}
