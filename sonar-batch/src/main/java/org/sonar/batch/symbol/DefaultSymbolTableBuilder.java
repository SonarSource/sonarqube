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

package org.sonar.batch.symbol;

import org.sonar.api.batch.sensor.symbol.Symbol;
import org.sonar.api.batch.sensor.symbol.SymbolTableBuilder;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbol;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.core.source.SnapshotDataTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultSymbolTableBuilder implements SymbolTableBuilder {

  private final String componentKey;
  private final ComponentDataCache cache;
  private final Map<org.sonar.api.source.Symbol, List<Integer>> referencesBySymbol = new LinkedHashMap<org.sonar.api.source.Symbol, List<Integer>>();

  public DefaultSymbolTableBuilder(String componentKey, ComponentDataCache cache) {
    this.componentKey = componentKey;
    this.cache = cache;
  }

  @Override
  public Symbol newSymbol(int fromOffset, int toOffset) {
    org.sonar.api.source.Symbol symbol = new DefaultSymbol(fromOffset, toOffset);
    referencesBySymbol.put(symbol, new ArrayList<Integer>());
    return symbol;
  }

  @Override
  public void newReference(Symbol symbol, int fromOffset) {
    if (!referencesBySymbol.containsKey(symbol)) {
      throw new UnsupportedOperationException("Cannot add reference to a symbol in another file");
    }
    if (fromOffset >= symbol.getDeclarationStartOffset() && fromOffset < symbol.getDeclarationEndOffset()) {
      throw new UnsupportedOperationException("Cannot add reference (" + fromOffset + ") overlapping " + symbol);
    }
    referencesBySymbol.get(symbol).add(fromOffset);
  }

  public SymbolData build() {
    return new SymbolData(referencesBySymbol);
  }

  @Override
  public void done() {
    cache.setData(componentKey, SnapshotDataTypes.SYMBOL_HIGHLIGHTING, build());
  }

  public static class SymbolComparator implements Comparator<Symbol>, Serializable {
    @Override
    public int compare(Symbol left, Symbol right) {
      return left.getDeclarationStartOffset() - right.getDeclarationStartOffset();
    }
  }

  public static class ReferenceComparator implements Comparator<Integer>, Serializable {
    @Override
    public int compare(Integer left, Integer right) {
      int result;
      if (left != null & right != null) {
        result = left - right;
      } else {
        result = left == null ? -1 : 1;
      }
      return result;
    }
  }
}
