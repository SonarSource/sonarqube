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
package org.sonar.core.graph.graphson;

/**
 * Modes of operation of the GraphSONUtility.
 *
 * @author Stephen Mallette
 */
public enum GraphsonMode {
  /**
   * COMPACT constructs GraphSON on the assumption that all property keys
   * are fair game for exclusion including _type, _inV, _outV, _label and _id.
   * It is possible to write GraphSON that cannot be read back into Graph,
   * if some or all of these keys are excluded.
   */
  COMPACT,

  /**
   * NORMAL includes the _type field and JSON data typing.
   */
  NORMAL,

  /**
   * EXTENDED includes the _type field and explicit data typing.
   */
  EXTENDED
}
