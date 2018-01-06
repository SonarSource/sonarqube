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
package org.sonar.api.batch.fs;

/**
 * Represents a text range in an {@link InputFile}. 
 * 
 * A range is delimited by two {@link TextPointer}. 
 * <ul>
 * <li><code>TextRange(TextPointer(1, 0), TextPointer(1, 1))</code> represents the first character at line 1</li>
 * <li><code>TextRange(TextPointer(1, 0), TextPointer(1, 10))</code> represents the 10 first characters at line 1</li>
 * </ul>
 * @see InputFile#newRange(int, int, int, int)
 *
 * @since 5.2
 */
public interface TextRange {

  /**
   * Start position of the range
   */
  TextPointer start();

  /**
   * End position of the range
   */
  TextPointer end();

  /**
   * Test if the current range has some common area with another range.
   * Exemple: say the two ranges are on same line. Range with offsets [1,3] overlaps range with offsets [2,4] but not
   * range with offset [3,5]
   */
  boolean overlap(TextRange another);

}
