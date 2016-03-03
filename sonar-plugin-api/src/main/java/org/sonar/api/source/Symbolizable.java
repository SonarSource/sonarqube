/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.api.source;

import org.sonar.api.component.Perspective;
import org.sonar.api.component.ResourcePerspectives;

import java.util.List;

/**
 * Use this perspective to save symbol references on files.
 * See {@link ResourcePerspectives}.
 * @since 3.6
 */
public interface Symbolizable extends Perspective {

  interface SymbolTableBuilder {

    /**
     * Creates a new Symbol.
     * The offsets are global in the file.
     */
    Symbol newSymbol(int fromOffset, int toOffset);

    /**
     * Creates a new reference for a symbol.
     * The length of the reference is assumed to be the same as the symbol's length.
     */
    void newReference(Symbol symbol, int fromOffset);
    
    /**
     * Creates a new reference for a symbol.
     * The offsets are global in the file.
     *
     * @since 5.3
     */
    void newReference(Symbol symbol, int fromOffset, int toOffset);

    /**
     * Creates a {@link SymbolTable} containing all symbols and references previously created in this file.
     */
    SymbolTable build();

  }

  interface SymbolTable {

    List<Symbol> symbols();

    /**
     * @deprecated since 5.2 not used
     */
    @Deprecated
    List<Integer> references(Symbol symbol);
  }

  SymbolTableBuilder newSymbolTableBuilder();

  void setSymbolTable(SymbolTable symbolTable);
}
