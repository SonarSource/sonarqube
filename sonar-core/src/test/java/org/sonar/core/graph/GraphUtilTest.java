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

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class GraphUtilTest {

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void uniqueAdjacent() {
    TinkerGraph graph = new TinkerGraph();
    Vertex a = graph.addVertex(null);
    Vertex b = graph.addVertex(null);
    Vertex c = graph.addVertex(null);
    graph.addEdge(null, a, b, "likes");
    graph.addEdge(null, a, c, "hates");

    assertThat(GraphUtil.singleAdjacent(a, Direction.OUT, "likes")).isEqualTo(b);
    assertThat(GraphUtil.singleAdjacent(a, Direction.OUT, "likes", "other")).isEqualTo(b);
    assertThat(GraphUtil.singleAdjacent(a, Direction.OUT, "other")).isNull();
    assertThat(GraphUtil.singleAdjacent(a, Direction.IN, "likes")).isNull();
  }

  @Test
  public void uniqueAdjacent_fail_if_multiple_adjacents() {
    thrown.expect(MultipleElementsException.class);
    thrown.expectMessage("More than one vertex is adjacent to: v[0], direction: OUT, labels: likes,hates");

    TinkerGraph graph = new TinkerGraph();
    Vertex a = graph.addVertex(null);
    Vertex b = graph.addVertex(null);
    Vertex c = graph.addVertex(null);
    graph.addEdge(null, a, b, "likes");
    graph.addEdge(null, a, c, "likes");

    GraphUtil.singleAdjacent(a, Direction.OUT, "likes", "hates");
  }

  @Test
  public void should_extract_single_element() {
    TinkerGraph graph = new TinkerGraph();
    Vertex a = graph.addVertex(null);

    Vertex single = GraphUtil.single(graph.getVertices());
    assertThat(single).isSameAs(a);
  }

  @Test
  public void should_extract_null() {
    TinkerGraph graph = new TinkerGraph();
    Vertex single = GraphUtil.single(graph.getVertices());
    assertThat(single).isNull();
  }

  @Test
    public void should_fail_to_extract_single_element() {
      thrown.expect(MultipleElementsException.class);
      thrown.expectMessage("More than one element");

      TinkerGraph graph = new TinkerGraph();
      graph.addVertex(null);
      graph.addVertex(null);
      graph.addVertex(null);

      GraphUtil.single(graph.getVertices());
    }
}
