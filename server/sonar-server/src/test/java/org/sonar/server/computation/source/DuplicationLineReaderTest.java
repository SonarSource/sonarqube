/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.source;

import org.junit.Test;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.source.db.FileSourceDb;

import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class DuplicationLineReaderTest {

  FileSourceDb.Data.Builder sourceData = FileSourceDb.Data.newBuilder();
  FileSourceDb.Line.Builder line1 = sourceData.addLinesBuilder().setSource("line1").setLine(1);
  FileSourceDb.Line.Builder line2 = sourceData.addLinesBuilder().setSource("line2").setLine(2);
  FileSourceDb.Line.Builder line3 = sourceData.addLinesBuilder().setSource("line3").setLine(3);
  FileSourceDb.Line.Builder line4 = sourceData.addLinesBuilder().setSource("line4").setLine(4);

  @Test
  public void read_nothing() throws Exception {
    DuplicationLineReader reader = new DuplicationLineReader(Collections.<BatchReport.Duplication>emptyList());

    reader.read(line1);

    assertThat(line1.getDuplicationList()).isEmpty();
  }

  @Test
  public void read_duplication_with_duplicates_on_same_file() throws Exception {
    DuplicationLineReader reader = new DuplicationLineReader(newArrayList(
      BatchReport.Duplication.newBuilder()
        .setOriginPosition(BatchReport.Range.newBuilder()
          .setStartLine(1)
          .setEndLine(2)
          .build())
        .addDuplicate(BatchReport.Duplicate.newBuilder()
          .setRange(BatchReport.Range.newBuilder()
            .setStartLine(3)
            .setEndLine(4)
            .build())
          .build())
        .build()
      ));

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
  public void read_duplication_with_duplicates_on_other_file() throws Exception {
    DuplicationLineReader reader = new DuplicationLineReader(newArrayList(
      BatchReport.Duplication.newBuilder()
        .setOriginPosition(BatchReport.Range.newBuilder()
          .setStartLine(1)
          .setEndLine(2)
          .build())
        .addDuplicate(BatchReport.Duplicate.newBuilder()
          .setOtherFileRef(2)
          .setRange(BatchReport.Range.newBuilder()
            .setStartLine(3)
            .setEndLine(4)
            .build())
          .build())
        .build()
      ));

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
  public void read_duplication_with_duplicates_on_other_file_from_other_project() throws Exception {
    DuplicationLineReader reader = new DuplicationLineReader(newArrayList(
      BatchReport.Duplication.newBuilder()
        .setOriginPosition(BatchReport.Range.newBuilder()
          .setStartLine(1)
          .setEndLine(2)
          .build())
        .addDuplicate(BatchReport.Duplicate.newBuilder()
          .setOtherFileKey("other-component-key-from-another-project")
          .setRange(BatchReport.Range.newBuilder()
            .setStartLine(3)
            .setEndLine(4)
            .build())
          .build())
        .build()
      ));

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
  public void read_many_duplications() throws Exception {
    DuplicationLineReader reader = new DuplicationLineReader(newArrayList(
      BatchReport.Duplication.newBuilder()
        .setOriginPosition(BatchReport.Range.newBuilder()
          .setStartLine(1)
          .setEndLine(1)
          .build())
        .addDuplicate(BatchReport.Duplicate.newBuilder()
          .setRange(BatchReport.Range.newBuilder()
            .setStartLine(2)
            .setEndLine(2)
            .build())
          .build())
        .build(),
      BatchReport.Duplication.newBuilder()
        .setOriginPosition(BatchReport.Range.newBuilder()
          .setStartLine(1)
          .setEndLine(2)
          .build())
        .addDuplicate(BatchReport.Duplicate.newBuilder()
          .setRange(BatchReport.Range.newBuilder()
            .setStartLine(3)
            .setEndLine(4)
            .build())
          .build())
        .build()
      ));

    reader.read(line1);
    reader.read(line2);
    reader.read(line3);
    reader.read(line4);

    assertThat(line1.getDuplicationList()).containsExactly(1, 3);
    assertThat(line2.getDuplicationList()).containsExactly(2, 3);
    assertThat(line3.getDuplicationList()).containsExactly(4);
    assertThat(line4.getDuplicationList()).containsExactly(4);
  }

  @Test
  public void should_be_sorted_by_line_block() throws Exception {
    DuplicationLineReader reader = new DuplicationLineReader(newArrayList(
      BatchReport.Duplication.newBuilder()
        .setOriginPosition(BatchReport.Range.newBuilder()
          .setStartLine(2)
          .setEndLine(2)
          .build())
        .addDuplicate(BatchReport.Duplicate.newBuilder()
          .setRange(BatchReport.Range.newBuilder()
            .setStartLine(4)
            .setEndLine(4)
            .build())
          .build())
        .build(),
      BatchReport.Duplication.newBuilder()
        .setOriginPosition(BatchReport.Range.newBuilder()
          .setStartLine(1)
          .setEndLine(1)
          .build())
        .addDuplicate(BatchReport.Duplicate.newBuilder()
          .setRange(BatchReport.Range.newBuilder()
            .setStartLine(3)
            .setEndLine(3)
            .build())
          .build())
        .build()
      ));

    reader.read(line1);
    reader.read(line2);
    reader.read(line3);
    reader.read(line4);

    assertThat(line1.getDuplicationList()).containsExactly(1);
    assertThat(line2.getDuplicationList()).containsExactly(3);
    assertThat(line3.getDuplicationList()).containsExactly(2);
    assertThat(line4.getDuplicationList()).containsExactly(4);
  }

  @Test
  public void should_be_sorted_by_line_length() throws Exception {
    DuplicationLineReader reader = new DuplicationLineReader(newArrayList(
      BatchReport.Duplication.newBuilder()
        .setOriginPosition(BatchReport.Range.newBuilder()
          .setStartLine(1)
          .setEndLine(2)
          .build())
        .addDuplicate(BatchReport.Duplicate.newBuilder()
          .setRange(BatchReport.Range.newBuilder()
            .setStartLine(3)
            .setEndLine(4)
            .build())
          .build())
        .build(),
      BatchReport.Duplication.newBuilder()
        .setOriginPosition(BatchReport.Range.newBuilder()
          .setStartLine(1)
          .setEndLine(1)
          .build())
        .addDuplicate(BatchReport.Duplicate.newBuilder()
          .setRange(BatchReport.Range.newBuilder()
            .setStartLine(4)
            .setEndLine(4)
            .build())
          .build())
        .build()
      ));

    reader.read(line1);
    reader.read(line2);
    reader.read(line3);
    reader.read(line4);

    assertThat(line1.getDuplicationList()).containsExactly(1, 3);
    assertThat(line2.getDuplicationList()).containsExactly(3);
    assertThat(line3.getDuplicationList()).containsExactly(4);
    assertThat(line4.getDuplicationList()).containsExactly(2, 4);
  }

}
