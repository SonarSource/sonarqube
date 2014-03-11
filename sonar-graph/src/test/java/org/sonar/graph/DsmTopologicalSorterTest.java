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

import static org.junit.Assert.assertEquals;

public class DsmTopologicalSorterTest {

  @Test
  public void sortAcyclicGraph() {
    StringPrintWriter textDsm = new StringPrintWriter();
    textDsm.println("  | A | B | C | D | E |");
    textDsm.println("A |   |   |   |   |   |");
    textDsm.println("B | 3 |   | 4 |   |   |");
    textDsm.println("C | 1 |   |   |   |   |");
    textDsm.println("D |   | 2 |   |   | 1 |");
    textDsm.println("E |   | 5 |   |   |   |");

    Dsm<String> dsm = DsmScanner.scan(textDsm.toString());
    DsmTopologicalSorter.sort(dsm);

    StringPrintWriter expectedTextDsm = new StringPrintWriter();
    expectedTextDsm.println("  | A | C | B | E | D |");
    expectedTextDsm.println("A |   |   |   |   |   |");
    expectedTextDsm.println("C | 1 |   |   |   |   |");
    expectedTextDsm.println("B | 3 | 4 |   |   |   |");
    expectedTextDsm.println("E |   |   | 5 |   |   |");
    expectedTextDsm.println("D |   |   | 2 | 1 |   |");

    Dsm<String> expectedDsm = DsmScanner.scan(expectedTextDsm.toString());
    DsmTopologicalSorter.sort(expectedDsm);

    assertEquals(DsmPrinter.print(dsm), DsmPrinter.print(expectedDsm));
  }

  @Test(expected = IllegalStateException.class)
  public void sortCyclicGraph() {
    StringPrintWriter textDsm = new StringPrintWriter();
    textDsm.println("  | A | B | C | D |");
    textDsm.println("A |   |   |   |   |");
    textDsm.println("B | 3 |   | 4 |   |");
    textDsm.println("C | 1 | 1 |   |   |");
    textDsm.println("D |   | 2 |   |   |");

    Dsm<String> dsm = DsmScanner.scan(textDsm.toString());
    DsmTopologicalSorter.sort(dsm);
  }

  @Test
  public void sortCyclicGraphWithManuallyFlaggedFeedbackEdges() {
    StringPrintWriter textDsm = new StringPrintWriter();
    textDsm.println("  | A | B | C | D |");
    textDsm.println("A |   |   |   |   |");
    textDsm.println("B | 3 |   | 4 |   |");
    textDsm.println("C | 1 | 1*|   |   |");
    textDsm.println("D |   | 2 |   |   |");

    Dsm<String> dsm = DsmScanner.scan(textDsm.toString());
    DsmTopologicalSorter.sort(dsm);

    StringPrintWriter expectedDsm = new StringPrintWriter();
    expectedDsm.println("  | A | C | B | D |");
    expectedDsm.println("A |   |   |   |   |");
    expectedDsm.println("C | 1 |   | 1*|   |");
    expectedDsm.println("B | 3 | 4 |   |   |");
    expectedDsm.println("D |   |   | 2 |   |");

    assertEquals(expectedDsm.toString(), DsmPrinter.print(dsm));
  }

  @Test
  public void sortCyclicGraphWithFlaggedFeedbackEdges() {
    DirectedGraph<String, StringEdge> dcg = DirectedGraph.createStringDirectedGraph();
    dcg.addEdge("A", "B", 3).addEdge("B", "A", 1);
    CycleDetector<String> cycleDetector = new CycleDetector<String>(dcg);
    cycleDetector.detectCycles();

    MinimumFeedbackEdgeSetSolver solver = new MinimumFeedbackEdgeSetSolver(cycleDetector.getCycles());

    Dsm<String> dsm = new Dsm<String>(dcg, solver.getEdges());
    DsmTopologicalSorter.sort(dsm);

    StringPrintWriter expectedDsm = new StringPrintWriter();
    expectedDsm.println("  | A | B |");
    expectedDsm.println("A |   | 1*|");
    expectedDsm.println("B | 3 |   |");

    assertEquals(expectedDsm.toString(), DsmPrinter.print(dsm));
  }

}
