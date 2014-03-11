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

import java.util.Set;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MinimumFeedbackEdgeSetSolverTest {

  @Test
  public void testGetFeedbackEdgesOnSimpleLoop() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B", 3).addEdge("B", "A", 1);
    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg);
    cycleDetector.detectCycles();

    MinimumFeedbackEdgeSetSolver solver = new MinimumFeedbackEdgeSetSolver(cycleDetector.getCycles());
    Set<Edge> feedbackEdges = solver.getEdges();
    assertThat(feedbackEdges.size(), is(1));
  }

  @Test
  public void testFlagFeedbackEdgesOnSimpleLoop() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B", 3).addEdge("B", "A", 1);
    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg);
    cycleDetector.detectCycles();

    MinimumFeedbackEdgeSetSolver solver = new MinimumFeedbackEdgeSetSolver(cycleDetector.getCycles());
    Set<Edge> feedbackEdges = solver.getEdges();
    assertTrue(feedbackEdges.contains(dcg.getEdge("B", "A")));
  }

  @Test
  public void testGetFeedbackEdgesOnComplexGraph() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B", 7).addEdge("B", "C", 3).addEdge("C", "D", 1).addEdge("D", "A", 3);
    dcg.addEdge("B", "A", 12);
    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg);
    cycleDetector.detectCycles();

    MinimumFeedbackEdgeSetSolver solver = new MinimumFeedbackEdgeSetSolver(cycleDetector.getCycles());
    Set<Edge> feedbackEdges = solver.getEdges();
    assertThat(feedbackEdges.size(), is(1));

    assertTrue(feedbackEdges.contains(dcg.getEdge("A", "B")));
  }

  @Test
  public void testFlagFeedbackEdgesOnUnrelatedCycles() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B", 7).addEdge("B", "C", 3).addEdge("C", "A", 2);
    dcg.addEdge("D", "E", 3).addEdge("E", "D", 5);
    dcg.addEdge("F", "G", 1).addEdge("G", "H", 4).addEdge("H", "F", 7);

    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg);
    cycleDetector.detectCycles();

    MinimumFeedbackEdgeSetSolver solver = new MinimumFeedbackEdgeSetSolver(cycleDetector.getCycles());
    Set<Edge> feedbackEdges = solver.getEdges();

    assertThat(feedbackEdges.size(), is(3));

    assertTrue(feedbackEdges.contains(dcg.getEdge("C", "A")));
    assertTrue(feedbackEdges.contains(dcg.getEdge("D", "E")));
    assertTrue(feedbackEdges.contains(dcg.getEdge("F", "G")));
  }

  @Test
  public void testApproximateMinimumFeedbackEdges() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B", 5).addEdge("B", "C", 9).addEdge("C", "A", 1);
    dcg.addEdge("D", "B", 5).addEdge("C", "D", 7);
    dcg.addEdge("F", "B", 5).addEdge("C", "F", 4);
    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg);
    cycleDetector.detectCycles();

    MinimumFeedbackEdgeSetSolver minimumSolver = new MinimumFeedbackEdgeSetSolver(cycleDetector.getCycles());
    assertThat(minimumSolver.getEdges().size(), is(1));
    assertTrue(minimumSolver.getEdges().contains(dcg.getEdge("B", "C")));

    MinimumFeedbackEdgeSetSolver approximateSolver = new MinimumFeedbackEdgeSetSolver(cycleDetector.getCycles(), 2);
    assertThat(approximateSolver.getEdges().size(), is(2));
    assertTrue(approximateSolver.getEdges().contains(dcg.getEdge("B", "C")));
    assertTrue(approximateSolver.getEdges().contains(dcg.getEdge("C", "A")));
  }

}
