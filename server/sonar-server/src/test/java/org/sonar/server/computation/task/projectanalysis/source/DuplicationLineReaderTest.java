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

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.ReportComponent;
import org.sonar.server.computation.task.projectanalysis.duplication.CrossProjectDuplicate;
import org.sonar.server.computation.task.projectanalysis.duplication.Duplicate;
import org.sonar.server.computation.task.projectanalysis.duplication.Duplication;
import org.sonar.server.computation.task.projectanalysis.duplication.InProjectDuplicate;
import org.sonar.server.computation.task.projectanalysis.duplication.InnerDuplicate;
import org.sonar.server.computation.task.projectanalysis.duplication.TextBlock;

import static org.assertj.core.api.Assertions.assertThat;

public class DuplicationLineReaderTest {

  DbFileSources.Data.Builder sourceData = DbFileSources.Data.newBuilder();
  DbFileSources.Line.Builder line1 = sourceData.addLinesBuilder().setSource("line1").setLine(1);
  DbFileSources.Line.Builder line2 = sourceData.addLinesBuilder().setSource("line2").setLine(2);
  DbFileSources.Line.Builder line3 = sourceData.addLinesBuilder().setSource("line3").setLine(3);
  DbFileSources.Line.Builder line4 = sourceData.addLinesBuilder().setSource("line4").setLine(4);

  @Test
  public void read_nothing() {
    DuplicationLineReader reader = new DuplicationLineReader(Collections.emptySet());

    reader.read(line1);

    assertThat(line1.getDuplicationList()).isEmpty();
  }

  @Test
  public void read_duplication_with_duplicates_on_same_file() {
    DuplicationLineReader reader = duplicationLineReader(duplication(1, 2, innerDuplicate(3, 4)));

    reader.read(line1);
    reader.read(line2);
    reader.read(line3);
    reader.read(line4);

    assertThat(line1.getDuplicationList()).containsExactly(1);
    assertThat(line2.getDuplicationList()).containsExactly(1);
    assertThat(line3.getDuplicationList()).containsExactly(2);
    assertThat(line4.getDuplicationList()).containsExactly(2);
  }

  @Test
  public void read_duplication_with_duplicates_on_other_file() {
    DuplicationLineReader reader = duplicationLineReader(
        duplication(
            1, 2,
            new InProjectDuplicate(fileComponent(1).build(), new TextBlock(3, 4))));

    reader.read(line1);
    reader.read(line2);
    reader.read(line3);
    reader.read(line4);

    assertThat(line1.getDuplicationList()).containsExactly(1);
    assertThat(line2.getDuplicationList()).containsExactly(1);
    assertThat(line3.getDuplicationList()).isEmpty();
    assertThat(line4.getDuplicationList()).isEmpty();
  }

  @Test
  public void read_duplication_with_duplicates_on_other_file_from_other_project() {
    DuplicationLineReader reader = duplicationLineReader(
        duplication(
            1, 2,
            new CrossProjectDuplicate("other-component-key-from-another-project", new TextBlock(3, 4))));

    reader.read(line1);
    reader.read(line2);
    reader.read(line3);
    reader.read(line4);

    assertThat(line1.getDuplicationList()).containsExactly(1);
    assertThat(line2.getDuplicationList()).containsExactly(1);
    assertThat(line3.getDuplicationList()).isEmpty();
    assertThat(line4.getDuplicationList()).isEmpty();
  }

  @Test
  public void read_many_duplications() {
    DuplicationLineReader reader = duplicationLineReader(
        duplication(
            1, 1,
            innerDuplicate(2, 2)),
        duplication(
            1, 2,
            innerDuplicate(3, 4))
    );

    reader.read(line1);
    reader.read(line2);
    reader.read(line3);
    reader.read(line4);

    assertThat(line1.getDuplicationList()).containsExactly(1, 2);
    assertThat(line2.getDuplicationList()).containsExactly(2, 3);
    assertThat(line3.getDuplicationList()).containsExactly(4);
    assertThat(line4.getDuplicationList()).containsExactly(4);
  }

  @Test
  public void should_be_sorted_by_line_block() {
    DuplicationLineReader reader = duplicationLineReader(
        duplication(
            2, 2,
            innerDuplicate(4, 4)),
        duplication(
            1, 1,
            innerDuplicate(3, 3))
    );

    reader.read(line1);
    reader.read(line2);
    reader.read(line3);
    reader.read(line4);

    assertThat(line1.getDuplicationList()).containsExactly(1);
    assertThat(line2.getDuplicationList()).containsExactly(2);
    assertThat(line3.getDuplicationList()).containsExactly(3);
    assertThat(line4.getDuplicationList()).containsExactly(4);
  }

  @Test
  public void should_be_sorted_by_line_length() {
    DuplicationLineReader reader = duplicationLineReader(
        duplication(
            1, 2,
            innerDuplicate(3, 4)),
        duplication(
            1, 1,
            innerDuplicate(4, 4)
        )
    );

    reader.read(line1);
    reader.read(line2);
    reader.read(line3);
    reader.read(line4);

    assertThat(line1.getDuplicationList()).containsExactly(1, 2);
    assertThat(line2.getDuplicationList()).containsExactly(2);
    assertThat(line3.getDuplicationList()).containsExactly(3);
    assertThat(line4.getDuplicationList()).containsExactly(3, 4);
  }

  private static ReportComponent.Builder fileComponent(int ref) {
    return ReportComponent.builder(Component.Type.FILE, ref);
  }

  private static DuplicationLineReader duplicationLineReader(Duplication... duplications) {
    return new DuplicationLineReader(ImmutableSet.copyOf(Arrays.asList(duplications)));
  }

  private static Duplication duplication(int originalStart, int originalEnd, Duplicate... duplicates) {
    return new Duplication(new TextBlock(originalStart, originalEnd), Arrays.asList(duplicates));
  }

  private static InnerDuplicate innerDuplicate(int start, int end) {
    return new InnerDuplicate(new TextBlock(start, end));
  }

}
