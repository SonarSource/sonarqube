/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.batch.scan.source;

import org.junit.Test;
import org.sonar.api.component.Component;
import org.sonar.api.scan.source.Symbol;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.core.source.jdbc.SnapshotDataDto;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DefaultSymbolPerspectiveTest {

  @Test
  public void should_store_references_for_new_symbol() throws Exception {

    Component component = mock(Component.class);
    ComponentDataCache cache = mock(ComponentDataCache.class);
    SymbolDataRepository symbolDataRepository = mock(SymbolDataRepository.class);

    DefaultSymbolPerspective symbolPerspective = new DefaultSymbolPerspective(cache, component, symbolDataRepository);

    Symbol symbol = symbolPerspective.newSymbol().build();
    symbolPerspective.declareReferences(symbol).addReference(1).addReference(20);

    verify(symbolDataRepository).registerSymbol(symbol);
    verify(symbolDataRepository).registerSymbolReference(symbol, 1);
    verify(symbolDataRepository).registerSymbolReference(symbol, 20);
  }

  @Test
  public void should_update_cache_when_done() throws Exception {

    Component component = mock(Component.class);
    when(component.key()).thenReturn("myComponent");

    ComponentDataCache cache = mock(ComponentDataCache.class);

    DefaultSymbolPerspective symbolPerspective = new DefaultSymbolPerspective(cache, component, new SymbolDataRepository());
    Symbol firstSymbol = symbolPerspective.newSymbol().setDeclaration(4, 8).build();
    symbolPerspective.declareReferences(firstSymbol).addReference(12).addReference(70);
    Symbol otherSymbol = symbolPerspective.newSymbol().setDeclaration(25, 33).build();
    symbolPerspective.declareReferences(otherSymbol).addReference(44).addReference(60).addReference(108);

    symbolPerspective.end();

    verify(cache).setStringData("myComponent", SnapshotDataDto.SYMBOL, "4,8,4,12,70;25,33,25,44,60,108;");
  }
}
