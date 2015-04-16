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
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;

public class SymbolsLineReaderTest {

  FileSourceDb.Data.Builder sourceData = FileSourceDb.Data.newBuilder();
  FileSourceDb.Line.Builder line1 = sourceData.addLinesBuilder().setSource("line1").setLine(1);
  FileSourceDb.Line.Builder line2 = sourceData.addLinesBuilder().setSource("line2").setLine(2);
  FileSourceDb.Line.Builder line3 = sourceData.addLinesBuilder().setSource("line3").setLine(3);
  FileSourceDb.Line.Builder line4 = sourceData.addLinesBuilder().setSource("line4").setLine(4);

  @Test
  public void read_nothing() throws Exception {
    SymbolsLineReader symbolsLineReader = new SymbolsLineReader(Collections.<BatchReport.Symbols.Symbol>emptyList());

    symbolsLineReader.read(line1);

    assertThat(line1.getSymbols()).isEmpty();
  }

  @Test
  public void read_symbols() throws Exception {
    List<BatchReport.Symbols.Symbol> symbols = newArrayList(
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1).setStartOffset(2).setEndOffset(4)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(3).setEndLine(3).setStartOffset(1).setEndOffset(3)
          .build())
        .build()
      );

    SymbolsLineReader symbolsLineReader = new SymbolsLineReader(symbols);
    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);

    assertThat(line1.getSymbols()).isEqualTo("2,4,1");
    assertThat(line2.getSymbols()).isEmpty();
    assertThat(line3.getSymbols()).isEqualTo("1,3,1");
  }

  @Test
  public void read_symbols_with_reference_on_same_line() throws Exception {
    List<BatchReport.Symbols.Symbol> symbols = newArrayList(
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1).setStartOffset(0).setEndOffset(1)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1).setStartOffset(2).setEndOffset(3)
          .build())
        .build()
      );

    SymbolsLineReader symbolsLineReader = new SymbolsLineReader(symbols);
    symbolsLineReader.read(line1);

    assertThat(line1.getSymbols()).isEqualTo("0,1,1;2,3,1");
  }

  @Test
  public void read_symbols_with_two_references() throws Exception {
    List<BatchReport.Symbols.Symbol> symbols = newArrayList(
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1).setStartOffset(2).setEndOffset(4)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(3).setEndLine(3).setStartOffset(1).setEndOffset(3)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(2).setEndLine(2).setStartOffset(0).setEndOffset(2)
          .build())
        .build()
      );

    SymbolsLineReader symbolsLineReader = new SymbolsLineReader(symbols);
    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);

    assertThat(line1.getSymbols()).isEqualTo("2,4,1");
    assertThat(line2.getSymbols()).isEqualTo("0,2,1");
    assertThat(line3.getSymbols()).isEqualTo("1,3,1");
  }

  @Test
  public void read_symbols_with_two_references_on_the_same_line() throws Exception {
    List<BatchReport.Symbols.Symbol> symbols = newArrayList(
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1).setStartOffset(2).setEndOffset(3)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(2).setEndLine(2).setStartOffset(0).setEndOffset(1)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(2).setEndLine(2).setStartOffset(2).setEndOffset(3)
          .build())
        .build()
    );

    SymbolsLineReader symbolsLineReader = new SymbolsLineReader(symbols);
    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);

    assertThat(line1.getSymbols()).isEqualTo("2,3,1");
    assertThat(line2.getSymbols()).isEqualTo("0,1,1;2,3,1");
  }

  @Test
  public void read_symbols_when_reference_line_is_before_declaration_line() throws Exception {
    List<BatchReport.Symbols.Symbol> symbols = newArrayList(
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          .setStartLine(2).setEndLine(2).setStartOffset(3).setEndOffset(4)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1).setStartOffset(1).setEndOffset(2)
          .build())
        .build()
      );

    SymbolsLineReader symbolsLineReader = new SymbolsLineReader(symbols);
    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);

    assertThat(line1.getSymbols()).isEqualTo("1,2,1");
    assertThat(line2.getSymbols()).isEqualTo("3,4,1");
  }

  @Test
  public void read_many_symbols_on_lines() throws Exception {
    List<BatchReport.Symbols.Symbol> symbols = newArrayList(
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1).setStartOffset(1).setEndOffset(2)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(3).setEndLine(3).setStartOffset(2).setEndOffset(3)
          .build())
        .build(),
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1).setStartOffset(3).setEndOffset(4)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(3).setEndLine(3).setStartOffset(0).setEndOffset(1)
          .build())
        .build()
      );

    SymbolsLineReader symbolsLineReader = new SymbolsLineReader(symbols);
    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);

    assertThat(line1.getSymbols()).isEqualTo("1,2,1;3,4,2");
    assertThat(line2.getSymbols()).isEmpty();
    assertThat(line3.getSymbols()).isEqualTo("2,3,1;0,1,2");
  }

  @Test
  public void symbol_declaration_should_be_sorted_by_offset() throws Exception {
    List<BatchReport.Symbols.Symbol> symbols = newArrayList(
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          // This symbol begins after the second symbol, it should appear in second place
          .setStartLine(1).setEndLine(1).setStartOffset(2).setEndOffset(3)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(3).setEndLine(3).setStartOffset(2).setEndOffset(3)
          .build())
        .build(),
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1).setStartOffset(0).setEndOffset(1)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(3).setEndLine(3).setStartOffset(0).setEndOffset(1)
          .build())
        .build());

    SymbolsLineReader symbolsLineReader = new SymbolsLineReader(symbols);
    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);

    assertThat(line1.getSymbols()).isEqualTo("0,1,1;2,3,2");
    assertThat(line2.getSymbols()).isEmpty();
    assertThat(line3.getSymbols()).isEqualTo("0,1,1;2,3,2");
  }

  @Test
  public void symbol_declaration_should_be_sorted_by_line() throws Exception {
    List<BatchReport.Symbols.Symbol> symbols = newArrayList(
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          // This symbol begins after the second symbol, it should appear in second place
          .setStartLine(2).setEndLine(2).setStartOffset(0).setEndOffset(1)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(3).setEndLine(3).setStartOffset(2).setEndOffset(3)
          .build())
        .build(),
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(1).setStartOffset(0).setEndOffset(1)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(3).setEndLine(3).setStartOffset(0).setEndOffset(1)
          .build())
        .build());

    SymbolsLineReader symbolsLineReader = new SymbolsLineReader(symbols);
    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);

    assertThat(line1.getSymbols()).isEqualTo("0,1,1");
    assertThat(line2.getSymbols()).isEqualTo("0,1,2");
    assertThat(line3.getSymbols()).isEqualTo("0,1,1;2,3,2");
  }

  @Test
  public void read_symbols_defined_on_many_lines() throws Exception {
    List<BatchReport.Symbols.Symbol> symbols = newArrayList(
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(2).setStartOffset(1).setEndOffset(3)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(3).setEndLine(4).setStartOffset(1).setEndOffset(3)
          .build())
        .build());

    SymbolsLineReader symbolsLineReader = new SymbolsLineReader(symbols);
    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);
    symbolsLineReader.read(line4);

    assertThat(line1.getSymbols()).isEqualTo("1,5,1");
    assertThat(line2.getSymbols()).isEqualTo("0,3,1");
    assertThat(line3.getSymbols()).isEqualTo("1,5,1");
    assertThat(line4.getSymbols()).isEqualTo("0,3,1");
  }

  @Test
  public void read_symbols_declared_on_a_whole_line() throws Exception {
    List<BatchReport.Symbols.Symbol> symbols = newArrayList(
      BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          .setStartLine(1).setEndLine(2).setStartOffset(0).setEndOffset(0)
          .build())
        .addReference(BatchReport.Range.newBuilder()
          .setStartLine(3).setEndLine(3).setStartOffset(1).setEndOffset(3)
          .build())
        .build()
    );

    SymbolsLineReader symbolsLineReader = new SymbolsLineReader(symbols);
    symbolsLineReader.read(line1);
    symbolsLineReader.read(line2);
    symbolsLineReader.read(line3);
    symbolsLineReader.read(line4);

    assertThat(line1.getSymbols()).isEqualTo("0,5,1");
    assertThat(line2.getSymbols()).isEmpty();
    assertThat(line3.getSymbols()).isEqualTo("1,3,1");
    assertThat(line4.getSymbols()).isEmpty();
  }

}
