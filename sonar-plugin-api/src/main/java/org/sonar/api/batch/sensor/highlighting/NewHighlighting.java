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
package org.sonar.api.batch.sensor.highlighting;

import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;

/**
 * This builder is used to define syntax highlighting (aka code coloration) on files.
 * 
 * Example:
 * 
 * <pre>
 *   sensorContext.newHighlighting().onFile(inputFile)
 *     .highlight(1, 10, 1, 15, KEYWORD)
 *     .highlight(1, 16, 1, 18, STRING)
 *     // Add more highlight if needed
 *     .save();
 *     
 * </pre>
 * 
 * @since 5.1
 */
public interface NewHighlighting {

  /**
   * The file the highlighting belongs to.
   */
  NewHighlighting onFile(InputFile inputFile);

  /**
   * Call this method to indicate the type of text in a range.
   * @param startOffset Starting position in file for this type of text. Beginning of a file starts with offset '0'.
   * @param endOffset End position in file for this type of text.
   * @param typeOfText see {@link TypeOfText} values.
   * @deprecated since 5.6 Only supported to ease migration from old API. Please prefer other {@code highlight()} methods.
   */
  @Deprecated
  NewHighlighting highlight(int startOffset, int endOffset, TypeOfText typeOfText);

  /**
   * Call this method to indicate the type of text in a range.
   * @param range Range of text to highlight. See for example {@link InputFile#newRange(int, int, int, int)}.
   * @param typeOfText see {@link TypeOfText} values.
   * @since 5.6
   */
  NewHighlighting highlight(TextRange range, TypeOfText typeOfText);

  /**
   * Shortcut to avoid calling {@link InputFile#newRange(int, int, int, int)}
   * @param typeOfText see {@link TypeOfText} values.
   * @since 5.6
   */
  NewHighlighting highlight(int startLine, int startLineOffset, int endLine, int endLineOffset, TypeOfText typeOfText);

  /**
   * Call this method only once when your are done with defining highlighting of the file. It is not supported to save highlighting twice for the same file.
   * @throws IllegalStateException if you have defined overlapping highlighting
   */
  void save();
}
