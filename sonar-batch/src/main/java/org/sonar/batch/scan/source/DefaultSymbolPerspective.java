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

package org.sonar.batch.scan.source;

import org.sonar.api.component.Component;
import org.sonar.api.scan.source.Symbol;
import org.sonar.api.scan.source.SymbolPerspective;
import org.sonar.batch.index.ComponentDataCache;
import org.sonar.core.source.jdbc.SnapshotDataDto;

public class DefaultSymbolPerspective implements SymbolPerspective {

  private final ComponentDataCache cache;
  private final Component component;
  private final SymbolDataRepository symbolDataRepository;

  public DefaultSymbolPerspective(ComponentDataCache cache, Component component, SymbolDataRepository symbolDataRepository) {
    this.cache = cache;
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
    cache.setStringData(component().key(), SnapshotDataDto.SYMBOL, symbolDataRepository.writeString());
  }

  @Override
  public Component component() {
    return component;
  }
}
