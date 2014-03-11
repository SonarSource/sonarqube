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

package org.sonar.squid.api;

public enum SourceCodeEdgeUsage {
  /**
   * Example: class A extends class B
   */
  EXTENDS,

  /**
   * Example: class A implements an interface B
   */
  IMPLEMENTS,

  /**
   * Examples:
   * <ul>
   * <li>method A returns an object of type B</li>
   * <li>method A declares a parameter of type B</li>
   * <li>method A throws an exception of type B</li>
   * <li>method A catch an exception of type B</li>
   * </ul>
   */
  USES,

  CALLS_FIELD, CALLS_METHOD,

  /**
   * Example: class A declares a field of type B
   */
  CONTAINS,

  /**
   * Unknown type
   */
  NO_LINK
}
