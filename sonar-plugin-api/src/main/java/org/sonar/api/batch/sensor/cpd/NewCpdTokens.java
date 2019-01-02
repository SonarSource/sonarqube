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
package org.sonar.api.batch.sensor.cpd;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;

/**
 * This builder is used to define tokens used by CPD algorithm on files.
 * 
 * Example:
 * 
 * <pre>
 *   sensorContext.newCpdTokens().onFile(inputFile)
 *     .addToken(1, 10, 1, 15, "class")
 *     .addToken(1, 16, 1, 18, "IDENTIFIER")
 *     // Add more tokens
 *     .save;
 *     
 * </pre>
 * @since 5.5
 */
public interface NewCpdTokens {

  /**
   * The tokenized file.
   */
  NewCpdTokens onFile(InputFile inputFile);

  /**
   * Call this method to register a token in a range. Tokens should be registered in order.
   * @param range Token position. Use {@link InputFile#newRange(int, int, int, int)} to get a valid range.
   * @param image Text content of the token. Can be replaced by a constant placeholder for some tokens (like litterals).
   */
  NewCpdTokens addToken(TextRange range, String image);

  /**
   * Shortcut to avoid calling {@link InputFile#newRange(int, int, int, int)}
   * @since 5.6
   */
  NewCpdTokens addToken(int startLine, int startLineOffset, int endLine, int endLineOffset, String image);

  /**
   * Call this method only once when your are done with defining tokens of the file. It is not supported to save CPD tokens twice for the same file.
   */
  void save();
}
