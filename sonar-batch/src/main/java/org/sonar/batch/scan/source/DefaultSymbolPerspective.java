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

import org.sonar.api.component.Component;
import org.sonar.api.scan.source.Symbol;
import org.sonar.api.scan.source.SymbolPerspective;

public class DefaultSymbolPerspective implements SymbolPerspective {

  private final SymbolDataCache symbolDataCache;
  private final Component component;
  private final SymbolDataRepository symbolDataRepository;

  public DefaultSymbolPerspective(SymbolDataCache symbolDataCache, Component component, SymbolDataRepository symbolDataRepository) {
    this.symbolDataCache = symbolDataCache;
    this.component = component;
    this.symbolDataRepository = symbolDataRepository;
  }

  @Override
  public SymbolPerspective begin() {
    return this;
  }

  @Override
  public SymbolBuilder newSymbol() {
    return new DefaultSymbol.Builder(symbolDataRepository);
  }

  @Override
  public ReferencesBuilder declareReferences(final Symbol symbol) {
    return new ReferencesBuilder() {
      @Override
      public ReferencesBuilder addReference(int startOffset) {
        symbolDataRepository.registerSymbolReference(symbol, startOffset);
        return this;
      }
    };
  }

  @Override
  public void end() {
    symbolDataCache.registerSymbolData(component().key(), symbolDataRepository.serializeAsString());
  }

  @Override
  public Component component() {
    return component;
  }
}
