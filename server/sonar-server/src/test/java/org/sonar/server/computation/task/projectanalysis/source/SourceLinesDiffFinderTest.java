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
package org.sonar.server.computation.task.projectanalysis.source;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceLinesDiffFinderTest {

  @Test
  public void shouldFindNotingWhenContentAreIdentical() {

    List<String> database = new ArrayList<>();
    database.add("line - 0");
    database.add("line - 1");
    database.add("line - 2");
    database.add("line - 3");
    database.add("line - 4");

    List<String> report = new ArrayList<>();
    report.add("line - 0");
    report.add("line - 1");
    report.add("line - 2");
    report.add("line - 3");
    report.add("line - 4");

    Set<Integer> diff = new SourceLinesDiffFinder(database, report).findNewOrChangedLines();

    assertThat(diff).isEmpty();

  }

  @Test
  public void shouldDetectWhenStartingWithModifiedLines() {

    List<String> database = new ArrayList<>();
    database.add("line - 0");
    database.add("line - 1");
    database.add("line - 2");
    database.add("line - 3");

    List<String> report = new ArrayList<>();
    report.add("line - 0 - modified");
    report.add("line - 1 - modified");
    report.add("line - 2");
    report.add("line - 3");

    Set<Integer> diff = new SourceLinesDiffFinder(database, report).findNewOrChangedLines();

    assertThat(diff).containsExactlyInAnyOrder(0, 1);

  }

  @Test
  public void shouldDetectWhenEndingWithModifiedLines() {

    List<String> database = new ArrayList<>();
    database.add("line - 0");
    database.add("line - 1");
    database.add("line - 2");
    database.add("line - 3");

    List<String> report = new ArrayList<>();
    report.add("line - 0");
    report.add("line - 1");
    report.add("line - 2 - modified");
    report.add("line - 3 - modified");

    Set<Integer> diff = new SourceLinesDiffFinder(database, report).findNewOrChangedLines();

    assertThat(diff).containsExactlyInAnyOrder(2, 3);

  }

  @Test
  public void shouldDetectModifiedLinesInMiddleOfTheFile() {

    List<String> database = new ArrayList<>();
    database.add("line - 0");
    database.add("line - 1");
    database.add("line - 2");
    database.add("line - 3");
    database.add("line - 4");
    database.add("line - 5");

    List<String> report = new ArrayList<>();
    report.add("line - 0");
    report.add("line - 1");
    report.add("line - 2 - modified");
    report.add("line - 3 - modified");
    report.add("line - 4");
    report.add("line - 5");

    Set<Integer> diff = new SourceLinesDiffFinder(database, report).findNewOrChangedLines();

    assertThat(diff).containsExactlyInAnyOrder(2, 3);

  }

  @Test
  public void shouldDetectNewLinesAtBeginningOfFile() {

    List<String> database = new ArrayList<>();
    database.add("line - 0");
    database.add("line - 1");
    database.add("line - 2");

    List<String> report = new ArrayList<>();
    report.add("line - new");
    report.add("line - new");
    report.add("line - 0");
    report.add("line - 1");
    report.add("line - 2");

    Set<Integer> diff = new SourceLinesDiffFinder(database, report).findNewOrChangedLines();

    assertThat(diff).containsExactlyInAnyOrder(0, 1);

  }

  @Test
  public void shouldDetectNewLinesInMiddleOfFile() {

    List<String> database = new ArrayList<>();
    database.add("line - 0");
    database.add("line - 1");
    database.add("line - 2");
    database.add("line - 3");

    List<String> report = new ArrayList<>();
    report.add("line - 0");
    report.add("line - 1");
    report.add("line - new");
    report.add("line - new");
    report.add("line - 2");
    report.add("line - 3");

    Set<Integer> diff = new SourceLinesDiffFinder(database, report).findNewOrChangedLines();

    assertThat(diff).containsExactlyInAnyOrder(2, 3);

  }

  @Test
  public void shouldDetectNewLinesAtEndOfFile() {

    List<String> database = new ArrayList<>();
    database.add("line - 0");
    database.add("line - 1");
    database.add("line - 2");

    List<String> report = new ArrayList<>();
    report.add("line - 0");
    report.add("line - 1");
    report.add("line - 2");
    report.add("line - new");
    report.add("line - new");

    Set<Integer> diff = new SourceLinesDiffFinder(database, report).findNewOrChangedLines();

    assertThat(diff).containsExactlyInAnyOrder(3, 4);

  }

  @Test
  public void shouldIgnoreDeletedLinesAtEndOfFile() {

    List<String> database = new ArrayList<>();
    database.add("line - 0");
    database.add("line - 1");
    database.add("line - 2");
    database.add("line - 3");
    database.add("line - 4");

    List<String> report = new ArrayList<>();
    report.add("line - 0");
    report.add("line - 1");
    report.add("line - 2");

    Set<Integer> diff = new SourceLinesDiffFinder(database, report).findNewOrChangedLines();

    assertThat(diff).isEmpty();

  }

  @Test
  public void shouldIgnoreDeletedLinesInTheMiddleOfFile() {

    List<String> database = new ArrayList<>();
    database.add("line - 0");
    database.add("line - 1");
    database.add("line - 2");
    database.add("line - 3");
    database.add("line - 4");
    database.add("line - 5");

    List<String> report = new ArrayList<>();
    report.add("line - 0");
    report.add("line - 1");
    report.add("line - 4");
    report.add("line - 5");

    Set<Integer> diff = new SourceLinesDiffFinder(database, report).findNewOrChangedLines();

    assertThat(diff).isEmpty();

  }

  @Test
  public void shouldIgnoreDeletedLinesAtTheStartOfTheFile() {

    List<String> database = new ArrayList<>();
    database.add("line - 0");
    database.add("line - 1");
    database.add("line - 2");
    database.add("line - 3");

    List<String> report = new ArrayList<>();
    report.add("line - 2");
    report.add("line - 3");

    Set<Integer> diff = new SourceLinesDiffFinder(database, report).findNewOrChangedLines();

    assertThat(diff).isEmpty();

  }

}
