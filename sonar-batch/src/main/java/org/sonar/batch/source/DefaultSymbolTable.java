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

package org.sonar.batch.source;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbol;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;
import org.sonar.batch.symbol.DefaultSymbolTableBuilder;

import java.util.ArrayList;
import java.util.List;

public class DefaultSymbolTable implements Symbolizable.SymbolTable {

  private SortedSetMultimap<org.sonar.api.batch.sensor.symbol.Symbol, Integer> referencesBySymbol;

  private DefaultSymbolTable(SortedSetMultimap<org.sonar.api.batch.sensor.symbol.Symbol, Integer> referencesBySymbol) {
    this.referencesBySymbol = referencesBySymbol;
  }

  public SortedSetMultimap<org.sonar.api.batch.sensor.symbol.Symbol, Integer> getReferencesBySymbol() {
    return referencesBySymbol;
  }

  @Override
  public List<Symbol> symbols() {
    List<Symbol> result = new ArrayList<Symbol>();
    for (org.sonar.api.batch.sensor.symbol.Symbol symbol : referencesBySymbol.keySet()) {
      result.add((Symbol) symbol);
    }
    return result;
  }

  @Override
  public List<Integer> references(Symbol symbol) {
    return new ArrayList<Integer>(referencesBySymbol.get(symbol));
  }

  public static class Builder implements Symbolizable.SymbolTableBuilder {

    private final SortedSetMultimap<org.sonar.api.batch.sensor.symbol.Symbol, Integer> referencesBySymbol;
    private final String componentKey;

    public Builder(String componentKey) {
      this.componentKey = componentKey;
      referencesBySymbol = TreeMultimap.create(new DefaultSymbolTableBuilder.SymbolComparator(), new DefaultSymbolTableBuilder.ReferenceComparator());
    }

    @Override
    public Symbol newSymbol(int fromOffset, int toOffset) {
      Symbol symbol = new DefaultSymbol(componentKey, fromOffset, toOffset);
      referencesBySymbol.put(symbol, symbol.getDeclarationStartOffset());
      return symbol;
    }

    @Override
    public void newReference(Symbol symbol, int fromOffset) {
      if (fromOffset >= symbol.getDeclarationStartOffset() && fromOffset < symbol.getDeclarationEndOffset()) {
        throw new UnsupportedOperationException("Cannot add reference (" + fromOffset + ") overlapping " + symbol);
      }
      referencesBySymbol.put(symbol, fromOffset);
    }

    @Override
    public Symbolizable.SymbolTable build() {
      return new DefaultSymbolTable(referencesBySymbol);
    }

  }
}
