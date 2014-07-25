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
package org.sonar.api.batch.sensor.symbol;

/**
 * Use this builder to create symbol references. For now only references
 * in the same file are supported.
 * @since 4.5
 */
public interface SymbolTableBuilder {

  /**
   * Create a new symbol.
   * @param fromOffset Starting offset in a file for the symbol declaration. File starts at offset '0'.
   * @param toOffset Ending offset of symbol declaration.
   * @return a new Symbol that can be used later in {@link #newReference(Symbol, int)}
   */
  Symbol newSymbol(int fromOffset, int toOffset);

  /**
   * Records that a {@link Symbol} is referenced at another location in the same file.
   * @param symbol Symbol previously created with {@link #newSymbol(int, int)}
   * @param fromOffset Starting offset of the place symbol is referenced. No need for end offset here since we assume it is same length.
   */
  void newReference(Symbol symbol, int fromOffset);

  /**
   * Call this method only once when your are done with defining symbols of the file.
   */
  void done();
}
