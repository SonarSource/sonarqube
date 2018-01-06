/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.core.hash;

import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceHashComputerTest {

  private static final String SOME_LINE = "some line";

  @Test
  public void hash_of_non_last_line_is_not_the_same_as_hash_of_last_line() {
    assertThat(hashASingleLine(SOME_LINE, true)).isNotEqualTo(hashASingleLine(SOME_LINE, false));
  }

  @Test
  public void hash_of_non_last_line_is_includes_an_added_line_return() {
    assertThat(hashASingleLine(SOME_LINE, true)).isEqualTo(hashASingleLine(SOME_LINE + '\n', false));
  }

  @Test
  public void hash_is_md5_digest_of_UTF8_character_array_in_hexa_encoding() {
    String someLineWithAccents = "yopa l\u00e9l\u00e0";

    assertThat(hashASingleLine(someLineWithAccents, false))
      .isEqualTo(DigestUtils.md5Hex(someLineWithAccents.getBytes(StandardCharsets.UTF_8)));

  }

  private static String hashASingleLine(String line, boolean hasNextLine) {
    SourceHashComputer sourceHashComputer = new SourceHashComputer();
    sourceHashComputer.addLine(line, hasNextLine);
    return sourceHashComputer.getHash();
  }
}
