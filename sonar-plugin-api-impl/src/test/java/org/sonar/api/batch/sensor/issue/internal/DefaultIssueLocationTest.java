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
package org.sonar.api.batch.sensor.issue.internal;

import org.apache.commons.lang.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.rules.ExpectedException.none;

public class DefaultIssueLocationTest {

  @Rule
  public ExpectedException thrown = none();

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
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("on() already called");
    new DefaultIssueLocation()
      .on(inputFile)
      .on(inputFile)
      .message("Wrong way!");
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
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Character \\u0000 is not supported in issue message");

    new DefaultIssueLocation()
      .message("pipo " + '\u0000' + " bimbo");
  }

  @Test
  public void prevent_null_character_in_message_text_when_builder_has_been_initialized() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(customMatcher("Character \\u0000 is not supported in issue message", ", on component: src/Foo.php"));

    new DefaultIssueLocation()
      .on(inputFile)
      .message("pipo " + '\u0000' + " bimbo");
  }

  private Matcher<String> customMatcher(String startWith, String endWith) {
    return new TypeSafeMatcher<String>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Invalid message");
      }

      @Override
      protected boolean matchesSafely(final String item) {
        return item.startsWith(startWith) && item.endsWith(endWith);
      }
    };
  }

}
