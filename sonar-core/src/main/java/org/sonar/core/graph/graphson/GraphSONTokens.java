/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.graph.graphson;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette
 */
class GraphSONTokens {
  public static final String VERTEX = "vertex";
  public static final String EDGE = "edge";
  public static final String _ID = "_id";
  public static final String _LABEL = "_label";
  public static final String _TYPE = "_type";
  public static final String _OUT_V = "_outV";
  public static final String _IN_V = "_inV";
  public static final String VALUE = "value";
  public static final String TYPE = "type";
  public static final String TYPE_LIST = "list";
  public static final String TYPE_STRING = "string";
  public static final String TYPE_DOUBLE = "double";
  public static final String TYPE_INTEGER = "integer";
  public static final String TYPE_FLOAT = "float";
  public static final String TYPE_MAP = "map";
  public static final String TYPE_BOOLEAN = "boolean";
  public static final String TYPE_LONG = "long";
  public static final String TYPE_UNKNOWN = "unknown";

  public static final String VERTICES = "vertices";
  public static final String EDGES = "edges";
  public static final String MODE = "mode";
}
