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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.protobuf.DbFileSources;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.component.Component;

import static java.lang.String.format;
import static org.sonar.server.computation.task.projectanalysis.source.RangeOffsetConverter.OFFSET_SEPARATOR;
import static org.sonar.server.computation.task.projectanalysis.source.RangeOffsetConverter.SYMBOLS_SEPARATOR;

public class SymbolsLineReader implements LineReader {

  private static final Logger LOG = Loggers.get(HighlightingLineReader.class);

  private final Component file;
  private final RangeOffsetConverter rangeOffsetConverter;
  private final Map<ScannerReport.Symbol, Integer> idsBySymbol;
  private final SetMultimap<Integer, ScannerReport.Symbol> symbolsPerLine;

  private boolean areSymbolsValid = true;

  public SymbolsLineReader(Component file, Iterator<ScannerReport.Symbol> symbolIterator, RangeOffsetConverter rangeOffsetConverter) {
    this.file = file;
    this.rangeOffsetConverter = rangeOffsetConverter;
    List<ScannerReport.Symbol> symbols = Lists.newArrayList(symbolIterator);
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

    List<ScannerReport.Symbol> lineSymbols = new ArrayList<>(this.symbolsPerLine.get(line));
    // Sort symbols to have deterministic results and avoid false variation that would lead to an unnecessary update of the source files
    // data
    Collections.sort(lineSymbols, SymbolsComparator.INSTANCE);

    StringBuilder symbolString = new StringBuilder();
    for (ScannerReport.Symbol lineSymbol : lineSymbols) {
      int symbolId = idsBySymbol.get(lineSymbol);

      appendSymbol(symbolString, lineSymbol.getDeclaration(), line, symbolId, lineBuilder.getSource());
      for (ScannerReport.TextRange range : lineSymbol.getReferenceList()) {
        appendSymbol(symbolString, range, line, symbolId, lineBuilder.getSource());
      }
    }
    if (symbolString.length() > 0) {
      lineBuilder.setSymbols(symbolString.toString());
    }
  }

  private void appendSymbol(StringBuilder lineSymbol, ScannerReport.TextRange range, int line, int symbolId, String sourceLine) {
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

  private static boolean matchLine(ScannerReport.TextRange range, int line) {
    return range.getStartLine() <= line && range.getEndLine() >= line;
  }

  private static Map<ScannerReport.Symbol, Integer> createIdsBySymbolMap(List<ScannerReport.Symbol> symbols) {
    Map<ScannerReport.Symbol, Integer> map = new IdentityHashMap<>(symbols.size());
    int symbolId = 1;
    for (ScannerReport.Symbol symbol : symbols) {
      map.put(symbol, symbolId);
      symbolId++;
    }
    return map;
  }

  private static SetMultimap<Integer, ScannerReport.Symbol> buildSymbolsPerLine(List<ScannerReport.Symbol> symbols) {
    SetMultimap<Integer, ScannerReport.Symbol> res = HashMultimap.create();
    for (ScannerReport.Symbol symbol : symbols) {
      putForTextRange(res, symbol, symbol.getDeclaration());
      for (ScannerReport.TextRange textRange : symbol.getReferenceList()) {
        putForTextRange(res, symbol, textRange);
      }
    }
    return res;
  }

  private static void putForTextRange(SetMultimap<Integer, ScannerReport.Symbol> res, ScannerReport.Symbol symbol, ScannerReport.TextRange declaration) {
    for (int i = declaration.getStartLine(); i <= declaration.getEndLine(); i++) {
      res.put(i, symbol);
    }
  }

  private enum SymbolsComparator implements Comparator<ScannerReport.Symbol> {
    INSTANCE;

    @Override
    public int compare(ScannerReport.Symbol o1, ScannerReport.Symbol o2) {
      if (o1.getDeclaration().getStartLine() == o2.getDeclaration().getStartLine()) {
        return Integer.compare(o1.getDeclaration().getStartOffset(), o2.getDeclaration().getStartOffset());
      } else {
        return Integer.compare(o1.getDeclaration().getStartLine(), o2.getDeclaration().getStartLine());
      }
    }
  }
}
