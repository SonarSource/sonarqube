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

import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;

import java.util.*;

public class DefaultSymbolTable implements Symbolizable.SymbolTable {

  private Map<Symbol, Set<TextRange>> referencesBySymbol;

  private DefaultSymbolTable(Map<Symbol, Set<TextRange>> referencesBySymbol) {
    this.referencesBySymbol = referencesBySymbol;
  }

  public Map<Symbol, Set<TextRange>> getReferencesBySymbol() {
    return referencesBySymbol;
  }

  @Override
  public List<Symbol> symbols() {
    List<Symbol> result = new ArrayList<>();
    for (Symbol symbol : referencesBySymbol.keySet()) {
      result.add((Symbol) symbol);
    }
    return result;
  }

  @Override
  public List<Integer> references(Symbol symbol) {
    throw new UnsupportedOperationException("references");
  }

  public static class Builder implements Symbolizable.SymbolTableBuilder {

    private final Map<Symbol, Set<TextRange>> referencesBySymbol = new LinkedHashMap<>();
    private final DefaultInputFile inputFile;

    public Builder(DefaultInputFile inputFile) {
      this.inputFile = inputFile;
    }

    @Override
    public Symbol newSymbol(int fromOffset, int toOffset) {
      TextRange declarationRange = inputFile.newRange(fromOffset, toOffset);
      DefaultSymbol symbol = new DefaultSymbol(declarationRange, toOffset - fromOffset);
      referencesBySymbol.put(symbol, new TreeSet<>(new Comparator<TextRange>() {
        @Override
        public int compare(TextRange o1, TextRange o2) {
          return o1.start().compareTo(o2.start());
        }
      }));
      return symbol;
    }

    @Override
    public void newReference(Symbol symbol, int fromOffset) {
      if (!referencesBySymbol.containsKey(symbol)) {
        throw new UnsupportedOperationException("Cannot add reference to a symbol in another file");
      }
      TextRange referenceRange = inputFile.newRange(fromOffset, fromOffset + ((DefaultSymbol) symbol).getLength());

      if (referenceRange.overlap(((DefaultSymbol) symbol).range())) {
        throw new UnsupportedOperationException("Cannot add reference (" + fromOffset + ") overlapping " + symbol + " in " + inputFile.key());
      }
      referencesBySymbol.get(symbol).add(referenceRange);
    }

    @Override
    public Symbolizable.SymbolTable build() {
      return new DefaultSymbolTable(referencesBySymbol);
    }

  }
}
