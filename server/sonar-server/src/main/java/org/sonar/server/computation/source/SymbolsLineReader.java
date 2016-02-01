/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.server.computation.source;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.server.computation.component.Component;

import static java.lang.String.format;
import static org.sonar.server.computation.source.RangeOffsetConverter.OFFSET_SEPARATOR;
import static org.sonar.server.computation.source.RangeOffsetConverter.SYMBOLS_SEPARATOR;

public class SymbolsLineReader implements LineReader {

  private static final Logger LOG = Loggers.get(HighlightingLineReader.class);

  private final Component file;
  private final RangeOffsetConverter rangeOffsetConverter;
  private final Map<BatchReport.Symbol, Integer> idsBySymbol;
  private final SetMultimap<Integer, BatchReport.Symbol> symbolsPerLine;

  private boolean areSymbolsValid = true;

  public SymbolsLineReader(Component file, Iterator<BatchReport.Symbol> symbolIterator, RangeOffsetConverter rangeOffsetConverter) {
    this.file = file;
    this.rangeOffsetConverter = rangeOffsetConverter;
    List<BatchReport.Symbol> symbols = Lists.newArrayList(symbolIterator);
    // Sort symbols to have deterministic id generation
    Collections.sort(symbols, SymbolsComparator.INSTANCE);

    this.idsBySymbol = createIdsBySymbolMap(symbols);
    this.symbolsPerLine = buildSymbolsPerLine(symbols);
  }

  @Override
  public void read(DbFileSources.Line.Builder lineBuilder) {
    if (!areSymbolsValid) {
      return;
    }
    try {
      processSymbols(lineBuilder);
    } catch (RangeOffsetConverter.RangeOffsetConverterException e) {
      areSymbolsValid = false;
      LOG.warn(format("Inconsistency detected in Symbols data. Symbols will be ignored for file '%s'", file.getKey()), e);
    }
  }

  private void processSymbols(DbFileSources.Line.Builder lineBuilder) {
    int line = lineBuilder.getLine();

    List<BatchReport.Symbol> lineSymbols = new ArrayList<>(this.symbolsPerLine.get(line));
    // Sort symbols to have deterministic results and avoid false variation that would lead to an unnecessary update of the source files
    // data
    Collections.sort(lineSymbols, SymbolsComparator.INSTANCE);

    StringBuilder symbolString = new StringBuilder();
    for (BatchReport.Symbol lineSymbol : lineSymbols) {
      int symbolId = idsBySymbol.get(lineSymbol);

      appendSymbol(symbolString, lineSymbol.getDeclaration(), line, symbolId, lineBuilder.getSource());
      for (BatchReport.TextRange range : lineSymbol.getReferenceList()) {
        appendSymbol(symbolString, range, line, symbolId, lineBuilder.getSource());
      }
    }
    if (symbolString.length() > 0) {
      lineBuilder.setSymbols(symbolString.toString());
    }
  }

  private void appendSymbol(StringBuilder lineSymbol, BatchReport.TextRange range, int line, int symbolId, String sourceLine) {
    if (matchLine(range, line)) {
      String offsets = rangeOffsetConverter.offsetToString(range, line, sourceLine.length());
      if (!offsets.isEmpty()) {
        if (lineSymbol.length() > 0) {
          lineSymbol.append(SYMBOLS_SEPARATOR);
        }
        lineSymbol.append(offsets)
          .append(OFFSET_SEPARATOR)
          .append(symbolId);
      }
    }
  }

  private static boolean matchLine(BatchReport.TextRange range, int line) {
    return range.getStartLine() <= line && range.getEndLine() >= line;
  }

  private static Map<BatchReport.Symbol, Integer> createIdsBySymbolMap(List<BatchReport.Symbol> symbols) {
    Map<BatchReport.Symbol, Integer> map = new HashMap<>(symbols.size());
    int symbolId = 1;
    for (BatchReport.Symbol symbol : symbols) {
      map.put(symbol, symbolId);
      symbolId++;
    }
    return map;
  }

  private static SetMultimap<Integer, BatchReport.Symbol> buildSymbolsPerLine(List<BatchReport.Symbol> symbols) {
    SetMultimap<Integer, BatchReport.Symbol> res = HashMultimap.create();
    for (BatchReport.Symbol symbol : symbols) {
      putForTextRange(res, symbol, symbol.getDeclaration());
      for (BatchReport.TextRange textRange : symbol.getReferenceList()) {
        putForTextRange(res, symbol, textRange);
      }
    }
    return res;
  }

  private static void putForTextRange(SetMultimap<Integer, BatchReport.Symbol> res, BatchReport.Symbol symbol, BatchReport.TextRange declaration) {
    for (int i = declaration.getStartLine(); i <= declaration.getEndLine(); i++) {
      res.put(i, symbol);
    }
  }

  private enum SymbolsComparator implements Comparator<BatchReport.Symbol> {
    INSTANCE;

    @Override
    public int compare(BatchReport.Symbol o1, BatchReport.Symbol o2) {
      if (o1.getDeclaration().getStartLine() == o2.getDeclaration().getStartLine()) {
        return Integer.compare(o1.getDeclaration().getStartOffset(), o2.getDeclaration().getStartOffset());
      } else {
        return Integer.compare(o1.getDeclaration().getStartLine(), o2.getDeclaration().getStartLine());
      }
    }
  }
}
