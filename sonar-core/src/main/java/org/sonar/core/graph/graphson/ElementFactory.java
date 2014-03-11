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

import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;

/**
 * The standard factory used for most graph element creation.  It uses an actual
 * Graph implementation to construct vertices and edges
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
class ElementFactory {

  private final Graph graph;

  ElementFactory(Graph g) {
    this.graph = g;
  }

  Edge createEdge(Object id, Vertex out, Vertex in, String label) {
    return this.graph.addEdge(id, out, in, label);
  }

  Vertex createVertex(Object id) {
    return this.graph.addVertex(id);
  }
}
