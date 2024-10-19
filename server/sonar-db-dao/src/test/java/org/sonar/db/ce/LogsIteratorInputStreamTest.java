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
package org.sonar.db.ce;

import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.sonar.core.util.CloseableIterator;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogsIteratorInputStreamTest {

  @Test
  void read_from_ClosableIterator_with_several_lines() throws IOException {
    assertThat(read(create("line1", "line2", "line3"))).isEqualTo("line1" + '\n' + "line2" + '\n' + "line3");
  }

  @Test
  void read_from_ClosableIterator_with_single_line() throws IOException {
    assertThat(read(create("line1"))).isEqualTo("line1");
  }

  @Test
  void read_from_ClosableIterator_with_single_empty_line() throws IOException {
    assertThat(read(create(""))).isEmpty();
  }

  @Test
  void read_from_ClosableIterator_with_several_empty_lines() throws IOException {
    assertThat(read(create("", "line2", "", "line4", "", "", "", "line8", "")))
      .isEqualTo('\n' + "line2" + '\n' + '\n' + "line4" + '\n' + '\n' + '\n' + '\n' + "line8" + '\n');
  }

  @Test
  void constructor_throws_IAE_when_ClosableIterator_is_empty() {
    assertThatThrownBy(LogsIteratorInputStreamTest::create)
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("LogsIterator can't be empty or already read");
  }

  @Test
  void constructor_throws_IAE_when_ClosableIterator_has_already_been_read() {
    CloseableIterator<String> iterator = CloseableIterator.from(Arrays.asList("line1").iterator());

    // read iterator to the end
    iterator.next();

    assertThatThrownBy(() -> new LogsIteratorInputStream(iterator, UTF_8))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("LogsIterator can't be empty or already read");
  }

  private static LogsIteratorInputStream create(String... lines) {
    return new LogsIteratorInputStream(CloseableIterator.from(Arrays.asList(lines).iterator()), UTF_8);
  }

  private static String read(LogsIteratorInputStream logsIteratorInputStream) throws IOException {
    return IOUtils.toString(logsIteratorInputStream, UTF_8);
  }
}
