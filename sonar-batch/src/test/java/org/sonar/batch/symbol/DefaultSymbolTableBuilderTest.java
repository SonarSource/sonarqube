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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.sonar.api.batch.sensor.symbol.Symbol;
import org.sonar.api.batch.sensor.symbol.SymbolTableBuilder;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.core.source.SnapshotDataTypes;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultSymbolTableBuilderTest {

  @Rule
  public ExpectedException throwable = ExpectedException.none();

  @Test
  public void should_write_symbol_and_references() throws Exception {
    ComponentDataCache componentDataCache = mock(ComponentDataCache.class);
    SymbolTableBuilder symbolTableBuilder = new DefaultSymbolTableBuilder("foo", componentDataCache);
    Symbol firstSymbol = symbolTableBuilder.newSymbol(10, 20);
    symbolTableBuilder.newReference(firstSymbol, 32);
    Symbol secondSymbol = symbolTableBuilder.newSymbol(84, 92);
    symbolTableBuilder.newReference(secondSymbol, 124);
    Symbol thirdSymbol = symbolTableBuilder.newSymbol(55, 62);
    symbolTableBuilder.newReference(thirdSymbol, 70);
    symbolTableBuilder.done();

    ArgumentCaptor<SymbolData> argCaptor = ArgumentCaptor.forClass(SymbolData.class);
    verify(componentDataCache).setData(eq("foo"), eq(SnapshotDataTypes.SYMBOL_HIGHLIGHTING), argCaptor.capture());

    Map<org.sonar.api.source.Symbol, Set<Integer>> referencesBySymbol = argCaptor.getValue().referencesBySymbol();

    assertThat(new ArrayList<Symbol>(referencesBySymbol.keySet())).containsExactly(firstSymbol, secondSymbol, thirdSymbol);
    assertThat(new ArrayList<Integer>(referencesBySymbol.get(firstSymbol))).containsExactly(32);
    assertThat(new ArrayList<Integer>(referencesBySymbol.get(secondSymbol))).containsExactly(124);
    assertThat(new ArrayList<Integer>(referencesBySymbol.get(thirdSymbol))).containsExactly(70);

    assertThat(argCaptor.getValue().writeString()).isEqualTo("10,20,10,32;84,92,84,124;55,62,55,70");
  }

  @Test
  public void should_serialize_unused_symbol() throws Exception {

    ComponentDataCache componentDataCache = mock(ComponentDataCache.class);
    SymbolTableBuilder symbolTableBuilder = new DefaultSymbolTableBuilder("foo", componentDataCache);
    symbolTableBuilder.newSymbol(10, 20);
    symbolTableBuilder.done();

    ArgumentCaptor<SymbolData> argCaptor = ArgumentCaptor.forClass(SymbolData.class);
    verify(componentDataCache).setData(eq("foo"), eq(SnapshotDataTypes.SYMBOL_HIGHLIGHTING), argCaptor.capture());

    assertThat(argCaptor.getValue().writeString()).isEqualTo("10,20,10");
  }

  @Test
  public void should_reject_reference_conflicting_with_declaration() throws Exception {
    throwable.expect(UnsupportedOperationException.class);

    ComponentDataCache componentDataCache = mock(ComponentDataCache.class);
    SymbolTableBuilder symbolTableBuilder = new DefaultSymbolTableBuilder("foo", componentDataCache);
    Symbol symbol = symbolTableBuilder.newSymbol(10, 20);
    symbolTableBuilder.newReference(symbol, 15);
  }

  @Test
  public void should_reject_reference_from_another_file() throws Exception {
    throwable.expect(UnsupportedOperationException.class);

    ComponentDataCache componentDataCache = mock(ComponentDataCache.class);
    SymbolTableBuilder symbolTableBuilder = new DefaultSymbolTableBuilder("foo", componentDataCache);
    Symbol symbol = symbolTableBuilder.newSymbol(10, 20);

    SymbolTableBuilder symbolTableBuilder2 = new DefaultSymbolTableBuilder("foo2", componentDataCache);
    Symbol symbol2 = symbolTableBuilder2.newSymbol(30, 40);

    symbolTableBuilder.newReference(symbol2, 15);
  }

}
