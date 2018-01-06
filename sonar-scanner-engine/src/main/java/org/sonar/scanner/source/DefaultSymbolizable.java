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

import java.util.Collections;
import java.util.List;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.source.Symbol;
import org.sonar.api.source.Symbolizable;
import org.sonar.scanner.sensor.DefaultSensorStorage;

public class DefaultSymbolizable implements Symbolizable {

  private static final NoOpSymbolTableBuilder NO_OP_SYMBOL_TABLE_BUILDER = new NoOpSymbolTableBuilder();
  private static final NoOpSymbolTable NO_OP_SYMBOL_TABLE = new NoOpSymbolTable();
  private static final NoOpSymbol NO_OP_SYMBOL = new NoOpSymbol();

  private static final class NoOpSymbolTableBuilder implements SymbolTableBuilder {
    @Override
    public Symbol newSymbol(int fromOffset, int toOffset) {
      return NO_OP_SYMBOL;
    }

    @Override
    public Symbol newSymbol(int startLine, int startLineOffset, int endLine, int endLineOffset) {
      return NO_OP_SYMBOL;
    }

    @Override
    public void newReference(Symbol symbol, int fromOffset) {
      // Do nothing
    }

    @Override
    public void newReference(Symbol symbol, int fromOffset, int toOffset) {
      // Do nothing
    }

    @Override
    public void newReference(Symbol symbol, int startLine, int startLineOffset, int endLine, int endLineOffset) {
      // Do nothing
    }

    @Override
    public SymbolTable build() {
      return NO_OP_SYMBOL_TABLE;
    }
  }

  private static final class NoOpSymbolTable implements SymbolTable {
    @Override
    public List<Symbol> symbols() {
      return Collections.emptyList();
    }

    @Override
    public List<Integer> references(Symbol symbol) {
      return Collections.emptyList();
    }
  }

  private static final class NoOpSymbol implements Symbol {
    @Override
    public String getFullyQualifiedName() {
      return null;
    }

    @Override
    public int getDeclarationStartOffset() {
      return 0;
    }

    @Override
    public int getDeclarationEndOffset() {
      return 0;
    }
  }

  private final InputFile inputFile;
  private final DefaultSensorStorage sensorStorage;
  private final AnalysisMode analysisMode;

  public DefaultSymbolizable(InputFile inputFile, DefaultSensorStorage sensorStorage, AnalysisMode analysisMode) {
    this.inputFile = inputFile;
    this.sensorStorage = sensorStorage;
    this.analysisMode = analysisMode;
  }

  @Override
  public SymbolTableBuilder newSymbolTableBuilder() {
    if (analysisMode.isIssues()) {
      return NO_OP_SYMBOL_TABLE_BUILDER;
    }
    return new DeprecatedDefaultSymbolTable.Builder(new DefaultSymbolTable(sensorStorage).onFile(inputFile));
  }

  @Override
  public void setSymbolTable(SymbolTable symbolTable) {
    if (analysisMode.isIssues()) {
      // No need for symbols in issues mode
      return;
    }
    ((DeprecatedDefaultSymbolTable) symbolTable).getWrapped().save();
  }
}
