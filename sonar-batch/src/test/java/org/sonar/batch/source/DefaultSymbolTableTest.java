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

import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.FileMetadata;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultSymbolTableTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();
  private DefaultInputFile inputFile;

  @Before
  public void prepare() {
    inputFile = new DefaultInputFile("foo", "src/Foo.php")
      .initMetadata(new FileMetadata().readMetadata(new StringReader(Strings.repeat("azerty\n", 20))));
  }

  @Test
  public void should_order_symbol_and_references() throws Exception {

    Symbolizable.SymbolTableBuilder symbolTableBuilder = new DefaultSymbolTable.Builder(inputFile);
    Symbol firstSymbol = symbolTableBuilder.newSymbol(10, 20);
    symbolTableBuilder.newReference(firstSymbol, 32);
    Symbol secondSymbol = symbolTableBuilder.newSymbol(84, 92);
    symbolTableBuilder.newReference(secondSymbol, 124);
    Symbol thirdSymbol = symbolTableBuilder.newSymbol(55, 62);
    symbolTableBuilder.newReference(thirdSymbol, 70);
    Symbolizable.SymbolTable symbolTable = symbolTableBuilder.build();

    assertThat(symbolTable.symbols()).containsExactly(firstSymbol, secondSymbol, thirdSymbol);
  }

  @Test
  public void should_reject_reference_conflicting_with_declaration() throws Exception {
    throwable.expect(UnsupportedOperationException.class);

    Symbolizable.SymbolTableBuilder symbolTableBuilder = new DefaultSymbolTable.Builder(inputFile);
    Symbol symbol = symbolTableBuilder.newSymbol(10, 20);
    symbolTableBuilder.newReference(symbol, 15);
  }

  @Test
  public void test_toString() throws Exception {
    Symbolizable.SymbolTableBuilder symbolTableBuilder = new DefaultSymbolTable.Builder(inputFile);
    Symbol symbol = symbolTableBuilder.newSymbol(10, 20);

    assertThat(symbol.toString()).isEqualTo("Symbol{range=Range[from [line=2, lineOffset=3] to [line=3, lineOffset=6]]}");
  }
}
