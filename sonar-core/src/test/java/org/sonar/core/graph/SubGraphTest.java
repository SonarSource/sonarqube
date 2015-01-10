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
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.GraphHelper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SubGraphTest {

  @Test
  public void should_extract_graph() {
    TinkerGraph graph = new TinkerGraph();
    Vertex a = GraphHelper.addVertex(graph, null, "key", "a");
    Vertex b = GraphHelper.addVertex(graph, null, "key", "b");
    Vertex c = GraphHelper.addVertex(graph, null, "key", "c");
    Vertex d = GraphHelper.addVertex(graph, null, "key", "d");
    Vertex e = GraphHelper.addVertex(graph, null, "key", "e");

    Edge ab = GraphHelper.addEdge(graph, null, a, b, "uses");
    Edge bc = GraphHelper.addEdge(graph, null, b, c, "inherits");
    Edge ad = GraphHelper.addEdge(graph, null, a, d, "uses");
    Edge de = GraphHelper.addEdge(graph, null, d, e, "implements");

    // a -uses-> b -inherits -> c
    // a -uses-> d -implements-> e

    Graph sub = SubGraph.extract(a, EdgePath.create(Direction.OUT, "uses", Direction.OUT, "implements"));

    // a -uses-> b
    // a -uses-> d -implements-> e
    assertThat(sub.getVertices()).hasSize(4);
    assertThat(sub.getVertex(a.getId()).getProperty("key")).isEqualTo("a");
    assertThat(sub.getVertex(b.getId()).getProperty("key")).isEqualTo("b");
    assertThat(sub.getVertex(c.getId())).isNull();
    assertThat(sub.getVertex(d.getId()).getProperty("key")).isEqualTo("d");
    assertThat(sub.getVertex(e.getId()).getProperty("key")).isEqualTo("e");

    assertThat(sub.getEdges()).hasSize(3);
    assertThat(sub.getEdge(ab.getId()).getLabel()).isEqualTo("uses");
    assertThat(sub.getEdge(ab.getId()).toString()).isEqualTo(ab.toString());
    assertThat(sub.getEdge(bc.getId())).isNull();
    assertThat(sub.getEdge(ad.getId()).toString()).isEqualTo(ad.toString());
    assertThat(sub.getEdge(de.getId()).toString()).isEqualTo(de.toString());
  }

  @Test
  public void should_extract_cyclic_graph() {
    TinkerGraph graph = new TinkerGraph();
    Vertex a = GraphHelper.addVertex(graph, null, "key", "a");
    Vertex b = GraphHelper.addVertex(graph, null, "key", "b");
    Vertex c = GraphHelper.addVertex(graph, null, "key", "c");
    Vertex d = GraphHelper.addVertex(graph, null, "key", "d");
    Vertex e = GraphHelper.addVertex(graph, null, "key", "e");

    Edge ab = GraphHelper.addEdge(graph, null, a, b, "uses");
    Edge bc = GraphHelper.addEdge(graph, null, b, c, "implements");
    Edge ce = GraphHelper.addEdge(graph, null, c, e, "package");
    Edge ad = GraphHelper.addEdge(graph, null, a, d, "uses");
    Edge dc = GraphHelper.addEdge(graph, null, d, c, "implements");

    // a -uses-> b -implements-> c -package-> e
    // a -uses-> d -implements-> c -package-> e

    Graph sub = SubGraph.extract(a, EdgePath.create(Direction.OUT, "uses", Direction.OUT, "implements", Direction.OUT, "package"));

    // same graph
    assertThat(sub.getVertices()).hasSize(5);
    assertThat(sub.getEdges()).hasSize(5);
  }

  @Test
  public void should_check_edge_direction() {
    TinkerGraph graph = new TinkerGraph();
    Vertex a = GraphHelper.addVertex(graph, null, "key", "a");
    Vertex b = GraphHelper.addVertex(graph, null, "key", "b");
    Vertex c = GraphHelper.addVertex(graph, null, "key", "c");
    Vertex d = GraphHelper.addVertex(graph, null, "key", "d");
    Vertex e = GraphHelper.addVertex(graph, null, "key", "e");

    Edge ab = GraphHelper.addEdge(graph, null, a, b, "uses");
    Edge bc = GraphHelper.addEdge(graph, null, b, c, "inherits");
    Edge ad = GraphHelper.addEdge(graph, null, a, d, "uses");
    Edge de = GraphHelper.addEdge(graph, null, d, e, "implements");

    // a -uses-> b -inherits -> c
    // a -uses-> d -implements-> e

    Graph sub = SubGraph.extract(a, EdgePath.create(Direction.IN /* instead of out */, "uses", Direction.OUT, "implements"));

    assertThat(sub.getVertices()).hasSize(1);
    assertThat(sub.getVertex(a.getId())).isNotNull();
    assertThat(sub.getEdges()).isEmpty();
  }
}
