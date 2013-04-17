/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.batch.scan.source;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.scan.source.Symbol;

import static org.fest.assertions.Assertions.assertThat;

public class SymbolDataRepositoryTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Test
  public void should_serialize_symbols_in_natural_order() throws Exception {

    SymbolDataRepository dataRepository = new SymbolDataRepository();

    Symbol firstSymbol = DefaultSymbol.builder(dataRepository).setDeclaration(10, 20).build();
    Symbol secondSymbol = DefaultSymbol.builder(dataRepository).setDeclaration(84, 92).build();
    Symbol thirdSymbol = DefaultSymbol.builder(dataRepository).setDeclaration(55, 62).build();

    dataRepository.registerSymbolReference(firstSymbol, 32);
    dataRepository.registerSymbolReference(secondSymbol, 124);
    dataRepository.registerSymbolReference(thirdSymbol, 70);

    String serializedSymbolData = dataRepository.writeString();

    assertThat(serializedSymbolData).isEqualTo("10,20,10,32;55,62,55,70;84,92,84,124;");
  }

  @Test
  public void should_serialize_unused_symbol() throws Exception {

    SymbolDataRepository dataRepository = new SymbolDataRepository();

    Symbol unusedSymbol = DefaultSymbol.builder(dataRepository).setDeclaration(10, 20).build();
    dataRepository.registerSymbol(unusedSymbol);

    String serializedSymbolData = dataRepository.writeString();

    assertThat(serializedSymbolData).isEqualTo("10,20,10;");
  }

  @Test
  public void should_reject_reference_conflicting_with_declaration() throws Exception {

    throwable.expect(UnsupportedOperationException.class);

    SymbolDataRepository dataRepository = new SymbolDataRepository();

    Symbol symbol = DefaultSymbol.builder(dataRepository).setDeclaration(10, 20).build();
    dataRepository.registerSymbolReference(symbol, 15);
  }
}
