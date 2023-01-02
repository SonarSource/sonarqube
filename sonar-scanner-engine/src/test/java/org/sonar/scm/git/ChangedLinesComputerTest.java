/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
    String example = "diff --git a/file-b1.xoo b/file-b1.xoo\n"
      + "index 0000000..c2a9048\n"
      + "--- a/foo\n"
      + "+++ b/bar\n"
      + "@@ -1 +0,0 @@\n"
      + "-deleted line\n";

    printDiff(example);
    assertThat(underTest.changedLines()).isEmpty();
  }

  @Test
  public void count_single_added_line() throws IOException {
    String example = "diff --git a/file-b1.xoo b/file-b1.xoo\n"
      + "index 0000000..c2a9048\n"
      + "--- a/foo\n"
      + "+++ b/bar\n"
      + "@@ -0,0 +1 @@\n"
      + "+added line\n";

    printDiff(example);
    assertThat(underTest.changedLines()).containsExactly(1);
  }

  @Test
  public void count_multiple_added_lines() throws IOException {
    String example = "diff --git a/file-b1.xoo b/file-b1.xoo\n"
      + "index 0000000..c2a9048\n"
      + "--- a/foo\n"
      + "+++ b/bar\n"
      + "@@ -1 +1,3 @@\n"
      + " unchanged line\n"
      + "+added line 1\n"
      + "+added line 2\n";

    printDiff(example);
    assertThat(underTest.changedLines()).containsExactly(2, 3);
  }

  @Test
  public void compute_from_multiple_hunks() throws IOException {
    String example = "diff --git a/lao b/lao\n"
      + "index 635ef2c..5af88a8 100644\n"
      + "--- a/lao\n"
      + "+++ b/lao\n"
      + "@@ -1,7 +1,6 @@\n"
      + "-The Way that can be told of is not the eternal Way;\n"
      + "-The name that can be named is not the eternal name.\n"
      + " The Nameless is the origin of Heaven and Earth;\n"
      + "-The Named is the mother of all things.\n"
      + "+The named is the mother of all things.\n"
      + "+\n"
      + " Therefore let there always be non-being,\n"
      + "   so we may see their subtlety,\n"
      + " And let there always be being,\n"
      + "@@ -9,3 +8,6 @@ And let there always be being,\n"
      + " The two are the same,\n"
      + " But after they are produced,\n"
      + "   they have different names.\n"
      + "+They both may be called deep and profound.\n"
      + "+Deeper and more profound,\n"
      + "+The door of all subtleties!\n";
    printDiff(example);
    assertThat(underTest.changedLines()).containsExactly(2, 3, 11, 12, 13);
  }

  @Test
  public void compute_from_multiple_hunks_with_extra_header_lines() throws IOException {
    String example = "diff --git a/lao b/lao\n"
      + "new file mode 100644\n"
      + "whatever "
      + "other "
      + "surprise header lines git might throw at us...\n"
      + "index 635ef2c..5af88a8 100644\n"
      + "--- a/lao\n"
      + "+++ b/lao\n"
      + "@@ -1,7 +1,6 @@\n"
      + "-The Way that can be told of is not the eternal Way;\n"
      + "-The name that can be named is not the eternal name.\n"
      + " The Nameless is the origin of Heaven and Earth;\n"
      + "-The Named is the mother of all things.\n"
      + "+The named is the mother of all things.\n"
      + "+\n"
      + " Therefore let there always be non-being,\n"
      + "   so we may see their subtlety,\n"
      + " And let there always be being,\n"
      + "@@ -9,3 +8,6 @@ And let there always be being,\n"
      + " The two are the same,\n"
      + " But after they are produced,\n"
      + "   they have different names.\n"
      + "+They both may be called deep and profound.\n"
      + "+Deeper and more profound,\n"
      + "+The door of all subtleties!\n";
    printDiff(example);
    assertThat(underTest.changedLines()).containsExactly(2, 3, 11, 12, 13);
  }

  @Test
  public void throw_exception_invalid_start_line_format() throws IOException {
    String example = "diff --git a/file-b1.xoo b/file-b1.xoo\n"
      + "index 0000000..c2a9048\n"
      + "--- a/foo\n"
      + "+++ b/bar\n"
      + "@@ -1 +x1,3 @@\n"
      + " unchanged line\n"
      + "+added line 1\n"
      + "+added line 2\n";

    assertThatThrownBy(() -> printDiff(example))
      .isInstanceOf(IllegalStateException.class);
  }

  private void printDiff(String unifiedDiff) throws IOException {
    try (OutputStreamWriter writer = new OutputStreamWriter(underTest.receiver())) {
      writer.write(unifiedDiff);
    }
  }
}
