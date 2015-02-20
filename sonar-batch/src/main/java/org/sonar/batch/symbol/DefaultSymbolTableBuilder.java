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

import org.sonar.api.source.Symbol;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.batch.source.DefaultSymbol;
import org.sonar.core.source.SnapshotDataTypes;

import java.io.Serializable;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DefaultSymbolTableBuilder {

  private final String componentKey;
  private final ComponentDataCache cache;
  private final Map<org.sonar.api.source.Symbol, Set<Integer>> referencesBySymbol = new LinkedHashMap<org.sonar.api.source.Symbol, Set<Integer>>();

  public DefaultSymbolTableBuilder(String componentKey, ComponentDataCache cache) {
    this.componentKey = componentKey;
    this.cache = cache;
  }

  public Symbol newSymbol(int fromOffset, int toOffset) {
    org.sonar.api.source.Symbol symbol = new DefaultSymbol(fromOffset, toOffset);
    referencesBySymbol.put(symbol, new TreeSet<Integer>());
    return symbol;
  }

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
      if (left != null && right != null) {
        result = left - right;
      } else {
        result = left == null ? -1 : 1;
      }
      return result;
    }
  }
}
