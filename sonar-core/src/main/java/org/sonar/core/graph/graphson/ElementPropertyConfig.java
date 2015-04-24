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

import java.util.Set;

/**
 * Configure how the GraphSON utility treats edge and vertex properties.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ElementPropertyConfig {

  enum ElementPropertiesRule {
    INCLUDE, EXCLUDE
  }

  private final Set<String> vertexPropertyKeys;
  private final Set<String> edgePropertyKeys;
  private final ElementPropertiesRule vertexPropertiesRule;
  private final ElementPropertiesRule edgePropertiesRule;

  /**
   * A configuration that includes all properties of vertices and edges.
   */
  static ElementPropertyConfig AllProperties = new ElementPropertyConfig(null, null,
    ElementPropertiesRule.INCLUDE, ElementPropertiesRule.INCLUDE);

  ElementPropertyConfig(Set<String> vertexPropertyKeys, Set<String> edgePropertyKeys,
                        ElementPropertiesRule vertexPropertiesRule, ElementPropertiesRule edgePropertiesRule) {
    this.vertexPropertiesRule = vertexPropertiesRule;
    this.vertexPropertyKeys = vertexPropertyKeys;
    this.edgePropertiesRule = edgePropertiesRule;
    this.edgePropertyKeys = edgePropertyKeys;
  }

  /**
   * Construct a configuration that includes the specified properties from both vertices and edges.
   */
  static ElementPropertyConfig includeProperties(Set<String> vertexPropertyKeys,
                                                 Set<String> edgePropertyKeys) {
    return new ElementPropertyConfig(vertexPropertyKeys, edgePropertyKeys, ElementPropertiesRule.INCLUDE,
      ElementPropertiesRule.INCLUDE);
  }

  /**
   * Construct a configuration that excludes the specified properties from both vertices and edges.
   */
  static ElementPropertyConfig excludeProperties(Set<String> vertexPropertyKeys,
                                                 Set<String> edgePropertyKeys) {
    return new ElementPropertyConfig(vertexPropertyKeys, edgePropertyKeys, ElementPropertiesRule.EXCLUDE,
      ElementPropertiesRule.EXCLUDE);
  }

  Set<String> getVertexPropertyKeys() {
    return vertexPropertyKeys;
  }

  Set<String> getEdgePropertyKeys() {
    return edgePropertyKeys;
  }

  ElementPropertiesRule getVertexPropertiesRule() {
    return vertexPropertiesRule;
  }

  ElementPropertiesRule getEdgePropertiesRule() {
    return edgePropertiesRule;
  }
}
