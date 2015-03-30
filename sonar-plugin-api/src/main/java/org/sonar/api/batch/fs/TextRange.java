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
package org.sonar.api.batch.fs;

/**
 * Represents a text range in an {@link InputFile}
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
