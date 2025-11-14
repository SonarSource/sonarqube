/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.scm.git;

import java.io.IOException;
import java.io.OutputStreamWriter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ChangedLinesComputerTest {
  private final ChangedLinesComputer underTest = new ChangedLinesComputer();

  @Test
  public void do_not_count_deleted_line() throws IOException {
    String example = """
      diff --git a/file-b1.xoo b/file-b1.xoo
      index 0000000..c2a9048
      --- a/foo
      +++ b/bar
      @@ -1 +0,0 @@
      -deleted line
      """;

    printDiff(example);
    assertThat(underTest.changedLines()).isEmpty();
  }

  @Test
  public void count_single_added_line() throws IOException {
    String example = """
      diff --git a/file-b1.xoo b/file-b1.xoo
      index 0000000..c2a9048
      --- a/foo
      +++ b/bar
      @@ -0,0 +1 @@
      +added line
      """;

    printDiff(example);
    assertThat(underTest.changedLines()).containsExactly(1);
  }

  @Test
  public void count_multiple_added_lines() throws IOException {
    String example = """
      diff --git a/file-b1.xoo b/file-b1.xoo
      index 0000000..c2a9048
      --- a/foo
      +++ b/bar
      @@ -1 +1,3 @@
       unchanged line
      +added line 1
      +added line 2
      """;

    printDiff(example);
    assertThat(underTest.changedLines()).containsExactly(2, 3);
  }

  @Test
  public void compute_from_multiple_hunks() throws IOException {
    String example = """
      diff --git a/lao b/lao
      index 635ef2c..5af88a8 100644
      --- a/lao
      +++ b/lao
      @@ -1,7 +1,6 @@
      -The Way that can be told of is not the eternal Way;
      -The name that can be named is not the eternal name.
       The Nameless is the origin of Heaven and Earth;
      -The Named is the mother of all things.
      +The named is the mother of all things.
      +
       Therefore let there always be non-being,
         so we may see their subtlety,
       And let there always be being,
      @@ -9,3 +8,6 @@ And let there always be being,
       The two are the same,
       But after they are produced,
         they have different names.
      +They both may be called deep and profound.
      +Deeper and more profound,
      +The door of all subtleties!
      """;
    printDiff(example);
    assertThat(underTest.changedLines()).containsExactly(2, 3, 11, 12, 13);
  }

  @Test
  public void compute_from_multiple_hunks_with_extra_header_lines() throws IOException {
    String example = """
      diff --git a/lao b/lao
      new file mode 100644
      whatever \
      other \
      surprise header lines git might throw at us...
      index 635ef2c..5af88a8 100644
      --- a/lao
      +++ b/lao
      @@ -1,7 +1,6 @@
      -The Way that can be told of is not the eternal Way;
      -The name that can be named is not the eternal name.
       The Nameless is the origin of Heaven and Earth;
      -The Named is the mother of all things.
      +The named is the mother of all things.
      +
       Therefore let there always be non-being,
         so we may see their subtlety,
       And let there always be being,
      @@ -9,3 +8,6 @@ And let there always be being,
       The two are the same,
       But after they are produced,
         they have different names.
      +They both may be called deep and profound.
      +Deeper and more profound,
      +The door of all subtleties!
      """;
    printDiff(example);
    assertThat(underTest.changedLines()).containsExactly(2, 3, 11, 12, 13);
  }

  @Test
  public void throw_exception_invalid_start_line_format() throws IOException {
    String example = """
      diff --git a/file-b1.xoo b/file-b1.xoo
      index 0000000..c2a9048
      --- a/foo
      +++ b/bar
      @@ -1 +x1,3 @@
       unchanged line
      +added line 1
      +added line 2
      """;

    assertThatThrownBy(() -> printDiff(example))
      .isInstanceOf(IllegalStateException.class);
  }

  private void printDiff(String unifiedDiff) throws IOException {
    try (OutputStreamWriter writer = new OutputStreamWriter(underTest.receiver())) {
      writer.write(unifiedDiff);
    }
  }
}
