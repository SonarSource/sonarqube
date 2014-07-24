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
import org.sonar.api.batch.sensor.symbol.Symbol;
import org.sonar.batch.index.Data;

import java.util.Collection;

public class SymbolData implements Data {

  private static final String FIELD_SEPARATOR = ",";
  private static final String SYMBOL_SEPARATOR = ";";

  private final SortedSetMultimap<Symbol, Integer> referencesBySymbol;

  public SymbolData(SortedSetMultimap<Symbol, Integer> referencesBySymbol) {
    this.referencesBySymbol = referencesBySymbol;
  }

  public SortedSetMultimap<Symbol, Integer> referencesBySymbol() {
    return referencesBySymbol;
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

}
