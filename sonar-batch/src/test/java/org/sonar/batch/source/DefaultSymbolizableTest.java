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
import org.sonar.api.component.Component;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.core.source.SnapshotDataType;

import static org.mockito.Mockito.*;

public class DefaultSymbolizableTest {

  @Test
  public void should_update_cache_when_done() throws Exception {

    Component component = mock(Component.class);
    when(component.key()).thenReturn("myComponent");

    ComponentDataCache cache = mock(ComponentDataCache.class);

    DefaultSymbolizable symbolPerspective = new DefaultSymbolizable(cache, component);
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

    verify(cache).setStringData("myComponent", SnapshotDataType.SYMBOL_HIGHLIGHTING.getValue(), "4,8,4,12,70;25,33,25,44,60,108;");
  }
}
