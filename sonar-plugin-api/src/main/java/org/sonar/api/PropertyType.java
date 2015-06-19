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
package org.sonar.api;

/**
 * @since 3.0
 */
public enum PropertyType {
  /**
   * Basic single line input field
   */
  STRING,

  /**
   * Multiple line text-area
   */
  TEXT,

  /**
   * Variation of {#STRING} with masked characters
   */
  PASSWORD,

  /**
   * True/False
   */
  BOOLEAN,

  /**
   * Integer value, positive or negative
   */
  INTEGER,

  /**
   * Floating point number
   */
  FLOAT,

  /**
   * Single select list with a list of options
   */
  SINGLE_SELECT_LIST,

  /**
   * Sonar Metric
   */
  METRIC,

  /**
   * SonarSource license
   */
  LICENSE,

  /**
   * Regular expression
   *
   * @since 3.2
   */
  REGULAR_EXPRESSION,

  /**
   * Property set instance
   *
   * @since 3.3
   */
  PROPERTY_SET,

  /**
  * User login
  * @since 5.1
  */
  USER_LOGIN,

  /**
   * Level metric type
   */
  METRIC_LEVEL,

  /**
   * Long value, positivie or negative
   */
  LONG
}
