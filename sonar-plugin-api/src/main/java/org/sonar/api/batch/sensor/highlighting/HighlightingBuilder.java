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
package org.sonar.api.batch.sensor.highlighting;

/**
 * This builder is used to define syntax highlighting (aka code coloration) on files.
 * @since 4.5
 */
public interface HighlightingBuilder {

  /**
   * Call this method to indicate the type of text in a range.
   * @param startOffset Starting position in file for this type of text. Beginning of a file starts with offset '0'.
   * @param endOffset End position in file for this type of text.
   * @param typeOfText see {@link TypeOfText} values.
   */
  HighlightingBuilder highlight(int startOffset, int endOffset, TypeOfText typeOfText);

  /**
   * Call this method only once when your are done with defining highlighting of the file.
   * @throws IllegalStateException if you have defined overlapping highlighting
   */
  void done();
}
