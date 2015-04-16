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

import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.source.db.FileSourceDb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;

public class SymbolsLineReader implements LineReader {

  private final List<BatchReport.Symbols.Symbol> symbols;
  private final Map<BatchReport.Symbols.Symbol, Integer> idsBySymbol;

  public SymbolsLineReader(List<BatchReport.Symbols.Symbol> symbols) {
    this.symbols = newArrayList(symbols);
    // Sort symbols to have deterministic results and avoid false variation that would lead to an unnecessary update of the source files
    // data
    Collections.sort(this.symbols, new SymbolsComparator());

    this.idsBySymbol = createIdsBySymbolMap(this.symbols);
  }

  @Override
  public void read(FileSourceDb.Line.Builder lineBuilder) {
    int line = lineBuilder.getLine();
    List<BatchReport.Symbols.Symbol> lineSymbols = findSymbolsMatchingLine(line);
    for (BatchReport.Symbols.Symbol lineSymbol : lineSymbols) {
      int symbolId = idsBySymbol.get(lineSymbol);
      StringBuilder symbolString = new StringBuilder(lineBuilder.getSymbols());

      appendSymbol(symbolString, lineSymbol.getDeclaration(), line, symbolId, lineBuilder.getSource());
      for (BatchReport.Range range : lineSymbol.getReferenceList()) {
        appendSymbol(symbolString, range, line, symbolId, lineBuilder.getSource());
      }

      if (symbolString.length() > 0) {
        lineBuilder.setSymbols(symbolString.toString());
      }
    }
  }

  private void appendSymbol(StringBuilder lineSymbol, BatchReport.Range range, int line, int symbolId, String sourceLine) {
    if (matchLine(range, line)) {
      String offsets = RangeOffsetHelper.offsetToString(range, line, sourceLine.length());
      if (!offsets.isEmpty()) {
        if (lineSymbol.length() > 0) {
          lineSymbol.append(RangeOffsetHelper.SYMBOLS_SEPARATOR);
        }
        lineSymbol.append(offsets)
          .append(RangeOffsetHelper.OFFSET_SEPARATOR)
          .append(symbolId);
      }
    }
  }

  private List<BatchReport.Symbols.Symbol> findSymbolsMatchingLine(int line) {
    List<BatchReport.Symbols.Symbol> lineSymbols = new ArrayList<>();
    Set<BatchReport.Symbols.Symbol> symbolsIndex = new HashSet<>();
    for (BatchReport.Symbols.Symbol symbol : symbols) {
      if (matchLine(symbol.getDeclaration(), line) && !symbolsIndex.contains(symbol)) {
        lineSymbols.add(symbol);
        symbolsIndex.add(symbol);
      } else {
        for (BatchReport.Range range : symbol.getReferenceList()) {
          if (matchLine(range, line) && !symbolsIndex.contains(symbol)) {
            lineSymbols.add(symbol);
            symbolsIndex.add(symbol);
          }
        }
      }
    }
    return lineSymbols;
  }

  private static boolean matchLine(BatchReport.Range range, int line) {
    return range.getStartLine() <= line && range.getEndLine() >= line;
  }

  private Map<BatchReport.Symbols.Symbol, Integer> createIdsBySymbolMap(List<BatchReport.Symbols.Symbol> symbols) {
    Map<BatchReport.Symbols.Symbol, Integer> map = new HashMap<>();
    int symbolId = 1;
    for (BatchReport.Symbols.Symbol symbol : symbols) {
      map.put(symbol, symbolId);
      symbolId++;
    }
    return map;
  }

  private static class SymbolsComparator implements Comparator<BatchReport.Symbols.Symbol>, Serializable {
    @Override
    public int compare(BatchReport.Symbols.Symbol o1, BatchReport.Symbols.Symbol o2) {
      if (o1.getDeclaration().getStartLine() == o2.getDeclaration().getStartLine()) {
        return Integer.compare(o1.getDeclaration().getStartOffset(), o2.getDeclaration().getStartOffset());
      } else {
        return Integer.compare(o1.getDeclaration().getStartLine(), o2.getDeclaration().getStartLine());
      }
    }
  }
}
