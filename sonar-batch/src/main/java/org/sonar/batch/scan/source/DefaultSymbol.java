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

import org.sonar.api.scan.source.Symbol;
import org.sonar.api.scan.source.SymbolPerspective;

public class DefaultSymbol implements Symbol {

  private int declarationStartOffset;
  private int declarationEndOffset;
  private String fullyQualifiedName;

  private DefaultSymbol(int startOffset, int endOffset, String fullyQualifiedName) {
    this.declarationStartOffset = startOffset;
    this.declarationEndOffset = endOffset;
    this.fullyQualifiedName = fullyQualifiedName;
  }

  public static Builder builder(SymbolDataRepository dataRepository) {
    return new Builder(dataRepository);
  }

  public int getDeclarationStartOffset() {
    return declarationStartOffset;
  }

  public int getDeclarationEndOffset() {
    return declarationEndOffset;
  }

  public String getFullyQualifiedName() {
    return fullyQualifiedName;
  }

  public static class Builder implements SymbolPerspective.SymbolBuilder {

    private int declarationStartOffset;
    private int declarationEndOffset;
    private String fullyQualifiedName;
    private final SymbolDataRepository dataRepository;


    public Builder(SymbolDataRepository dataRepository) {
      this.dataRepository = dataRepository;
    }

    @Override
    public SymbolPerspective.SymbolBuilder setDeclaration(int startOffset, int endOffset) {
      this.declarationStartOffset = startOffset;
      this.declarationEndOffset = endOffset;
      return this;
    }

    @Override
    public SymbolPerspective.SymbolBuilder setFullyQualifiedName(String fullyQualifiedName) {
      this.fullyQualifiedName = fullyQualifiedName;
      return this;
    }

    @Override
    public Symbol build() {
      Symbol symbol = new DefaultSymbol(declarationStartOffset, declarationEndOffset, fullyQualifiedName);
      dataRepository.registerSymbol(symbol);
      return symbol;
    }
  }
}
