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

import org.junit.Test;
import org.sonar.api.scan.source.Symbol;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DefaultSymbolTest {

  @Test
  public void should_build_default_symbol() throws Exception {

    SymbolDataRepository dataRepository = mock(SymbolDataRepository.class);

    Symbol symbol = DefaultSymbol.builder(dataRepository).setDeclaration(10, 20).setFullyQualifiedName("org.foo.Bar#myMethod").build();

    assertThat(symbol.getDeclarationStartOffset()).isEqualTo(10);
    assertThat(symbol.getDeclarationEndOffset()).isEqualTo(20);
    assertThat(symbol.getFullyQualifiedName()).isEqualTo("org.foo.Bar#myMethod");

    verify(dataRepository).registerSymbol(symbol);
  }
}
