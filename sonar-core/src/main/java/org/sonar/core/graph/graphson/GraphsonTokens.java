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
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette
 */
class GraphsonTokens {
  private GraphsonTokens() {
  }

  static final String VERTEX = "vertex";
  static final String EDGE = "edge";
  static final String _ID = "_id";
  static final String _LABEL = "_label";
  static final String _TYPE = "_type";
  static final String _OUT_V = "_outV";
  static final String _IN_V = "_inV";
  static final String VALUE = "value";
  static final String TYPE = "type";
  static final String TYPE_LIST = "list";
  static final String TYPE_STRING = "string";
  static final String TYPE_DOUBLE = "double";
  static final String TYPE_INTEGER = "integer";
  static final String TYPE_FLOAT = "float";
  static final String TYPE_MAP = "map";
  static final String TYPE_BOOLEAN = "boolean";
  static final String TYPE_LONG = "long";
  static final String TYPE_UNKNOWN = "unknown";

  static final String VERTICES = "vertices";
  static final String EDGES = "edges";
  static final String MODE = "mode";
}
