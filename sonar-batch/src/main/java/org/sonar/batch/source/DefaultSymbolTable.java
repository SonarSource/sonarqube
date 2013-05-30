/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DefaultSymbolTable implements Symbolizable.SymbolTable {

  private Multimap<Symbol, Integer> referencesBySymbol;

  private DefaultSymbolTable(Multimap<Symbol, Integer> referencesBySymbol) {
    this.referencesBySymbol = referencesBySymbol;
  }

  public static Builder builder() {
    return new Builder();
  }

  public Multimap<Symbol, Integer> getReferencesBySymbol() {
    return referencesBySymbol;
  }

  @Override
  public List<Symbol> symbols() {
    return new ArrayList<Symbol>(referencesBySymbol.keySet());
  }

  @Override
  public List<Integer> references(Symbol symbol) {
    return new ArrayList<Integer>(referencesBySymbol.get(symbol));
  }

  public static class Builder implements Symbolizable.SymbolTableBuilder {

    private final Multimap<Symbol, Integer> referencesBySymbol;

    public Builder() {
      referencesBySymbol = TreeMultimap.create(new SymbolComparator(), new ReferenceComparator());
    }

    @Override
    public Symbol newSymbol(int fromOffset, int toOffset) {
      Symbol symbol = new DefaultSymbol(fromOffset, toOffset, null);
      referencesBySymbol.put(symbol, symbol.getDeclarationStartOffset());
      return symbol;
    }

    @Override
    public void newReference(Symbol symbol, int fromOffset) {
      if (fromOffset >= symbol.getDeclarationStartOffset() && fromOffset < symbol.getDeclarationEndOffset()) {
        throw new UnsupportedOperationException("Cannot add reference overlapping the symbol declaration");
      }
      referencesBySymbol.put(symbol, fromOffset);
    }

    @Override
    public Symbolizable.SymbolTable build() {
      return new DefaultSymbolTable(referencesBySymbol);
    }

    private static class SymbolComparator implements Comparator<Symbol>, Serializable {
      @Override
      public int compare(Symbol left, Symbol right) {
        return left.getDeclarationStartOffset() - right.getDeclarationStartOffset();
      }
    }

    private static class ReferenceComparator implements Comparator<Integer>, Serializable {
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
}
