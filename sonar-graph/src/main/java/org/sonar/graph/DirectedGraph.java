/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DirectedGraph<V, E extends Edge<V>> implements DirectedGraphAccessor<V, E> {

  private EdgeFactory<V, E> edgeFactory;
  private Map<V, Map<V, E>> outgoingEdgesByVertex = new HashMap<V, Map<V, E>>();
  private Map<V, Map<V, E>> incomingEdgesByVertex = new HashMap<V, Map<V, E>>();
  private Set<V> vertices = new HashSet<V>();

  public DirectedGraph() {
  }

  public DirectedGraph(EdgeFactory<V, E> edgeFactory) {
    this.edgeFactory = edgeFactory;
  }

  public static DirectedGraph<String, StringEdge> createStringDirectedGraph() {
    return new DirectedGraph<String, StringEdge>(new StringEdgeFactory());
  }

  public DirectedGraph<V, E> addEdge(V from, V to) {
    checkEdgeFacory();
    E edge = edgeFactory.createEdge(from, to);
    return addEdge(edge);
  }

  public DirectedGraph<V, E> addEdge(V from, V to, int weight) {
    checkEdgeFacory();
    E edge = edgeFactory.createEdge(from, to, weight);
    return addEdge(edge);
  }

  private void checkEdgeFacory() {
    if (edgeFactory == null) {
      throw new IllegalStateException(
          "EdgeFactory<V, E> has not been defined. Please use the 'public E addEdge(V from, V to, E edge)' method.");
    }
  }

  public DirectedGraph<V, E> addEdge(E edge) {
    addEdgeToMap(edge.getFrom(), edge.getTo(), edge, outgoingEdgesByVertex);
    addEdgeToMap(edge.getTo(), edge.getFrom(), edge, incomingEdgesByVertex);
    vertices.add(edge.getFrom());
    vertices.add(edge.getTo());
    return this;
  }

  private void addEdgeToMap(V vertexKey1, V vertexKey2, E edge, Map<V, Map<V, E>> edgesByVertex) {
    Map<V, E> edges = edgesByVertex.get(vertexKey1);
    if (edges == null) {
      edges = new HashMap<V, E>();
      edgesByVertex.put(vertexKey1, edges);
    }
    if (edges.containsKey(vertexKey2)) {
      throw new IllegalStateException("The graph already contains the edge : " + edge);
    }
    edges.put(vertexKey2, edge);
  }

  public E getEdge(V from, V to) {
    Map<V, E> outgoingEdgesFrom = outgoingEdgesByVertex.get(from);
    if (outgoingEdgesFrom == null) {
      return null;
    } else {
      return outgoingEdgesFrom.get(to);
    }
  }

  public boolean hasEdge(V from, V to) {
    Map<V, E> outgoingEdges = outgoingEdgesByVertex.get(from);
    if (outgoingEdges == null) {
      return false;
    }
    return outgoingEdges.containsKey(to);
  }

  public void addVertex(V vertex) {
    vertices.add(vertex);
  }

  public void addVertices(Collection<V> vertices) {
    for (V vertex : vertices) {
      addVertex(vertex);
    }
  }

  public Set<V> getVertices() {
    return vertices;
  }

  public List<E> getEdges(Collection<V> vertices) {
    List<E> result = new ArrayList<E>();
    for (V vertice : vertices) {
      Collection<E> outgoingEdges = getOutgoingEdges(vertice);
      if (outgoingEdges != null) {
        result.addAll(outgoingEdges);
      }
    }
    return result;
  }

  public Collection<E> getOutgoingEdges(V from) {
    Map<V, E> outgoingEdges = outgoingEdgesByVertex.get(from);
    if (outgoingEdges == null) {
      return new HashSet<E>();
    }
    return outgoingEdges.values();
  }

  public Collection<E> getIncomingEdges(V to) {
    Map<V, E> incomingEdges = incomingEdgesByVertex.get(to);
    if (incomingEdges == null) {
      return new HashSet<E>();
    }
    return incomingEdges.values();
  }
}
