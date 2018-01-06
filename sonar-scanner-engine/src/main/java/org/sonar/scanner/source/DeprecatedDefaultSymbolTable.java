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

import java.util.List;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;

public class DeprecatedDefaultSymbolTable implements Symbolizable.SymbolTable {

  private final DefaultSymbolTable wrapped;

  public DeprecatedDefaultSymbolTable(DefaultSymbolTable wrapped) {
    this.wrapped = wrapped;
  }

  public DefaultSymbolTable getWrapped() {
    return wrapped;
  }

  @Override
  public List<Symbol> symbols() {
    throw new UnsupportedOperationException("symbols");
  }

  @Override
  public List<Integer> references(Symbol symbol) {
    throw new UnsupportedOperationException("references");
  }

  public static class Builder implements Symbolizable.SymbolTableBuilder {

    private final DefaultSymbolTable symbolTable;

    public Builder(DefaultSymbolTable symbolTable) {
      this.symbolTable = symbolTable;
    }

    @Override
    public Symbol newSymbol(int fromOffset, int toOffset) {
      return new DeprecatedDefaultSymbol(symbolTable.newSymbol(fromOffset, toOffset), toOffset - fromOffset);
    }

    @Override
    public Symbol newSymbol(int startLine, int startLineOffset, int endLine, int endLineOffset) {
      // This is wrong in case of multiline symbol bu I assume references will be added using start and end offsets so length is useless.
      int length = endLineOffset - startLineOffset;
      return new DeprecatedDefaultSymbol(symbolTable.newSymbol(startLine, startLineOffset, endLine, endLineOffset), length);
    }

    @Override
    public void newReference(Symbol symbol, int fromOffset) {
      ((DeprecatedDefaultSymbol) symbol).getWrapped().newReference(fromOffset, fromOffset + ((DeprecatedDefaultSymbol) symbol).getLength());
    }

    @Override
    public void newReference(Symbol symbol, int fromOffset, int toOffset) {
      ((DeprecatedDefaultSymbol) symbol).getWrapped().newReference(fromOffset, toOffset);
    }

    @Override
    public void newReference(Symbol symbol, int startLine, int startLineOffset, int endLine, int endLineOffset) {
      ((DeprecatedDefaultSymbol) symbol).getWrapped().newReference(startLine, startLineOffset, endLine, endLineOffset);
    }

    @Override
    public Symbolizable.SymbolTable build() {
      return new DeprecatedDefaultSymbolTable(symbolTable);
    }

  }
}
