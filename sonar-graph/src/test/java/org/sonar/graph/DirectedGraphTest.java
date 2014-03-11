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

import org.junit.Before;
import org.junit.Test;
import org.sonar.graph.DirectedGraph;
import org.sonar.graph.StringEdge;

import java.util.Arrays;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

public class DirectedGraphTest {

  private DirectedGraph<String, StringEdge> graph;

  @Before
  public void init() {
    graph = DirectedGraph.createStringDirectedGraph();
    graph.addEdge("A", "B");
    graph.addEdge("A", "C");
    graph.addEdge("B", "C");
  }

  @Test
  public void testGetVertices() {
    assertThat(graph.getVertices(), hasItems("A", "B"));
    assertThat(graph.getVertices().size(), is(3));
  }

  @Test
  public void testGetEdge() {
    assertNull(graph.getEdge("A", "T"));
    graph.addEdge("A", "T", 5);
    assertThat(graph.getEdge("A", "T").getWeight(), is(5));
  }

  @Test(expected = IllegalStateException.class)
  public void testAddEdgeThrowsException() {
    graph.addEdge("B", "C");
  }

  @Test
  public void testAddEdgeWithWeight() {
    graph.addEdge("D", "B", 4);
    assertThat(graph.getOutgoingEdges("D").iterator().next().getWeight(), is(4));
  }

  @Test
  public void testGetOutgoingEdges() {
    assertThat(graph.getOutgoingEdges("A"), hasItem(new StringEdge("A", "B")));
    assertThat(graph.getOutgoingEdges("A").size(), is(2));
    assertThat(graph.getOutgoingEdges("B"), hasItem(new StringEdge("B", "C")));
  }

  @Test
  public void testGetIncomingEdges() {
    assertThat(graph.getIncomingEdges("C"), hasItem(new StringEdge("A", "C")));
    assertThat(graph.getIncomingEdges("A").size(), is(0));
  }

  @Test
  public void testGetEdges() {
    assertTrue(graph.getEdges(Arrays.asList("A")).containsAll(Arrays.asList(new StringEdge("A", "B"), new StringEdge("A", "C"))));
    assertTrue(graph.getEdges(Arrays.asList("A", "B")).containsAll(Arrays.asList(new StringEdge("A", "B"), new StringEdge("A", "C"), new StringEdge("B", "C"))));
  }

  @Test
  public void testHasEdge() {
    assertTrue(graph.hasEdge("A", "B"));
    assertFalse(graph.hasEdge("C", "A"));
  }

  @Test
  public void testAddVertex() {
    graph.addVertex("X");
    assertThat(graph.getVertices(), hasItem("X"));
    assertThat(graph.getOutgoingEdges("X").size(), is(0));
  }

  @Test
  public void testAddVertices() {
    String[] vertices = { "X", "Y", "Z" };
    graph.addVertices(Arrays.asList(vertices));
    assertThat(graph.getVertices(), hasItems("X", "Y", "Z"));
  }
}
