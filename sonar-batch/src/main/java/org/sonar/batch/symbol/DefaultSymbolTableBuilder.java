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

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.sonar.api.batch.sensor.symbol.Symbol;
import org.sonar.api.batch.sensor.symbol.SymbolTableBuilder;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbol;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.core.source.SnapshotDataTypes;

import java.io.Serializable;
import java.util.Comparator;

public class DefaultSymbolTableBuilder implements SymbolTableBuilder {

  private final String componentKey;
  private final ComponentDataCache cache;
  private final SortedSetMultimap<Symbol, Integer> referencesBySymbol;

  public DefaultSymbolTableBuilder(String componentKey, ComponentDataCache cache) {
    this.componentKey = componentKey;
    this.cache = cache;
    this.referencesBySymbol = TreeMultimap.create(new SymbolComparator(), new ReferenceComparator());
  }

  @Override
  public Symbol newSymbol(int fromOffset, int toOffset) {
    Symbol symbol = new DefaultSymbol(componentKey, fromOffset, toOffset);
    referencesBySymbol.put(symbol, symbol.getDeclarationStartOffset());
    return symbol;
  }

  @Override
  public void newReference(Symbol symbol, int fromOffset) {
    String otherComponentKey = ((DefaultSymbol) symbol).componentKey();
    if (!otherComponentKey.equals(componentKey)) {
      throw new UnsupportedOperationException("Cannot add reference from (" + componentKey + ") to another file (" + otherComponentKey + ")");
    }
    if (fromOffset >= symbol.getDeclarationStartOffset() && fromOffset < symbol.getDeclarationEndOffset()) {
      throw new UnsupportedOperationException("Cannot add reference (" + fromOffset + ") overlapping " + symbol);
    }
    referencesBySymbol.put(symbol, fromOffset);
  }

  @Override
  public void done() {
    SymbolData symbolData = new SymbolData(referencesBySymbol);
    cache.setData(componentKey, SnapshotDataTypes.SYMBOL_HIGHLIGHTING, symbolData);
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
