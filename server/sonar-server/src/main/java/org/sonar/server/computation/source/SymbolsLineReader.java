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

import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.db.protobuf.DbFileSources;

public class SymbolsLineReader implements LineReader {

  private final List<BatchReport.Symbol> symbols;
  private final Map<BatchReport.Symbol, Integer> idsBySymbol;

  public SymbolsLineReader(Iterator<BatchReport.Symbol> symbols) {
    this.symbols = Lists.newArrayList(symbols);
    // Sort symbols to have deterministic results and avoid false variation that would lead to an unnecessary update of the source files
    // data
    Collections.sort(this.symbols, new SymbolsComparator());

    this.idsBySymbol = createIdsBySymbolMap(this.symbols);
  }

  @Override
  public void read(DbFileSources.Line.Builder lineBuilder) {
    int line = lineBuilder.getLine();
    List<BatchReport.Symbol> lineSymbols = findSymbolsMatchingLine(line);
    for (BatchReport.Symbol lineSymbol : lineSymbols) {
      int symbolId = idsBySymbol.get(lineSymbol);
      StringBuilder symbolString = new StringBuilder(lineBuilder.getSymbols());

      appendSymbol(symbolString, lineSymbol.getDeclaration(), line, symbolId, lineBuilder.getSource());
      for (BatchReport.TextRange range : lineSymbol.getReferenceList()) {
        appendSymbol(symbolString, range, line, symbolId, lineBuilder.getSource());
      }

      if (symbolString.length() > 0) {
        lineBuilder.setSymbols(symbolString.toString());
      }
    }
  }

  private static void appendSymbol(StringBuilder lineSymbol, BatchReport.TextRange range, int line, int symbolId, String sourceLine) {
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

  private List<BatchReport.Symbol> findSymbolsMatchingLine(int line) {
    List<BatchReport.Symbol> lineSymbols = new ArrayList<>();
    Set<BatchReport.Symbol> symbolsIndex = new HashSet<>();
    for (BatchReport.Symbol symbol : symbols) {
      if (matchLine(symbol.getDeclaration(), line) && !symbolsIndex.contains(symbol)) {
        lineSymbols.add(symbol);
        symbolsIndex.add(symbol);
      } else {
        for (BatchReport.TextRange range : symbol.getReferenceList()) {
          if (matchLine(range, line) && !symbolsIndex.contains(symbol)) {
            lineSymbols.add(symbol);
            symbolsIndex.add(symbol);
          }
        }
      }
    }
    return lineSymbols;
  }

  private static boolean matchLine(BatchReport.TextRange range, int line) {
    return range.getStartLine() <= line && range.getEndLine() >= line;
  }

  private static Map<BatchReport.Symbol, Integer> createIdsBySymbolMap(List<BatchReport.Symbol> symbols) {
    Map<BatchReport.Symbol, Integer> map = new HashMap<>();
    int symbolId = 1;
    for (BatchReport.Symbol symbol : symbols) {
      map.put(symbol, symbolId);
      symbolId++;
    }
    return map;
  }

  private static class SymbolsComparator implements Comparator<BatchReport.Symbol>, Serializable {
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
