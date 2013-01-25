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
package org.sonar.core.component;

import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.io.graphson.GraphSONReader;
import org.sonar.core.component.ComponentGraph;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class GraphReader {

  public ComponentGraph read(String data, String rootVertexId) {
    ByteArrayInputStream input = new ByteArrayInputStream(data.getBytes());
    try {
      TinkerGraph graph = new TinkerGraph();
      GraphSONReader.inputGraph(graph, input);
      Vertex root = graph.getVertex(rootVertexId);
      return new ComponentGraph(graph, root);
    } catch (IOException e) {
      throw new IllegalStateException();
    }
  }
}
