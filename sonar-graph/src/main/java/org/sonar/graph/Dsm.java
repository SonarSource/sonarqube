/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class Dsm<V> {

  private V[] vertices;
  private DsmCell[][] cells;
  private int dimension;
  private DirectedGraphAccessor<V, ? extends Edge<V>> graph;

  public Dsm(DirectedGraphAccessor<V, ? extends Edge<V>> graph, Collection<V> vertices, Set<Edge> feedbackEdges) {
    this.graph = graph;
    this.dimension = vertices.size();
    this.cells = new DsmCell[dimension][dimension];
    initVertices(vertices);
    initCells(feedbackEdges);
  }

  public Dsm(DirectedGraphAccessor<V, ? extends Edge<V>> acyclicGraph, Set<Edge> feedbackEdges) {
    this(acyclicGraph, acyclicGraph.getVertices(), feedbackEdges);
  }

  public Dsm(DirectedGraphAccessor<V, ? extends Edge<V>> acyclicGraph) {
    this(acyclicGraph, acyclicGraph.getVertices(), Collections.<Edge>emptySet());
  }

  private void initCells(Set<Edge> feedbackEdges) {
    for (int x = 0; x < dimension; x++) {
      for (int y = 0; y < dimension; y++) {
        V from = vertices[x];
        V to = vertices[y];

        Edge<V> edge = graph.getEdge(from, to);
        boolean isFeedbackEdge = edge != null && feedbackEdges.contains(edge);
        DsmCell cell = new DsmCell(edge, isFeedbackEdge);
        cells[x][y] = cell;
      }
    }
  }

  private void initVertices(Collection<V> verticesCol) {
    this.vertices = (V[]) new Object[dimension];
    int i = 0;
    for (V vertex : verticesCol) {
      vertices[i] = vertex;
      i++;
    }
  }

  public V getVertex(int rowIndex) {
    return vertices[rowIndex];
  }

  public int getDimension() {
    return dimension;
  }

  public void permute(int fromIndex, int toIndex) {
    if (fromIndex != toIndex) {
      checkIndicesBoudaries(fromIndex, toIndex);
      permuteVertice(fromIndex, toIndex);
      permuteColumns(fromIndex, toIndex);
      permuteRows(fromIndex, toIndex);
    }
  }

  private void checkIndicesBoudaries(int... indices) {
    for (int index : indices) {
      if (index < 0 || index >= dimension) {
        StringBuilder builder = new StringBuilder("DSM contains the following vertices : ");
        for (V vertex : vertices) {
          builder.append(vertex.toString()).append(" | ");
        }
        builder.append(". Trying to reach index ").append(index);
        throw new ArrayIndexOutOfBoundsException(builder.toString());
      }
    }
  }

  private void permuteVertice(int fromIndex, int toIndex) {
    V fromVertex = vertices[fromIndex];
    V toVertex = vertices[toIndex];
    vertices[fromIndex] = toVertex;
    vertices[toIndex] = fromVertex;
  }

  private void permuteRows(int fromYIndex, int toYIndex) {
    for (int x = 0; x < dimension; x++) {
      permuteCells(x, fromYIndex, x, toYIndex);
    }
  }

  private void permuteColumns(int fromXIndex, int toXIndex) {
    for (int y = 0; y < dimension; y++) {
      permuteCells(fromXIndex, y, toXIndex, y);
    }
  }

  private void permuteCells(int fromXIndex, int fromYIndex, int toXIndex, int toYIndex) {
    DsmCell fromCell = cells[fromXIndex][fromYIndex];
    DsmCell toCell = cells[toXIndex][toYIndex];
    cells[toXIndex][toYIndex] = fromCell;
    cells[fromXIndex][fromYIndex] = toCell;
  }

  public int getNumberOfIncomingEdges(int y, int from, int to) {
    int incomingEdges = 0;
    for (int x = from; x <= to; x++) {
      DsmCell cell = cells[x][y];
      if (cell.getWeight() != 0 && !cell.isFeedbackEdge()) {
        incomingEdges++;
      }
    }
    return incomingEdges;
  }

  public int getNumberOfOutgoingEdges(int x, int from, int to) {
    int outgoingEdges = 0;
    for (int y = from; y <= to; y++) {
      DsmCell cell = cells[x][y];
      if (cell.getWeight() != 0 && !cell.isFeedbackEdge()) {
        outgoingEdges++;
      }
    }
    return outgoingEdges;
  }

  public DsmCell getCell(int x, int y) {
    return cells[x][y];
  }

  public V[] getVertices() {
    V[] verticesCopy = (V[]) new Object[vertices.length];
    System.arraycopy(vertices, 0, verticesCopy, 0, vertices.length);
    return verticesCopy;
  }
}
