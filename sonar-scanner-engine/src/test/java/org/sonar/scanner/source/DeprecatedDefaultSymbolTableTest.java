/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner.source;

import com.google.common.base.Strings;
import java.util.Set;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;

import static org.assertj.core.api.Assertions.assertThat;

public class DeprecatedDefaultSymbolTableTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();
  private DefaultInputFile inputFile;

  @Before
  public void prepare() {
    inputFile = new TestInputFileBuilder("foo", "src/Foo.php")
      .initMetadata(Strings.repeat("azerty\n", 20))
      .build();
  }

  @Test
  public void should_order_symbol_and_references() {

    Symbolizable.SymbolTableBuilder symbolTableBuilder = new DeprecatedDefaultSymbolTable.Builder(new DefaultSymbolTable(null).onFile(inputFile));
    Symbol firstSymbol = symbolTableBuilder.newSymbol(10, 20);
    symbolTableBuilder.newReference(firstSymbol, 32);
    Symbol secondSymbol = symbolTableBuilder.newSymbol(84, 92);
    symbolTableBuilder.newReference(secondSymbol, 124);
    Symbol thirdSymbol = symbolTableBuilder.newSymbol(55, 62);
    symbolTableBuilder.newReference(thirdSymbol, 70);

    DeprecatedDefaultSymbolTable symbolTable = (DeprecatedDefaultSymbolTable) symbolTableBuilder.build();

    assertThat(symbolTable.getWrapped().getReferencesBySymbol().keySet()).containsExactly(range(10, 20), range(84, 92), range(55, 62));
  }

  @Test
  public void variable_length_references() {
    Symbolizable.SymbolTableBuilder symbolTableBuilder = new DeprecatedDefaultSymbolTable.Builder(new DefaultSymbolTable(null).onFile(inputFile));
    Symbol firstSymbol = symbolTableBuilder.newSymbol(10, 20);
    symbolTableBuilder.newReference(firstSymbol, 32);
    symbolTableBuilder.newReference(firstSymbol, 44, 47);

    DeprecatedDefaultSymbolTable symbolTable = (DeprecatedDefaultSymbolTable) symbolTableBuilder.build();

    assertThat(symbolTable.getWrapped().getReferencesBySymbol().keySet()).containsExactly(range(10, 20));

    Set<TextRange> references = symbolTable.getWrapped().getReferencesBySymbol().get(range(10, 20));
    assertThat(references).containsExactly(range(32, 42), range(44, 47));
  }

  private TextRange range(int start, int end) {
    return inputFile.newRange(start, end);
  }

  @Test
  public void should_reject_reference_conflicting_with_declaration() {
    throwable.expect(IllegalArgumentException.class);

    Symbolizable.SymbolTableBuilder symbolTableBuilder = new DeprecatedDefaultSymbolTable.Builder(new DefaultSymbolTable(null).onFile(inputFile));
    Symbol symbol = symbolTableBuilder.newSymbol(10, 20);
    symbolTableBuilder.newReference(symbol, 15);
  }

}
