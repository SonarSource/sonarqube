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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class CycleDetectorTest {

  @Test
  public void testIsAcyclicGraph() {
    DirectedGraph<String, StringEdge> dag = DirectedGraph.createStringDirectedGraph();
    dag.addEdge("A", "B").addEdge("B", "C").addEdge("C", "D");
    dag.addEdge("B", "D");
    dag.addEdge("A", "D");

    CycleDetector<String> cycleDetector = new CycleDetector<String>(dag);
    cycleDetector.detectCycles();
    assertThat(cycleDetector.isAcyclicGraph()).isTrue();
  }

  @Test
  public void testIsNotAcyclicGraph() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B").addEdge("B", "C").addEdge("C", "A");

    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg);
    cycleDetector.detectCycles();
    assertThat(cycleDetector.isAcyclicGraph()).isFalse();
  }

  @Test
  public void testGetCyclesWithMultipleCycles() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B").addEdge("B", "C").addEdge("C", "D").addEdge("D", "A");
    dcg.addEdge("C", "A");
    dcg.addEdge("B", "A");
    dcg.addEdge("A", "E").addEdge("E", "C");
    dcg.addEdge("E", "D");
    dcg.addEdge("E", "F");
    dcg.addEdge("F", "C");

    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg);
    cycleDetector.detectCycles();
    assertThat(cycleDetector.getCycles()).hasSize(8);
    assertThat(cycleDetector.getSearchCyclesCalls()).isEqualTo(11);
  }

  @Test
  public void testMaxSearchDepthOption() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B").addEdge("B", "C").addEdge("C", "D").addEdge("D", "A");
    dcg.addEdge("C", "A");
    dcg.addEdge("B", "A");

    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg);
    cycleDetector.detectCyclesWithMaxSearchDepth(3);
    assertThat(cycleDetector.getCycles()).hasSize(2);

    cycleDetector = new CycleDetector<String>(dcg);
    cycleDetector.detectCyclesWithMaxSearchDepth(2);
    assertThat(cycleDetector.getCycles()).hasSize(1);
  }

  @Test
  public void testExcludeEdgesFromSearch() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B").addEdge("B", "C").addEdge("C", "D").addEdge("D", "A");
    dcg.addEdge("C", "A");
    dcg.addEdge("B", "A");

    Set<Edge> excludedEdges = new HashSet<Edge>();
    excludedEdges.add(dcg.getEdge("C", "A"));
    excludedEdges.add(dcg.getEdge("B", "A"));

    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg, excludedEdges);
    cycleDetector.detectCycles();
    assertThat(cycleDetector.getCycles()).hasSize(1);
  }

  @Test
  public void testGetCyclesWithOneCycle() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B").addEdge("B", "C").addEdge("C", "D").addEdge("D", "E");
    dcg.addEdge("B", "A");

    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg);
    cycleDetector.detectCycles();
    assertThat(cycleDetector.getCycles()).hasSize(1);
    Cycle cycle = cycleDetector.getCycles().iterator().next();
    assertThat(cycle.size()).isEqualTo(2);
    assertThat(cycle.contains(new StringEdge("A", "B"))).isTrue();
    assertThat(cycle.contains(new StringEdge("B", "A"))).isTrue();
  }

  @Test
  public void getCyclesInLimitedSetOfVertices() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B").addEdge("B", "C").addEdge("C", "A");

    // C must not be used to find cycles
    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg, Arrays.asList("A", "B"));
    cycleDetector.detectCycles();
    assertThat(cycleDetector.getCycles()).isEmpty();

    // C is used to find cycles
    cycleDetector = new CycleDetector<String>(dcg, Arrays.asList("A", "B", "C"));
    cycleDetector.detectCycles();
    assertThat(cycleDetector.getCycles().size()).isEqualTo(1);
  }

  @Test(expected = IllegalStateException.class)
  public void testExecutingTwoCycleDetectionsOnSameObject() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B").addEdge("B", "C").addEdge("C", "A");

    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg);
    cycleDetector.detectCycles();
    cycleDetector.detectCycles();
  }

  @Test
  public void testDetectCyclesWithUpperLimit() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B").addEdge("B", "C").addEdge("C", "D").addEdge("D", "A");
    dcg.addEdge("B", "A");

    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg);
    assertThat(cycleDetector.detectCyclesWithUpperLimit(1)).hasSize(1);
  }
}
