/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.internal.SensorStorage;
import org.sonar.api.batch.fs.internal.TestInputFileBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DefaultSymbolTableTest {

  private static final InputFile INPUT_FILE = new TestInputFileBuilder("foo", "src/Foo.java")
    .setLines(2)
    .setOriginalLineStartOffsets(new int[] {0, 50})
    .setOriginalLineEndOffsets(new int[] {49, 100})
    .setLastValidOffset(101)
    .build();

  private Map<TextRange, Set<TextRange>> referencesPerSymbol;

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Before
  public void setUpSampleSymbols() {

    DefaultSymbolTable symbolTableBuilder = new DefaultSymbolTable(mock(SensorStorage.class))
      .onFile(INPUT_FILE);
    symbolTableBuilder
      .newSymbol(0, 10)
      .newReference(12, 15)
      .newReference(2, 10, 2, 15);

    symbolTableBuilder.newSymbol(1, 12, 1, 15).newReference(52, 55);

    symbolTableBuilder.save();

    referencesPerSymbol = symbolTableBuilder.getReferencesBySymbol();
  }

  @Test
  public void should_register_symbols() {
    assertThat(referencesPerSymbol).hasSize(2);
  }

}
