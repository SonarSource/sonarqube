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
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;
import org.sonar.scanner.sensor.DefaultSensorStorage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultSymbolizableTest {

  @Test
  public void should_update_cache_when_done() {

    DefaultSensorStorage sensorStorage = mock(DefaultSensorStorage.class);
    DefaultInputFile inputFile = new TestInputFileBuilder("foo", "src/Foo.php")
      .initMetadata(Strings.repeat("azerty\n", 20)).build();

    DefaultSymbolizable symbolPerspective = new DefaultSymbolizable(inputFile, sensorStorage, mock(AnalysisMode.class));
    Symbolizable.SymbolTableBuilder symbolTableBuilder = symbolPerspective.newSymbolTableBuilder();
    Symbol firstSymbol = symbolTableBuilder.newSymbol(4, 8);
    symbolTableBuilder.newReference(firstSymbol, 12);
    symbolTableBuilder.newReference(firstSymbol, 70);
    Symbol otherSymbol = symbolTableBuilder.newSymbol(25, 33);
    symbolTableBuilder.newReference(otherSymbol, 44);
    symbolTableBuilder.newReference(otherSymbol, 60);
    symbolTableBuilder.newReference(otherSymbol, 108);
    Symbolizable.SymbolTable symbolTable = symbolTableBuilder.build();

    symbolPerspective.setSymbolTable(symbolTable);

    ArgumentCaptor<DefaultSymbolTable> argCaptor = ArgumentCaptor.forClass(DefaultSymbolTable.class);
    verify(sensorStorage).store(argCaptor.capture());
    // Map<Symbol, Set<TextRange>>
    assertThat(argCaptor.getValue().getReferencesBySymbol().keySet()).hasSize(2);
  }
}
