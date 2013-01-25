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
import com.tinkerpop.gremlin.java.GremlinPipeline;
import org.junit.Test;
import org.sonar.core.graph.GraphUtil;

import static org.fest.assertions.Assertions.assertThat;

public class GraphUtilTest {
  @Test
  public void subGraph() {
    TinkerGraph graph = new TinkerGraph();
    Vertex a = graph.addVertex("1");
    Vertex b = graph.addVertex("2");
    Vertex c = graph.addVertex("3");
    graph.addEdge("4", a, b, "likes");
    graph.addEdge("5", b, c, "has");

    TinkerGraph subGraph = new TinkerGraph();
    GremlinPipeline pipeline = new GremlinPipeline(a).outE("likes").inV().path();
    GraphUtil.subGraph(pipeline, subGraph);

    assertThat(subGraph.getVertices()).hasSize(2);
    assertThat(subGraph.getEdges()).hasSize(1);
  }
}
