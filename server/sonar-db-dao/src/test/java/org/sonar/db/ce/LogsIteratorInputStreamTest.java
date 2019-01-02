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
package org.sonar.db.ce;

import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.core.util.CloseableIterator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class LogsIteratorInputStreamTest {
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void read_from_ClosableIterator_with_several_lines() throws IOException {
    assertThat(read(create("line1", "line2", "line3"))).isEqualTo("line1" + '\n' + "line2" + '\n' + "line3");
  }

  @Test
  public void read_from_ClosableIterator_with_single_line() throws IOException {
    assertThat(read(create("line1"))).isEqualTo("line1");
  }

  @Test
  public void read_from_ClosableIterator_with_single_empty_line() throws IOException {
    assertThat(read(create(""))).isEqualTo("");
  }

  @Test
  public void read_from_ClosableIterator_with_several_empty_lines() throws IOException {
    assertThat(read(create("", "line2", "", "line4", "", "", "", "line8", "")))
      .isEqualTo('\n' + "line2" + '\n' + '\n' + "line4" + '\n' + '\n' + '\n' + '\n' + "line8" + '\n');
  }

  @Test
  public void constructor_throws_IAE_when_ClosableIterator_is_empty() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("LogsIterator can't be empty or already read");

    create();
  }

  @Test
  public void constructor_throws_IAE_when_ClosableIterator_has_already_been_read() {
    CloseableIterator<String> iterator = CloseableIterator.from(Arrays.asList("line1").iterator());

    // read iterator to the end
    iterator.next();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("LogsIterator can't be empty or already read");

    new LogsIteratorInputStream(iterator, UTF_8);
  }

  private static LogsIteratorInputStream create(String... lines) {
    return new LogsIteratorInputStream(CloseableIterator.from(Arrays.asList(lines).iterator()), UTF_8);
  }

  private static String read(LogsIteratorInputStream logsIteratorInputStream) throws IOException {
    return IOUtils.toString(logsIteratorInputStream, UTF_8);
  }
}
