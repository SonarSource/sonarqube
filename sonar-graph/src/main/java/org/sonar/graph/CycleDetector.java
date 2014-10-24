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
package org.sonar.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CycleDetector<V> {

  private Set<V> vertices;
  private DirectedGraphAccessor<V, ? extends Edge> graph;
  private Set<V> analyzedVertices;
  private Set<Cycle> cycles = new LinkedHashSet<Cycle>();
  private Set<Edge> edgesToExclude;
  private long searchCyclesCalls = 0;
  private int maxSearchDepth = -1;
  private boolean maxSearchDepthActivated = false;
  private int maxCyclesToFound = Integer.MAX_VALUE;

  public CycleDetector(DirectedGraphAccessor<V, ? extends Edge> graph, Collection<V> vertices) {
    init(graph, vertices, new LinkedHashSet<Edge>());
  }

  public CycleDetector(DirectedGraphAccessor<V, ? extends Edge> graph, Collection<V> vertices, Set<Edge> edgesToExclude) {
    init(graph, vertices, edgesToExclude);
  }

  public CycleDetector(DirectedGraphAccessor<V, ? extends Edge> graph) {
    init(graph, graph.getVertices(), new LinkedHashSet<Edge>());
  }

  public CycleDetector(DirectedGraphAccessor<V, ? extends Edge> graph, Set<Edge> edgesToExclude) {
    init(graph, graph.getVertices(), edgesToExclude);
  }

  private void init(DirectedGraphAccessor<V, ? extends Edge> graph, Collection<V> vertices, Set<Edge> edgesToExclude) {
    this.graph = graph;
    this.vertices = new LinkedHashSet<V>(vertices);
    this.analyzedVertices = new LinkedHashSet<V>();
    this.edgesToExclude = edgesToExclude;
  }

  public Set<Cycle> detectCycles() {
    run();
    return getCycles();
  }

  public Set<Cycle> detectCyclesWithUpperLimit(int maxCyclesToFound) {
    this.maxCyclesToFound = maxCyclesToFound;
    run();
    return getCycles();
  }

  public Set<Cycle> detectCyclesWithMaxSearchDepth(int maxSearchDepth) {
    if (maxSearchDepth > 1) {
      maxSearchDepthActivated = true;
      this.maxSearchDepth = maxSearchDepth;
    }
    run();
    return getCycles();
  }

  private void run() {
    if (!cycles.isEmpty()) {
      throw new IllegalStateException("Cycle detection can't be executed twice on the same CycleDetector object.");
    }
    try {
      for (V vertex : vertices) {
        if (maxSearchDepthActivated || !analyzedVertices.contains(vertex)) {
          Set<V> tmpAnalyzedVertices = new LinkedHashSet<V>();
          searchCycles(vertex, new ArrayList<V>(), tmpAnalyzedVertices);
          analyzedVertices.addAll(tmpAnalyzedVertices);
        }
      }
    } catch (MaximumCyclesToFoundException e) {
      // ignore
    }
  }

  private void searchCycles(V fromVertex, List<V> path, Set<V> tmpAnalyzedVertices) {
    searchCyclesCalls++;
    path.add(fromVertex);
    tmpAnalyzedVertices.add(fromVertex);
    for (Edge<V> edge : graph.getOutgoingEdges(fromVertex)) {
      V toVertex = edge.getTo();
      if (!edgesToExclude.contains(edge) && vertices.contains(toVertex)
        && (maxSearchDepthActivated || !analyzedVertices.contains(toVertex))) {
        if (path.contains(toVertex)) {
          path.add(toVertex);
          List<V> cyclePath = path.subList(path.indexOf(toVertex), path.size());
          Cycle cycle = convertListOfVerticesToCycle(cyclePath);
          cycles.add(cycle);

          if (cycles.size() >= maxCyclesToFound) {
            throw new MaximumCyclesToFoundException();
          }
          path.remove(path.size() - 1);
        } else if (!maxSearchDepthActivated || (maxSearchDepthActivated && path.size() < maxSearchDepth)) {
          searchCycles(toVertex, path, tmpAnalyzedVertices);
        }
      }
    }
    path.remove(path.size() - 1);
  }

  private Cycle convertListOfVerticesToCycle(List<V> vertices) {
    List<Edge> edges = new ArrayList<Edge>();
    V firstVertex = vertices.get(0);
    V from = firstVertex;
    for (int index = 1; index < vertices.size(); index++) {
      V to = vertices.get(index);
      edges.add(graph.getEdge(from, to));
      from = to;
    }
    return new Cycle(edges);
  }

  public Set<Cycle> getCycles() {
    return cycles;
  }

  public boolean isAcyclicGraph() {
    return cycles.isEmpty();
  }

  public long getSearchCyclesCalls() {
    return searchCyclesCalls;
  }

}

class MaximumCyclesToFoundException extends RuntimeException {
}
