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

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class IncrementalCyclesAndFESSolverTest {

  @Test
  public void testSimpleCase() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B").addEdge("B", "C").addEdge("C", "D").addEdge("D", "A");
    dcg.addEdge("C", "A");
    dcg.addEdge("B", "A");
    dcg.addEdge("A", "E").addEdge("E", "C");
    dcg.addEdge("E", "D");
    dcg.addEdge("E", "F");
    dcg.addEdge("F", "C");

    IncrementalCyclesAndFESSolver<String> cyclesAndFESSolver = new IncrementalCyclesAndFESSolver<String>(dcg, dcg.getVertices(), 3,
        Integer.MAX_VALUE);
    assertThat(cyclesAndFESSolver.getCycles().size(), is(4));
    assertThat(cyclesAndFESSolver.getFeedbackEdgeSet().size(), is(2));
    assertThat(cyclesAndFESSolver.getWeightOfFeedbackEdgeSet(), is(2));
  }

  @Test
  public void testWithNoCycleUnderTheThreshold() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B").addEdge("B", "C").addEdge("C", "D").addEdge("D", "A");

    IncrementalCyclesAndFESSolver<String> cyclesAndFESSolver = new IncrementalCyclesAndFESSolver<String>(dcg, dcg.getVertices(), 2,
        Integer.MAX_VALUE);
    assertThat(cyclesAndFESSolver.getCycles().size(), is(1));
    assertThat(cyclesAndFESSolver.getFeedbackEdgeSet().size(), is(1));
    assertThat(cyclesAndFESSolver.getWeightOfFeedbackEdgeSet(), is(1));
  }

  @Test
  public void testBothMaxSearchDepthAtFirstAndMaxCyclesToFoundByIteration() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B").addEdge("B", "C").addEdge("C", "D").addEdge("D", "A");
    dcg.addEdge("E", "F").addEdge("F", "G").addEdge("G", "E");
    dcg.addEdge("H", "I").addEdge("I", "H");

    IncrementalCyclesAndFESSolver<String> cyclesAndFESSolver = new IncrementalCyclesAndFESSolver<String>(dcg, dcg.getVertices(), 2, 1);
    assertThat(cyclesAndFESSolver.getCycles().size(), is(3));
    assertThat(cyclesAndFESSolver.getIterations(), is(4));
    cyclesAndFESSolver.getFeedbackEdgeSet();
  }

}
