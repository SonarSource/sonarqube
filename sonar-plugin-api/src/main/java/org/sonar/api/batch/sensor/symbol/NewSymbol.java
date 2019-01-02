/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.api.batch.sensor.symbol;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;

/**
 * This builder is used to define symbol references on files.
 * @since 5.6
 */
public interface NewSymbol {

  /**
   * Register a new symbol reference.
   * @param startOffset Starting position in file for the declaration of this symbol. Beginning of a file starts with offset '0'.
   * @param endOffset End position in file for this symbol declaration.
   * @deprecated since 6.1 Only supported to ease migration from old API. Please prefer other {@code newReference()} methods.
   */
  @Deprecated
  NewSymbol newReference(int startOffset, int endOffset);

  /**
   * Register a new symbol.
   * @param range Range of text for the symbol declaration. See for example {@link InputFile#newRange(int, int, int, int)}.
   */
  NewSymbol newReference(TextRange range);

  /**
   * Shortcut to avoid calling {@link InputFile#newRange(int, int, int, int)}
   */
  NewSymbol newReference(int startLine, int startLineOffset, int endLine, int endLineOffset);
}
