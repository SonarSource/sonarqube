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
package org.sonar.api.web;

public enum WidgetPropertyType {
  /**
   * Integer value, positive or negative
   */
  INTEGER,

  /**
   * True/False
   */
  BOOLEAN,

  /**
   * Floating point number
   */
  FLOAT,

  /**
   * Basic single line input field
   */
  STRING,

  /**
   * Sonar Metric
   *
   * @since 2.10
   */
  METRIC,

  /**
   * Measure Filter id
   *
   * @since 3.1
   */
  FILTER,

  /**
   * Issue Filter id
   *
   * @since 3.7
   */
  ISSUE_FILTER,

  /**
   * Multiple line text-area
   *
   * @since 3.2
   */
  TEXT,

  /**
   * Variation of {#STRING} with masked characters
   *
   * @since 3.2
   */
  PASSWORD,

  /**
   * Regular expression
   *
   * @since 3.2
   */
  REGULAR_EXPRESSION,

  /**
   * Single select list with a list of options
   *
   * @since 3.5
   */
  SINGLE_SELECT_LIST
}
