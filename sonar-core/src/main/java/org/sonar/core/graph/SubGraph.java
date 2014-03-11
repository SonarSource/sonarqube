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
package org.sonar.core.graph;

import com.google.common.collect.Sets;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.ElementHelper;

import java.util.List;
import java.util.Set;

/**
 * Not thread-safe
 */
public class SubGraph {

  private TinkerGraph sub = new TinkerGraph();
  private Set<Edge> edgesToCopy = Sets.newHashSet();

  private SubGraph() {
  }

  public static Graph extract(Vertex start, EdgePath edgePath) {
    return new SubGraph().process(start, edgePath);
  }

  private Graph process(Vertex start, EdgePath edgePath) {
    copy(start);
    browse(start, 0, edgePath.getElements());
    for (Edge edge : edgesToCopy) {
      Vertex from = edge.getVertex(Direction.OUT);
      Vertex to = edge.getVertex(Direction.IN);
      Edge copyEdge = sub.addEdge(edge.getId(), sub.getVertex(from.getId()), sub.getVertex(to.getId()), edge.getLabel());
      ElementHelper.copyProperties(edge, copyEdge);
    }
    return sub;
  }

  private void browse(Vertex from, int cursor, List<Object> edgePath) {
    if (from != null && cursor < edgePath.size()) {
      Direction edgeDirection = (Direction) edgePath.get(cursor);
      String edgeLabel = (String) edgePath.get(cursor + 1);
      Iterable<Edge> edges = from.getEdges(edgeDirection, edgeLabel);
      for (Edge edge : edges) {
        edgesToCopy.add(edge);
        Vertex tail = edge.getVertex(edgeDirection.opposite());
        copy(tail);
        browse(tail, cursor + 2, edgePath);
      }
    }
  }

  private Vertex copy(Vertex v) {
    Vertex to = sub.getVertex(v.getId());
    if (to == null) {
      to = sub.addVertex(v.getId());
      ElementHelper.copyProperties(v, to);
    }
    return to;
  }
}
