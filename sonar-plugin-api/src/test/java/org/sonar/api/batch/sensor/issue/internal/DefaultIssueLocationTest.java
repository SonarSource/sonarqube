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
package org.sonar.api.batch.sensor.issue.internal;

import org.apache.commons.lang.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultIssueLocationTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private InputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.php")
    .initMetadata("Foo\nBar\n")
    .build();

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

}
