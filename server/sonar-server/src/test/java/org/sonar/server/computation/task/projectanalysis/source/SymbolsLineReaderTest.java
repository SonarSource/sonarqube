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

import java.util.Arrays;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.TextRange;
import org.sonar.server.computation.task.projectanalysis.component.Component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.utils.log.LoggerLevel.WARN;
import static org.sonar.server.computation.task.projectanalysis.component.ReportComponent.builder;

public class SymbolsLineReaderTest {

  @Rule
  public LogTester logTester = new LogTester();

  private static final Component FILE = builder(Component.Type.FILE, 1).setUuid("FILE_UUID").setKey("FILE_KEY").build();

  private static final int DEFAULT_LINE_LENGTH = 5;

  private static final int LINE_1 = 1;
  private static final int LINE_2 = 2;
  private static final int LINE_3 = 3;
  private static final int LINE_4 = 4;

  private static final int OFFSET_0 = 0;
  private static final int OFFSET_1 = 1;
  private static final int OFFSET_2 = 2;
  private static final int OFFSET_3 = 3;
  private static final int OFFSET_4 = 4;

  private static final String RANGE_LABEL_1 = "1,2";
  private static final String RANGE_LABEL_2 = "2,3";
  private static final String RANGE_LABEL_3 = "3,4";
  private static final String RANGE_LABEL_4 = "0,2";

  private RangeOffsetConverter rangeOffsetConverter = mock(RangeOffsetConverter.class);

  private DbFileSources.Data.Builder sourceData = DbFileSources.Data.newBuilder();
  private DbFileSources.Line.Builder line1 = sourceData.addLinesBuilder().setSource("line1").setLine(1);
  private DbFileSources.Line.Builder line2 = sourceData.addLinesBuilder().setSource("line2").setLine(2);
  private DbFileSources.Line.Builder line3 = sourceData.addLinesBuilder().setSource("line3").setLine(3);
  private DbFileSources.Line.Builder line4 = sourceData.addLinesBuilder().setSource("line4").setLine(4);

  @Test
  public void read_nothing() {
    SymbolsLineReader symbolsLineReader = newReader();

    symbolsLineReader.read(line1);

    assertThat(line1.getSymbols()).isEmpty();
  }

  @Test
  public void read_symbols() {
    SymbolsLineReader symbolsLineReader = newReader(newSymbol(
      newSingleLineTextRangeWithExpectedLabel(LINE_1, OFFSET_2, OFFSET_4, RANGE_LABEL_1),
      newSingleLineTextRangeWithExpectedLabel(LINE_3, OFFSET_1, OFFSET_3, RANGE_LABEL_2)
      ));

    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);

    assertThat(line1.getSymbols()).isEqualTo(RANGE_LABEL_1 + ",1");
    assertThat(line2.getSymbols()).isEmpty();
    assertThat(line3.getSymbols()).isEqualTo(RANGE_LABEL_2 + ",1");
  }

  @Test
  public void read_symbols_with_reference_on_same_line() {
    SymbolsLineReader symbolsLineReader = newReader(newSymbol(
      newSingleLineTextRangeWithExpectedLabel(LINE_1, OFFSET_0, OFFSET_1, RANGE_LABEL_1),
      newSingleLineTextRangeWithExpectedLabel(LINE_1, OFFSET_2, OFFSET_3, RANGE_LABEL_2)
      ));

    symbolsLineReader.read(line1);

    assertThat(line1.getSymbols()).isEqualTo(RANGE_LABEL_1 + ",1;" + RANGE_LABEL_2 + ",1");
  }

  @Test
  public void read_symbols_with_two_references() {
    SymbolsLineReader symbolsLineReader = newReader(newSymbol(
      newSingleLineTextRangeWithExpectedLabel(LINE_1, OFFSET_2, OFFSET_4, RANGE_LABEL_1),
      newSingleLineTextRangeWithExpectedLabel(LINE_3, OFFSET_1, OFFSET_3, RANGE_LABEL_2),
      newSingleLineTextRangeWithExpectedLabel(LINE_2, OFFSET_0, OFFSET_2, RANGE_LABEL_3)
      ));

    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);

    assertThat(line1.getSymbols()).isEqualTo(RANGE_LABEL_1 + ",1");
    assertThat(line2.getSymbols()).isEqualTo(RANGE_LABEL_3 + ",1");
    assertThat(line3.getSymbols()).isEqualTo(RANGE_LABEL_2 + ",1");
  }

  @Test
  public void read_symbols_with_two_references_on_the_same_line() {
    SymbolsLineReader symbolsLineReader = newReader(newSymbol(
      newSingleLineTextRangeWithExpectedLabel(LINE_1, OFFSET_2, OFFSET_3, RANGE_LABEL_1),
      newSingleLineTextRangeWithExpectedLabel(LINE_2, OFFSET_0, OFFSET_1, RANGE_LABEL_2),
      newSingleLineTextRangeWithExpectedLabel(LINE_2, OFFSET_2, OFFSET_3, RANGE_LABEL_3)
      ));

    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);

    assertThat(line1.getSymbols()).isEqualTo(RANGE_LABEL_1 + ",1");
    assertThat(line2.getSymbols()).isEqualTo(RANGE_LABEL_2 + ",1;" + RANGE_LABEL_3 + ",1");
  }

  @Test
  public void read_symbols_when_reference_line_is_before_declaration_line() {
    SymbolsLineReader symbolsLineReader = newReader(newSymbol(
      newSingleLineTextRangeWithExpectedLabel(LINE_2, OFFSET_3, OFFSET_4, RANGE_LABEL_1),
      newSingleLineTextRangeWithExpectedLabel(LINE_1, OFFSET_1, OFFSET_2, RANGE_LABEL_2)
      ));

    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);

    assertThat(line1.getSymbols()).isEqualTo(RANGE_LABEL_2 + ",1");
    assertThat(line2.getSymbols()).isEqualTo(RANGE_LABEL_1 + ",1");
  }

  @Test
  public void read_many_symbols_on_lines() {
    SymbolsLineReader symbolsLineReader = newReader(
      newSymbol(
        newSingleLineTextRangeWithExpectedLabel(LINE_1, OFFSET_1, OFFSET_2, RANGE_LABEL_1),
        newSingleLineTextRangeWithExpectedLabel(LINE_3, OFFSET_2, OFFSET_3, RANGE_LABEL_2)),
      newSymbol(
        newSingleLineTextRangeWithExpectedLabel(LINE_1, OFFSET_3, OFFSET_4, RANGE_LABEL_3),
        newSingleLineTextRangeWithExpectedLabel(LINE_3, OFFSET_0, OFFSET_1, RANGE_LABEL_4)
      ));

    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);

    assertThat(line1.getSymbols()).isEqualTo(RANGE_LABEL_1 + ",1;" + RANGE_LABEL_3 + ",2");
    assertThat(line2.getSymbols()).isEmpty();
    assertThat(line3.getSymbols()).isEqualTo(RANGE_LABEL_2 + ",1;" + RANGE_LABEL_4 + ",2");
  }

  @Test
  public void symbol_declaration_should_be_sorted_by_offset() {
    SymbolsLineReader symbolsLineReader = newReader(
      newSymbol(
        // This symbol begins after the second symbol, it should appear in second place
        newSingleLineTextRangeWithExpectedLabel(LINE_1, OFFSET_2, OFFSET_3, RANGE_LABEL_1),
        newSingleLineTextRangeWithExpectedLabel(LINE_3, OFFSET_2, OFFSET_3, RANGE_LABEL_1)),
      newSymbol(
        newSingleLineTextRangeWithExpectedLabel(LINE_1, OFFSET_0, OFFSET_1, RANGE_LABEL_2),
        newSingleLineTextRangeWithExpectedLabel(LINE_3, OFFSET_0, OFFSET_1, RANGE_LABEL_2)
      ));

    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);

    assertThat(line1.getSymbols()).isEqualTo(RANGE_LABEL_2 + ",1;" + RANGE_LABEL_1 + ",2");
    assertThat(line2.getSymbols()).isEmpty();
    assertThat(line3.getSymbols()).isEqualTo(RANGE_LABEL_2 + ",1;" + RANGE_LABEL_1 + ",2");
  }

  @Test
  public void symbol_declaration_should_be_sorted_by_line() {
    SymbolsLineReader symbolsLineReader = newReader(
      newSymbol(
        newSingleLineTextRangeWithExpectedLabel(LINE_2, OFFSET_0, OFFSET_1, RANGE_LABEL_1),
        newSingleLineTextRangeWithExpectedLabel(LINE_3, OFFSET_2, OFFSET_3, RANGE_LABEL_2)),
      newSymbol(
        newSingleLineTextRangeWithExpectedLabel(LINE_1, OFFSET_0, OFFSET_1, RANGE_LABEL_1),
        newSingleLineTextRangeWithExpectedLabel(LINE_3, OFFSET_0, OFFSET_1, RANGE_LABEL_1)
      ));

    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);

    assertThat(line1.getSymbols()).isEqualTo(RANGE_LABEL_1 + ",1");
    assertThat(line2.getSymbols()).isEqualTo(RANGE_LABEL_1 + ",2");
    assertThat(line3.getSymbols()).isEqualTo(RANGE_LABEL_1 + ",1;" + RANGE_LABEL_2 + ",2");
  }

  @Test
  public void read_symbols_defined_on_many_lines() {
    TextRange declaration = newTextRange(LINE_1, LINE_2, OFFSET_1, OFFSET_3);
    when(rangeOffsetConverter.offsetToString(declaration, LINE_1, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_1);
    when(rangeOffsetConverter.offsetToString(declaration, LINE_2, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_2);

    TextRange reference = newTextRange(LINE_3, LINE_4, OFFSET_1, OFFSET_3);
    when(rangeOffsetConverter.offsetToString(reference, LINE_3, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_1);
    when(rangeOffsetConverter.offsetToString(reference, LINE_4, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_2);

    SymbolsLineReader symbolsLineReader = newReader(newSymbol(declaration, reference));

    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);
    symbolsLineReader.read(line4);

    assertThat(line1.getSymbols()).isEqualTo(RANGE_LABEL_1 + ",1");
    assertThat(line2.getSymbols()).isEqualTo(RANGE_LABEL_2 + ",1");
    assertThat(line3.getSymbols()).isEqualTo(RANGE_LABEL_1 + ",1");
    assertThat(line4.getSymbols()).isEqualTo(RANGE_LABEL_2 + ",1");
  }

  @Test
  public void read_symbols_declared_on_a_whole_line() {
    TextRange declaration = newTextRange(LINE_1, LINE_2, OFFSET_0, OFFSET_0);
    when(rangeOffsetConverter.offsetToString(declaration, LINE_1, DEFAULT_LINE_LENGTH)).thenReturn(RANGE_LABEL_1);
    when(rangeOffsetConverter.offsetToString(declaration, LINE_2, DEFAULT_LINE_LENGTH)).thenReturn("");
    TextRange reference = newSingleLineTextRangeWithExpectedLabel(LINE_3, OFFSET_1, OFFSET_3, RANGE_LABEL_2);

    SymbolsLineReader symbolsLineReader = newReader(newSymbol(declaration, reference));

    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);
    symbolsLineReader.read(line4);

    assertThat(line1.getSymbols()).isEqualTo(RANGE_LABEL_1 + ",1");
    assertThat(line2.getSymbols()).isEmpty();
    assertThat(line3.getSymbols()).isEqualTo(RANGE_LABEL_2 + ",1");
    assertThat(line4.getSymbols()).isEmpty();
  }

  @Test
  public void not_fail_and_stop_processing_when_range_offset_converter_throw_RangeOffsetConverterException() {
    TextRange declaration = newTextRange(LINE_1, LINE_1, OFFSET_1, OFFSET_3);
    doThrow(RangeOffsetConverter.RangeOffsetConverterException.class).when(rangeOffsetConverter).offsetToString(declaration, LINE_1, DEFAULT_LINE_LENGTH);

    TextRange reference = newSingleLineTextRangeWithExpectedLabel(LINE_2, OFFSET_1, OFFSET_3, RANGE_LABEL_2);

    SymbolsLineReader symbolsLineReader = newReader(newSymbol(declaration, reference));

    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);

    assertNoSymbol();
    assertThat(logTester.logs(WARN)).isNotEmpty();
  }

  @Test
  public void keep_existing_processed_symbols_when_range_offset_converter_throw_RangeOffsetConverterException() {
    TextRange declaration = newSingleLineTextRangeWithExpectedLabel(LINE_1, OFFSET_1, OFFSET_3, RANGE_LABEL_2);

    TextRange reference = newTextRange(LINE_2, LINE_2, OFFSET_1, OFFSET_3);
    doThrow(RangeOffsetConverter.RangeOffsetConverterException.class).when(rangeOffsetConverter).offsetToString(reference, LINE_2, DEFAULT_LINE_LENGTH);

    SymbolsLineReader symbolsLineReader = newReader(newSymbol(declaration, reference));

    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);

    assertThat(line1.hasSymbols()).isTrue();
    assertThat(line2.hasSymbols()).isFalse();
    assertThat(logTester.logs(WARN)).isNotEmpty();
  }

  @Test
  public void display_file_key_in_warning_when_range_offset_converter_throw_RangeOffsetConverterException() {
    TextRange declaration = newTextRange(LINE_1, LINE_1, OFFSET_1, OFFSET_3);
    doThrow(RangeOffsetConverter.RangeOffsetConverterException.class).when(rangeOffsetConverter).offsetToString(declaration, LINE_1, DEFAULT_LINE_LENGTH);
    SymbolsLineReader symbolsLineReader = newReader(newSymbol(declaration, newSingleLineTextRangeWithExpectedLabel(LINE_2, OFFSET_1, OFFSET_3, RANGE_LABEL_2)));

    symbolsLineReader.read(line1);

    assertThat(logTester.logs(WARN)).containsOnly("Inconsistency detected in Symbols data. Symbols will be ignored for file 'FILE_KEY'");
  }

  private ScannerReport.Symbol newSymbol(TextRange declaration, TextRange... references) {
    ScannerReport.Symbol.Builder builder = ScannerReport.Symbol.newBuilder()
      .setDeclaration(declaration);
    for (TextRange reference : references) {
      builder.addReference(reference);
    }
    return builder.build();
  }

  private SymbolsLineReader newReader(ScannerReport.Symbol... symbols) {
    return new SymbolsLineReader(FILE, Arrays.asList(symbols).iterator(), rangeOffsetConverter);
  }

  private TextRange newSingleLineTextRangeWithExpectedLabel(int line, int startOffset, int endOffset, String rangeLabel) {
    TextRange textRange = newTextRange(line, line, startOffset, endOffset);
    when(rangeOffsetConverter.offsetToString(textRange, line, DEFAULT_LINE_LENGTH)).thenReturn(rangeLabel);
    return textRange;
  }

  private static TextRange newTextRange(int startLine, int endLine, int startOffset, int endOffset) {
    return TextRange.newBuilder()
      .setStartLine(startLine).setEndLine(endLine)
      .setStartOffset(startOffset).setEndOffset(endOffset)
      .build();
  }

  private void assertNoSymbol() {
    assertThat(line1.hasSymbols()).isFalse();
    assertThat(line2.hasSymbols()).isFalse();
    assertThat(line3.hasSymbols()).isFalse();
    assertThat(line4.hasSymbols()).isFalse();
  }

}
