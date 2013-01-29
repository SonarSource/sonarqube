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
package org.sonar.core.graph;

import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.ElementHelper;

import java.util.List;

/**
 * Not thread-safe
 */
public class SubGraph {

  private TinkerGraph sub = new TinkerGraph();
  private List<Edge> edgesToCopy = Lists.newArrayList();

  private SubGraph() {
  }

  private Graph process(Vertex start, Object... edgePath) {
    browse(start, 0, edgePath);
    for (Edge edge : edgesToCopy) {
      Vertex from = edge.getVertex(Direction.IN);
      Vertex to = edge.getVertex(Direction.OUT);
      Edge copyEdge = sub.addEdge(edge.getId(), sub.getVertex(from.getId()), sub.getVertex(to.getId()), edge.getLabel());
      ElementHelper.copyProperties(edge, copyEdge);
    }
    return sub;
  }

  public static Graph extract(Vertex start, Object... edgePath) {
    return new SubGraph().process(start, edgePath);
  }

  private void browse(Vertex vertex, int cursor, Object... edgePath) {
    if (vertex != null) {
      copy(vertex);
      if (cursor < edgePath.length) {
        String edgeLabel = (String) edgePath[cursor];
        Direction edgeDirection = (Direction) edgePath[cursor + 1];
        Iterable<Edge> edges = vertex.getEdges(edgeDirection, edgeLabel);
        for (Edge edge : edges) {
          edgesToCopy.add(edge);
          browse(edge.getVertex(edgeDirection.opposite()), cursor + 2, edgePath);
        }
      }
    }
  }

  private Vertex copy(Vertex v) {
    Vertex to = sub.addVertex(v.getId());
    ElementHelper.copyProperties(v, to);
    return to;
  }
}
