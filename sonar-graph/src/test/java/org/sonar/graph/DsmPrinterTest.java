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

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;

public class DsmPrinterTest {

  private Dsm<String> dsm;

  @Before
  public void init() {
    DirectedGraph<String, StringEdge> graph = DirectedGraph.createStringDirectedGraph();
    graph.addEdge("A", "B", 3).addEdge("A", "C", 2);
    graph.addEdge("C", "B", 4).addEdge("C", "A", 1);
    HashSet<Edge> feedbackEdges = Sets.<Edge>newHashSet(graph.getEdge("C", "A"));
    dsm = new Dsm<String>(graph, feedbackEdges);
    DsmManualSorter.sort(dsm, "A", "C", "B");
  }

  @Test
  public void testPrintDsm() {
    StringPrintWriter expectedResult = new StringPrintWriter();
    expectedResult.println("  | A | C | B |");
    expectedResult.println("A |   | 1*|   |");
    expectedResult.println("C | 2 |   |   |");
    expectedResult.println("B | 3 | 4 |   |");

    assertEquals(expectedResult.toString(), DsmPrinter.print(dsm));
  }

  @Test
  public void testPrintDsmWithoutColumnHeaders() {
    StringPrintWriter expectedResult = new StringPrintWriter();
    expectedResult.println("A |   | 1*|   |");
    expectedResult.println("C | 2 |   |   |");
    expectedResult.println("B | 3 | 4 |   |");

    assertEquals(expectedResult.toString(), DsmPrinter.print(dsm, false));
  }
}
