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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.SyntaxHighlightingRule.HighlightingType;
import org.sonar.scanner.protocol.output.ScannerReport.TextRange;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.source.RangeOffsetConverter.RangeOffsetConverterException;

import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.log.LoggerLevel.WARN;
import static org.sonar.db.protobuf.DbFileSources.Data.newBuilder;
import static org.sonar.scanner.protocol.output.ScannerReport.SyntaxHighlightingRule.HighlightingType.ANNOTATION;
import static org.sonar.scanner.protocol.output.ScannerReport.SyntaxHighlightingRule.HighlightingType.COMMENT;
import static org.sonar.scanner.protocol.output.ScannerReport.SyntaxHighlightingRule.HighlightingType.CONSTANT;
import static org.sonar.scanner.protocol.output.ScannerReport.SyntaxHighlightingRule.HighlightingType.CPP_DOC;
import static org.sonar.scanner.protocol.output.ScannerReport.SyntaxHighlightingRule.HighlightingType.HIGHLIGHTING_STRING;
import static org.sonar.scanner.protocol.output.ScannerReport.SyntaxHighlightingRule.HighlightingType.KEYWORD;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

public class HighlightingLineReaderTest {

  @Rule
  public LogTester logTester = new LogTester();

  private static final Component FILE = builder(Component.Type.FILE, 1).setUuid("FILE_UUID").setKey("FILE_KEY").build();

  private static final int DEFAULT_LINE_LENGTH = 5;

  private static final int LINE_1 = 1;
  private static final int LINE_2 = 2;
  private static final int LINE_3 = 3;
  private static final int LINE_4 = 4;

  private static final String RANGE_LABEL_1 = "1,2";
  private static final String RANGE_LABEL_2 = "2,3";
  private static final String RANGE_LABEL_3 = "3,4";
  private static final String RANGE_LABEL_4 = "0,2";
  private static final String RANGE_LABEL_5 = "0,3";

  private RangeOffsetConverter rangeOffsetConverter = mock(RangeOffsetConverter.class);

  private DbFileSources.Data.Builder sourceData = newBuilder();
  private DbFileSources.Line.Builder line1 = sourceData.addLinesBuilder().setSource("line1").setLine(1);
  private DbFileSources.Line.Builder line2 = sourceData.addLinesBuilder().setSource("line2").setLine(2);
  private DbFileSources.Line.Builder line3 = sourceData.addLinesBuilder().setSource("line3").setLine(3);
  private DbFileSources.Line.Builder line4 = sourceData.addLinesBuilder().setSource("line4").setLine(4);

  @Test
  public void nothing_to_read() {
    HighlightingLineReader highlightingLineReader = newReader(Collections.emptyMap());

    DbFileSources.Line.Builder lineBuilder = newBuilder().addLinesBuilder().setLine(1);
    highlightingLineReader.read(lineBuilder);

    assertThat(lineBuilder.hasHighlighting()).isFalse();
  }

  @Test
  public void read_one_line() {
    HighlightingLineReader highlightingLineReader = newReader(of(
      newSingleLineTextRangeWithExpectingLabel(LINE_1, RANGE_LABEL_1), ANNOTATION));

    highlightingLineReader.read(line1);

    assertThat(line1.getHighlighting()).isEqualTo(RANGE_LABEL_1 + ",a");
  }

  @Test
  public void read_many_lines() {
    HighlightingLineReader highlightingLineReader = newReader(of(
      newSingleLineTextRangeWithExpectingLabel(LINE_1, RANGE_LABEL_1), ANNOTATION,
      newSingleLineTextRangeWithExpectingLabel(LINE_2, RANGE_LABEL_2), COMMENT,
      newSingleLineTextRangeWithExpectingLabel(LINE_4, RANGE_LABEL_3), CONSTANT));

    highlightingLineReader.read(line1);
    highlightingLineReader.read(line2);
    highlightingLineReader.read(line3);
    highlightingLineReader.read(line4);

    assertThat(line1.getHighlighting()).isEqualTo(RANGE_LABEL_1 + ",a");
    assertThat(line2.getHighlighting()).isEqualTo(RANGE_LABEL_2 + ",cd");
    assertThat(line4.getHighlighting()).isEqualTo(RANGE_LABEL_3 + ",c");
  }

  @Test
  public void supports_highlighting_over_multiple_lines_including_an_empty_one() {
    List<ScannerReport.SyntaxHighlightingRule> syntaxHighlightingList = new ArrayList<>();
    addHighlighting(syntaxHighlightingList, 1, 0, 1, 7, KEYWORD); // package
    addHighlighting(syntaxHighlightingList, 2, 0, 4, 6, CPP_DOC); // comment over 3 lines
    addHighlighting(syntaxHighlightingList, 5, 0, 5, 6, KEYWORD); // public
    addHighlighting(syntaxHighlightingList, 5, 7, 5, 12, KEYWORD); // class
    HighlightingLineReader highlightingLineReader = new HighlightingLineReader(FILE, syntaxHighlightingList.iterator(), new RangeOffsetConverter());

    DbFileSources.Line.Builder[] builders = new DbFileSources.Line.Builder[] {
      addSourceLine(highlightingLineReader, 1, "package example;"),
      addSourceLine(highlightingLineReader, 2, "/*"),
      addSourceLine(highlightingLineReader, 3, ""),
      addSourceLine(highlightingLineReader, 4, " foo*/"),
      addSourceLine(highlightingLineReader, 5, "public class One {"),
      addSourceLine(highlightingLineReader, 6, "}")
    };

    assertThat(builders)
      .extracting("highlighting")
      .containsExactly(
        "0,7,k",
        "0,2,cppd",
        "",
        "0,6,cppd",
        "0,6,k;7,12,k",
        "");
  }

  private DbFileSources.Line.Builder addSourceLine(HighlightingLineReader highlightingLineReader, int line, String source) {
    DbFileSources.Line.Builder lineBuilder = sourceData.addLinesBuilder().setSource(source).setLine(line);
    highlightingLineReader.read(lineBuilder);
    return lineBuilder;
  }

  private void addHighlighting(List<ScannerReport.SyntaxHighlightingRule> syntaxHighlightingList,
    int startLine, int startOffset,
    int endLine, int endOffset,
    HighlightingType type) {
    TextRange.Builder textRangeBuilder = TextRange.newBuilder();
    ScannerReport.SyntaxHighlightingRule.Builder ruleBuilder = ScannerReport.SyntaxHighlightingRule.newBuilder();
    syntaxHighlightingList.add(ruleBuilder
      .setRange(textRangeBuilder
        .setStartLine(startLine).setEndLine(endLine)
        .setStartOffset(startOffset).setEndOffset(endOffset)
        .build())
      .setType(type)
      .build());
  }

  @Test
  public void read_many_syntax_highlighting_on_same_line() {
    HighlightingLineReader highlightingLineReader = newReader(of(
      newSingleLineTextRangeWithExpectingLabel(LINE_1, RANGE_LABEL_1), ANNOTATION,
      newSingleLineTextRangeWithExpectingLabel(LINE_1, RANGE_LABEL_2), COMMENT));

    highlightingLineReader.read(line1);

    assertThat(line1.getHighlighting()).isEqualTo(RANGE_LABEL_1 + ",a;" + RANGE_LABEL_2 + ",cd");
  }

  @Test
  public void read_one_syntax_highlighting_on_many_lines() {
    // This highlighting begin on line 1 and finish on line 3
    TextRange textRange = newTextRange(LINE_1, LINE_3);
    when(rangeOffsetConverter.offsetToString(textRange, LINE_1, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_1);
    when(rangeOffsetConverter.offsetToString(textRange, LINE_2, 6)).thenReturn(RANGE_LABEL_2);
    when(rangeOffsetConverter.offsetToString(textRange, LINE_3, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_3);

    HighlightingLineReader highlightingLineReader = newReader(of(textRange, ANNOTATION));

    highlightingLineReader.read(line1);
    DbFileSources.Line.Builder line2 = sourceData.addLinesBuilder().setSource("line 2").setLine(2);
    highlightingLineReader.read(line2);
    highlightingLineReader.read(line3);

    assertThat(line1.getHighlighting()).isEqualTo(RANGE_LABEL_1 + ",a");
    assertThat(line2.getHighlighting()).isEqualTo(RANGE_LABEL_2 + ",a");
    assertThat(line3.getHighlighting()).isEqualTo(RANGE_LABEL_3 + ",a");
  }

  @Test
  public void read_many_syntax_highlighting_on_many_lines() {
    TextRange textRange1 = newTextRange(LINE_1, LINE_3);
    when(rangeOffsetConverter.offsetToString(textRange1, LINE_1, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_1);
    when(rangeOffsetConverter.offsetToString(textRange1, LINE_2, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_2);
    when(rangeOffsetConverter.offsetToString(textRange1, LINE_3, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_3);

    TextRange textRange2 = newTextRange(LINE_2, LINE_4);
    when(rangeOffsetConverter.offsetToString(textRange2, LINE_2, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_2);
    when(rangeOffsetConverter.offsetToString(textRange2, LINE_3, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_2);
    when(rangeOffsetConverter.offsetToString(textRange2, LINE_4, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_4);

    TextRange textRange3 = newTextRange(LINE_2, LINE_2);
    when(rangeOffsetConverter.offsetToString(textRange3, LINE_2, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_5);

    HighlightingLineReader highlightingLineReader = newReader(of(
      textRange1, ANNOTATION,
      textRange2, HIGHLIGHTING_STRING,
      textRange3, COMMENT));

    highlightingLineReader.read(line1);
    highlightingLineReader.read(line2);
    highlightingLineReader.read(line3);
    highlightingLineReader.read(line4);

    assertThat(line1.getHighlighting()).isEqualTo(RANGE_LABEL_1 + ",a");
    assertThat(line2.getHighlighting()).isEqualTo(RANGE_LABEL_2 + ",a;" + RANGE_LABEL_2 + ",s;" + RANGE_LABEL_5 + ",cd");
    assertThat(line3.getHighlighting()).isEqualTo(RANGE_LABEL_3 + ",a;" + RANGE_LABEL_2 + ",s");
    assertThat(line4.getHighlighting()).isEqualTo(RANGE_LABEL_4 + ",s");
  }

  @Test
  public void read_highlighting_declared_on_a_whole_line() {
    TextRange textRange = newTextRange(LINE_1, LINE_2);
    when(rangeOffsetConverter.offsetToString(textRange, LINE_1, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_1);
    when(rangeOffsetConverter.offsetToString(textRange, LINE_2, DEFAULT_LINE_LENGTH)).thenReturn("");

    HighlightingLineReader highlightingLineReader = newReader(of(textRange, ANNOTATION));

    highlightingLineReader.read(line1);
    highlightingLineReader.read(line2);
    highlightingLineReader.read(line3);

    assertThat(line1.getHighlighting()).isEqualTo(RANGE_LABEL_1 + ",a");
    // Nothing should be set on line 2
    assertThat(line2.getHighlighting()).isEmpty();
    assertThat(line3.getHighlighting()).isEmpty();
  }

  @Test
  public void not_fail_and_stop_processing_when_range_offset_converter_throw_RangeOffsetConverterException() {
    TextRange textRange1 = newTextRange(LINE_1, LINE_1);
    doThrow(RangeOffsetConverterException.class).when(rangeOffsetConverter).offsetToString(textRange1, LINE_1, DEFAULT_LINE_LENGTH);

    HighlightingLineReader highlightingLineReader = newReader(of(
      textRange1, HighlightingType.ANNOTATION,
      newSingleLineTextRangeWithExpectingLabel(LINE_2, RANGE_LABEL_1), HIGHLIGHTING_STRING));

    highlightingLineReader.read(line1);
    highlightingLineReader.read(line2);

    assertNoHighlighting();
    assertThat(logTester.logs(WARN)).isNotEmpty();
  }

  @Test
  public void keep_existing_processed_highlighting_when_range_offset_converter_throw_RangeOffsetConverterException() {
    TextRange textRange2 = newTextRange(LINE_2, LINE_2);
    doThrow(RangeOffsetConverterException.class).when(rangeOffsetConverter).offsetToString(textRange2, LINE_2, DEFAULT_LINE_LENGTH);

    HighlightingLineReader highlightingLineReader = newReader(of(
      newSingleLineTextRangeWithExpectingLabel(LINE_1, RANGE_LABEL_1), ANNOTATION,
      textRange2, HIGHLIGHTING_STRING));

    highlightingLineReader.read(line1);
    highlightingLineReader.read(line2);

    assertThat(line1.hasHighlighting()).isTrue();
    assertThat(line2.hasHighlighting()).isFalse();
    assertThat(logTester.logs(WARN)).isNotEmpty();
  }

  @Test
  public void display_file_key_in_warning_when_range_offset_converter_throw_RangeOffsetConverterException() {
    TextRange textRange1 = newTextRange(LINE_1, LINE_1);
    doThrow(RangeOffsetConverterException.class).when(rangeOffsetConverter).offsetToString(textRange1, LINE_1, DEFAULT_LINE_LENGTH);
    HighlightingLineReader highlightingLineReader = newReader(of(textRange1, ANNOTATION));

    highlightingLineReader.read(line1);

    assertThat(logTester.logs(WARN)).containsOnly("Inconsistency detected in Highlighting data. Highlighting will be ignored for file 'FILE_KEY'");
  }

  private HighlightingLineReader newReader(Map<TextRange, HighlightingType> textRangeByType) {
    List<ScannerReport.SyntaxHighlightingRule> syntaxHighlightingList = new ArrayList<>();
    for (Map.Entry<TextRange, HighlightingType> entry : textRangeByType.entrySet()) {
      syntaxHighlightingList.add(ScannerReport.SyntaxHighlightingRule.newBuilder()
        .setRange(entry.getKey())
        .setType(entry.getValue())
        .build());
    }
    return new HighlightingLineReader(FILE, syntaxHighlightingList.iterator(), rangeOffsetConverter);
  }

  private static TextRange newTextRange(int startLine, int enLine) {
    Random random = new Random();
    return TextRange.newBuilder()
      .setStartLine(startLine).setEndLine(enLine)
      // Offsets are not used by the reader
      .setStartOffset(random.nextInt()).setEndOffset(random.nextInt())
      .build();
  }

  private TextRange newSingleLineTextRangeWithExpectingLabel(int line, String rangeLabel) {
    TextRange textRange = newTextRange(line, line);
    when(rangeOffsetConverter.offsetToString(textRange, line, DEFAULT_LINE_LENGTH)).thenReturn(rangeLabel);
    return textRange;
  }

  private void assertNoHighlighting() {
    assertThat(line1.hasHighlighting()).isFalse();
    assertThat(line2.hasHighlighting()).isFalse();
    assertThat(line3.hasHighlighting()).isFalse();
    assertThat(line4.hasHighlighting()).isFalse();
  }

}
