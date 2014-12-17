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

import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbol;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DefaultSymbolTable implements Symbolizable.SymbolTable {

  private Map<Symbol, Set<Integer>> referencesBySymbol;

  private DefaultSymbolTable(Map<Symbol, Set<Integer>> referencesBySymbol) {
    this.referencesBySymbol = referencesBySymbol;
  }

  public Map<Symbol, Set<Integer>> getReferencesBySymbol() {
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

    private final Map<Symbol, Set<Integer>> referencesBySymbol = new LinkedHashMap<Symbol, Set<Integer>>();
    private final String componentKey;

    public Builder(String componentKey) {
      this.componentKey = componentKey;
    }

    @Override
    public Symbol newSymbol(int fromOffset, int toOffset) {
      Symbol symbol = new DefaultSymbol(fromOffset, toOffset);
      referencesBySymbol.put(symbol, new TreeSet<Integer>());
      return symbol;
    }

    @Override
    public void newReference(Symbol symbol, int fromOffset) {
      if (!referencesBySymbol.containsKey(symbol)) {
        throw new UnsupportedOperationException("Cannot add reference to a symbol in another file");
      }
      if (fromOffset >= symbol.getDeclarationStartOffset() && fromOffset < symbol.getDeclarationEndOffset()) {
        throw new UnsupportedOperationException("Cannot add reference (" + fromOffset + ") overlapping " + symbol + " in " + componentKey);
      }
      referencesBySymbol.get(symbol).add(fromOffset);
    }

    @Override
    public Symbolizable.SymbolTable build() {
      return new DefaultSymbolTable(referencesBySymbol);
    }

  }
}
