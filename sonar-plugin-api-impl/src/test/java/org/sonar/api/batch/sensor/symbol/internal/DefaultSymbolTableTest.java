/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.api.batch.sensor.symbol.internal;

import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.sensor.symbol.NewSymbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

public class DefaultSymbolTableTest {

  private static final InputFile INPUT_FILE = new TestInputFileBuilder("foo", "src/Foo.java")
    .setLines(2)
    .setOriginalLineStartOffsets(new int[] {0, 50})
    .setOriginalLineEndOffsets(new int[] {49, 100})
    .setLastValidOffset(101)
    .build();

  private Map<TextRange, Set<TextRange>> referencesPerSymbol;


  @Before
  public void setUpSampleSymbols() {

    DefaultSymbolTable symbolTableBuilder = new DefaultSymbolTable(mock(SensorStorage.class))
      .onFile(INPUT_FILE);
    symbolTableBuilder
      .newSymbol(1, 0, 1, 10)
      .newReference(2, 10, 2, 15)
      .newReference(1, 16, 1, 20);

    symbolTableBuilder
      .newSymbol(1, 12, 1, 15)
      .newReference(2, 1, 2, 5);

    symbolTableBuilder.save();

    referencesPerSymbol = symbolTableBuilder.getReferencesBySymbol();
  }

  @Test
  public void fail_on_reference_overlaps_declaration() {
    NewSymbol symbol = new DefaultSymbolTable(mock(SensorStorage.class))
      .onFile(INPUT_FILE)
      .newSymbol(1, 0, 1, 10);

    assertThatThrownBy(() -> symbol.newReference(1, 3, 1, 12))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(
        "Overlapping symbol declaration and reference for symbol declared at Range[from [line=1, lineOffset=0] to [line=1, lineOffset=10]] and referenced at Range[from [line=1, lineOffset=3] to [line=1, lineOffset=12]] in file src/Foo.java");
  }

  @Test
  public void should_register_symbols() {
    assertThat(referencesPerSymbol).hasSize(2);
  }

}
