/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.api.batch.sensor.issue.internal;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DefaultIssueLocationTest {

  private InputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.php")
    .initMetadata("Foo\nBar\n")
    .build();

  @Test
  public void should_build() {
    assertThat(new DefaultIssueLocation()
      .on(inputFile)
      .message("pipo bimbo")
      .message()
    ).isEqualTo("pipo bimbo");
  }

  @Test
  public void not_allowed_to_call_on_twice() {
    assertThatThrownBy(() -> new DefaultIssueLocation()
      .on(inputFile)
      .on(inputFile)
      .message("Wrong way!"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("on() already called");
  }

  @Test
  public void prevent_too_long_messages() {
    assertThat(new DefaultIssueLocation()
      .on(inputFile)
      .message(StringUtils.repeat("a", 4000)).message()).hasSize(4000);

    assertThat(new DefaultIssueLocation()
      .on(inputFile)
      .message(StringUtils.repeat("a", 4001)).message()).hasSize(4000);
  }

  @Test
  public void prevent_null_character_in_message_text() {
    assertThatThrownBy(() -> new DefaultIssueLocation()
      .message("pipo " + '\u0000' + " bimbo"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("Character \\u0000 is not supported in issue message");
  }

  @Test
  public void prevent_null_character_in_message_text_when_builder_has_been_initialized() {
    assertThatThrownBy(() -> new DefaultIssueLocation()
      .on(inputFile)
      .message("pipo " + '\u0000' + " bimbo"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageStartingWith("Character \\u0000 is not supported in issue message")
      .hasMessageEndingWith(", on component: src/Foo.php");
  }
}
