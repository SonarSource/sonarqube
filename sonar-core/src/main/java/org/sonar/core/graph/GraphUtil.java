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

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.ElementHelper;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import org.hibernate.engine.JoinSequence;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Iterator;
import java.util.List;

public class GraphUtil {

  private GraphUtil() {
  }

  /**
   * Get adjacent vertex. It assumes that there are only 0 or 1 results.
   *
   * @throws MultipleElementsException if there are more than 1 adjacent vertices with the given criteria.
   */
  @CheckForNull
  public static Vertex singleAdjacent(Vertex from, Direction direction, String... labels) {
    Iterator<Vertex> vertices = from.getVertices(direction, labels).iterator();
    Vertex result = null;
    if (vertices.hasNext()) {
      result = vertices.next();
      if (vertices.hasNext()) {
        throw new MultipleElementsException(String.format("More than one vertex is adjacent to: %s, direction: %s, labels: %s", from, direction, Joiner.on(",").join(labels)));
      }
    }
    return result;
  }

  public static <T extends Element> T single(Iterable<T> iterable) {
    Iterator<T> iterator = iterable.iterator();
    T result = null;
    if (iterator.hasNext()) {
      result = iterator.next();
      if (iterator.hasNext()) {
        throw new MultipleElementsException("More than one elements");
      }
    }
    return result;
  }

  public static void setNullableProperty(Element elt, String key, @Nullable Object value) {
    if (value == null) {
      elt.removeProperty(key);
    } else {
      elt.setProperty(key, value);
    }
  }

  public static void subGraph(GremlinPipeline path, TinkerGraph toGraph) {
    List<Edge> edges = Lists.newArrayList();
    if (path.hasNext()) {
      for (Object element : (Iterable) path.next()) {
        if (element instanceof Vertex) {
          Vertex v = (Vertex) element;
          Vertex toVertex = toGraph.addVertex(v.getId());
          ElementHelper.copyProperties(v, toVertex);
        } else if (element instanceof Edge) {
          edges.add((Edge) element);
        }
      }
      for (Edge edge : edges) {
        Vertex from = edge.getVertex(Direction.IN);
        Vertex to = edge.getVertex(Direction.OUT);
        Edge copyEdge = toGraph.addEdge(edge.getId(), toGraph.getVertex(from.getId()), toGraph.getVertex(to.getId()), edge.getLabel());
        ElementHelper.copyProperties(edge, copyEdge);
      }
    }
  }
}
