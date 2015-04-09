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
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.source.db.FileSourceDb;

import java.util.Collections;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class HighlightingLineReaderTest {

  @Test
  public void nothing_to_read() {
    HighlightingLineReader highlightingLineReader = new HighlightingLineReader(Collections.<BatchReport.SyntaxHighlighting>emptyList().iterator());

    FileSourceDb.Line.Builder lineBuilder = FileSourceDb.Data.newBuilder().addLinesBuilder().setLine(1);
    highlightingLineReader.read(lineBuilder);

    assertThat(lineBuilder.hasHighlighting()).isFalse();
  }

  @Test
  public void read_one_line() {
    HighlightingLineReader highlightingLineReader = new HighlightingLineReader(newArrayList(
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1)
          .setStartOffset(2).setEndOffset(4)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build()
    ).iterator());

    FileSourceDb.Line.Builder lineBuilder = FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line1").setLine(1);
    highlightingLineReader.read(lineBuilder);

    assertThat(lineBuilder.getHighlighting()).isEqualTo("2,4,a");
  }

  @Test
  public void read_many_lines() {
    HighlightingLineReader highlightingLineReader = new HighlightingLineReader(newArrayList(
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1)
          .setStartOffset(0).setEndOffset(4)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build(),
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(2).setEndLine(2)
          .setStartOffset(0).setEndOffset(1)
          .build())
        .setType(Constants.HighlightingType.COMMENT)
        .build(),
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(4).setEndLine(4)
          .setStartOffset(1).setEndOffset(2)
          .build())
        .setType(Constants.HighlightingType.CONSTANT)
        .build()
    ).iterator());

    FileSourceDb.Line.Builder line1 = FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line1").setLine(1);
    highlightingLineReader.read(line1);
    FileSourceDb.Line.Builder line2 = FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line2").setLine(2);
    highlightingLineReader.read(line2);
    highlightingLineReader.read(FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line3").setLine(3));
    FileSourceDb.Line.Builder line4 = FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line4").setLine(4);
    highlightingLineReader.read(line4);

    assertThat(line1.getHighlighting()).isEqualTo("0,4,a");
    assertThat(line2.getHighlighting()).isEqualTo("0,1,cd");
    assertThat(line4.getHighlighting()).isEqualTo("1,2,c");
  }

  @Test
  public void read_many_syntax_highlighting_on_same_line() {
    HighlightingLineReader highlightingLineReader = new HighlightingLineReader(newArrayList(
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1)
          .setStartOffset(2).setEndOffset(3)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build(),
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1)
          .setStartOffset(4).setEndOffset(5)
          .build())
        .setType(Constants.HighlightingType.COMMENT)
        .build()
    ).iterator());

    FileSourceDb.Line.Builder lineBuilder = FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line1").setLine(1);
    highlightingLineReader.read(lineBuilder);

    assertThat(lineBuilder.getHighlighting()).isEqualTo("2,3,a;4,5,cd");
  }

  @Test
  public void read_nested_syntax_highlighting_on_same_line() {
    HighlightingLineReader highlightingLineReader = new HighlightingLineReader(newArrayList(
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1)
          .setStartOffset(0).setEndOffset(4)
          .build())
        .setType(Constants.HighlightingType.CONSTANT)
        .build(),
      // This highlighting is nested in previous one
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1)
          .setStartOffset(2).setEndOffset(3)
          .build())
        .setType(Constants.HighlightingType.KEYWORD)
        .build()
    ).iterator());

    FileSourceDb.Line.Builder lineBuilder = FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line1").setLine(1);
    highlightingLineReader.read(lineBuilder);

    assertThat(lineBuilder.getHighlighting()).isEqualTo("0,4,c;2,3,k");
  }

  @Test
  public void read_one_syntax_highlighting_on_many_lines() {
    HighlightingLineReader highlightingLineReader = new HighlightingLineReader(newArrayList(
      // This highlighting begin on line 1 and finish on line 3
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(3)
          .setStartOffset(3).setEndOffset(2)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build()
    ).iterator());

    FileSourceDb.Line.Builder line1 = FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line1").setLine(1);
    highlightingLineReader.read(line1);
    FileSourceDb.Line.Builder line2 = FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line 2").setLine(2);
    highlightingLineReader.read(line2);
    FileSourceDb.Line.Builder line3 = FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line3").setLine(3);
    highlightingLineReader.read(line3);

    assertThat(line1.getHighlighting()).isEqualTo("3,4,a");
    assertThat(line2.getHighlighting()).isEqualTo("0,5,a");
    assertThat(line3.getHighlighting()).isEqualTo("0,2,a");
  }

  // TODO
  @Test
  public void read_many_syntax_highlighting_on_many_lines() {
    HighlightingLineReader highlightingLineReader = new HighlightingLineReader(newArrayList(
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(3)
          .setStartOffset(3).setEndOffset(2)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build(),
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(2).setEndLine(4)
          .setStartOffset(0).setEndOffset(3)
          .build())
        .setType(Constants.HighlightingType.HIGHLIGHTING_STRING)
        .build(),
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(2).setEndLine(2)
          .setStartOffset(1).setEndOffset(2)
          .build())
        .setType(Constants.HighlightingType.COMMENT)
        .build()
    ).iterator());

    FileSourceDb.Line.Builder line1 = FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line1").setLine(1);
    highlightingLineReader.read(line1);
    FileSourceDb.Line.Builder line2 = FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line2").setLine(2);
    highlightingLineReader.read(line2);
    FileSourceDb.Line.Builder line3 = FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line3").setLine(3);
    highlightingLineReader.read(line3);
    FileSourceDb.Line.Builder line4 = FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line4").setLine(4);
    highlightingLineReader.read(line4);

    assertThat(line1.getHighlighting()).isEqualTo("3,4,a");
    assertThat(line2.getHighlighting()).isEqualTo("0,4,a;0,4,s;1,2,cd");
    assertThat(line3.getHighlighting()).isEqualTo("0,2,a;0,4,s");
    assertThat(line4.getHighlighting()).isEqualTo("0,3,s");
  }

  @Test
  public void fail_when_end_offset_is_before_start_offset() {
    HighlightingLineReader highlightingLineReader = new HighlightingLineReader(newArrayList(
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1)
          .setStartOffset(4).setEndOffset(2)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build()
    ).iterator());

    try {
      highlightingLineReader.read(FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line1").setLine(1));
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("End offset 2 cannot be defined before start offset 4 on line 1");
    }
  }

  @Test
  public void fail_when_end_offset_is_higher_than_line_length() {
    HighlightingLineReader highlightingLineReader = new HighlightingLineReader(newArrayList(
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1)
          .setStartOffset(2).setEndOffset(10)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build()
    ).iterator());

    try {
      highlightingLineReader.read(FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line1").setLine(1));
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("End offset 10 is defined outside the length (5) of the line 1");
    }
  }

  @Test
  public void fail_when_start_offset_is_higher_than_line_length() {
    HighlightingLineReader highlightingLineReader = new HighlightingLineReader(newArrayList(
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1)
          .setStartOffset(10).setEndOffset(11)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build()
    ).iterator());

    try {
      highlightingLineReader.read(FileSourceDb.Data.newBuilder().addLinesBuilder().setSource("line1").setLine(1));
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Start offset 10 is defined outside the length (5) of the line 1");
    }
  }

}
