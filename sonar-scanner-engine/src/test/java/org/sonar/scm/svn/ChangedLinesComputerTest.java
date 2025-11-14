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
package org.sonar.scm.svn;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

public class ChangedLinesComputerTest {

  private final Path rootBaseDir = Paths.get("/foo");
  private final ChangedLinesComputer underTest = new ChangedLinesComputer(rootBaseDir, new HashSet<>(Arrays.asList(
    rootBaseDir.resolve("sample1"),
    rootBaseDir.resolve("sample2"),
    rootBaseDir.resolve("sample3"),
    rootBaseDir.resolve("sample4"))));

  @Test
  public void do_not_count_deleted_line() throws IOException {
    String example = """
      Index: sample1
      ===================================================================
      --- a/sample1
      +++ b/sample1
      @@ -1 +0,0 @@
      -deleted line
      """;

    printDiff(example);
    assertThat(underTest.changedLines()).isEmpty();
  }

  @Test
  public void count_single_added_line() throws IOException {
    String example = """
      Index: sample1
      ===================================================================
      --- a/sample1
      +++ b/sample1
      @@ -0,0 +1 @@
      +added line
      """;

    printDiff(example);
    assertThat(underTest.changedLines()).isEqualTo(Collections.singletonMap(rootBaseDir.resolve("sample1"), singleton(1)));
  }

  @Test
  public void count_multiple_added_lines() throws IOException {
    String example = """
      Index: sample1
      ===================================================================
      --- a/sample1
      +++ b/sample1
      @@ -1 +1,3 @@
       same line
      +added line 1
      +added line 2
      """;

    printDiff(example);
    assertThat(underTest.changedLines()).isEqualTo(Collections.singletonMap(rootBaseDir.resolve("sample1"), new HashSet<>(Arrays.asList(2, 3))));
  }

  @Test
  public void handle_index_using_absolute_paths() throws IOException {
    String example = """
      Index: /foo/sample1
      ===================================================================
      --- a/sample1
      +++ b/sample1
      @@ -1 +1,3 @@
       same line
      +added line 1
      +added line 2
      """;

    printDiff(example);
    assertThat(underTest.changedLines()).isEqualTo(Collections.singletonMap(rootBaseDir.resolve("sample1"), new HashSet<>(Arrays.asList(2, 3))));
  }

  @Test
  public void compute_from_multiple_hunks() throws IOException {
    String example = """
      Index: sample1
      ===================================================================
      --- lao\t2002-02-21 23:30:39.942229878 -0800
      +++ tzu\t2002-02-21 23:30:50.442260588 -0800
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
      @@ -9,3 +8,6 @@
       The two are the same,
       But after they are produced,
         they have different names.
      +They both may be called deep and profound.
      +Deeper and more profound,
      +The door of all subtleties!
      """;
    printDiff(example);
    assertThat(underTest.changedLines()).isEqualTo(Collections.singletonMap(rootBaseDir.resolve("sample1"), new HashSet<>(Arrays.asList(2, 3, 11, 12, 13))));
  }

  @Test(expected = IllegalStateException.class)
  public void crash_on_invalid_start_line_format() throws IOException {
    String example = """
      Index: sample1
      ===================================================================
      --- a/sample1
      +++ b/sample1
      @@ -1 +x1,3 @@
       same line
      +added line 1
      +added line 2
      """;

    printDiff(example);
    underTest.changedLines();
  }

  @Test
  public void parse_diff_with_multiple_files() throws IOException {
    String example = """
      Index: sample1
      ===================================================================
      --- a/sample1
      +++ b/sample1
      @@ -1 +0,0 @@
      -deleted line
      Index: sample2
      ===================================================================
      --- a/sample2
      +++ b/sample2
      @@ -0,0 +1 @@
      +added line
      Index: sample3
      ===================================================================
      --- a/sample3
      +++ b/sample3
      @@ -0,0 +1,2 @@
      +added line 1
      +added line 2
      Index: sample3-not-included
      ===================================================================
      --- a/sample3-not-included
      +++ b/sample3-not-included
      @@ -0,0 +1,2 @@
      +added line 1
      +added line 2
      Index: sample4
      ===================================================================
      --- a/sample4
      +++ b/sample4
      @@ -1 +1,3 @@
       same line
      +added line 1
      +added line 2
      """;

    printDiff(example);
    Map<Path, Set<Integer>> expected = new HashMap<>();
    expected.put(rootBaseDir.resolve("sample2"), Collections.singleton(1));
    expected.put(rootBaseDir.resolve("sample3"), new HashSet<>(Arrays.asList(1, 2)));
    expected.put(rootBaseDir.resolve("sample4"), new HashSet<>(Arrays.asList(2, 3)));

    assertThat(underTest.changedLines()).isEqualTo(expected);
  }

  private void printDiff(String unifiedDiff) throws IOException {
    try (OutputStreamWriter writer = new OutputStreamWriter(underTest.receiver())) {
      writer.write(unifiedDiff);
    }
  }
}
