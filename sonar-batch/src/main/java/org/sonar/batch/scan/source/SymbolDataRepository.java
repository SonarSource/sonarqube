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

package org.sonar.batch.scan.source;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import org.sonar.api.scan.source.Symbol;
import org.sonar.batch.index.Data;

import java.util.Collection;
import java.util.Comparator;

public class SymbolDataRepository implements Data {

  private static final String FIELD_SEPARATOR = ",";
  private static final String SYMBOL_SEPARATOR = ";";

  private Multimap<Symbol, Integer> referencesBySymbol;

  public SymbolDataRepository() {
    referencesBySymbol = TreeMultimap.create(new SymbolComparator(), new ReferenceComparator());
  }

  public void registerSymbol(Symbol symbol) {
    referencesBySymbol.put(symbol, symbol.getDeclarationStartOffset());
  }

  public void registerSymbolReference(Symbol symbol, int startOffset) {
    if (startOffset >= symbol.getDeclarationStartOffset() && startOffset < symbol.getDeclarationEndOffset()) {
      throw new UnsupportedOperationException("Cannot add reference overlapping the symbol declaration");
    }
    referencesBySymbol.put(symbol, startOffset);
  }

  @Override
  public String writeString() {
    StringBuilder sb = new StringBuilder();

    for (Symbol symbol : referencesBySymbol.keySet()) {

      sb.append(symbol.getDeclarationStartOffset())
        .append(FIELD_SEPARATOR)
        .append(symbol.getDeclarationEndOffset());
      Collection<Integer> symbolReferences = referencesBySymbol.get(symbol);
      for (Integer symbolReference : symbolReferences) {
        sb.append(FIELD_SEPARATOR).append(symbolReference);
      }
      sb.append(SYMBOL_SEPARATOR);
    }

    return sb.toString();
  }

  @Override
  public void readString(String s) {
    throw new UnsupportedOperationException();
  }


  private class SymbolComparator implements Comparator<Symbol> {
    @Override
    public int compare(Symbol left, Symbol right) {
      return left.getDeclarationStartOffset() - right.getDeclarationStartOffset();
    }
  }

  private class ReferenceComparator implements Comparator<Integer> {
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
