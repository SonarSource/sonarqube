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

import org.junit.Test;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;

import static org.fest.assertions.Assertions.assertThat;

public class SymbolDataTest {

  @Test
  public void should_serialize_symbols_in_natural_order() throws Exception {

    Symbolizable.SymbolTableBuilder symbolTableBuilder = new DefaultSymbolTable.Builder();
    Symbol firstSymbol = symbolTableBuilder.newSymbol(10, 20);
    symbolTableBuilder.newReference(firstSymbol, 32);
    Symbol secondSymbol = symbolTableBuilder.newSymbol(84, 92);
    symbolTableBuilder.newReference(secondSymbol, 124);
    Symbol thirdSymbol = symbolTableBuilder.newSymbol(55, 62);
    symbolTableBuilder.newReference(thirdSymbol, 70);
    Symbolizable.SymbolTable symbolTable = symbolTableBuilder.build();

    SymbolData dataRepository = new SymbolData(symbolTable);
    String serializedSymbolData = dataRepository.writeString();

    assertThat(serializedSymbolData).isEqualTo("10,20,10,32;55,62,55,70;84,92,84,124;");
  }

  @Test
  public void should_serialize_unused_symbol() throws Exception {

    Symbolizable.SymbolTableBuilder symbolTableBuilder = new DefaultSymbolTable.Builder();
    symbolTableBuilder.newSymbol(10, 20);

    SymbolData dataRepository = new SymbolData(symbolTableBuilder.build());
    String serializedSymbolData = dataRepository.writeString();

    assertThat(serializedSymbolData).isEqualTo("10,20,10;");
  }
}
