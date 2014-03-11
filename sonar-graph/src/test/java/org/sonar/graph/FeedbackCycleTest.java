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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FeedbackCycleTest {

  private Edge[] AB_Edges = { new StringEdge("A", "B"), new StringEdge("B", "A") };
  private Edge[] ABC_Edges = { new StringEdge("A", "B"), new StringEdge("B", "C"), new StringEdge("C", "A") };
  private Edge[] BCDA_Edges = { new StringEdge("B", "C"), new StringEdge("C", "D"), new StringEdge("D", "A"), new StringEdge("A", "B"), };
  private Edge[] EF_Edges = { new StringEdge("E", "F"), new StringEdge("F", "E") };
  private Edge[] GHIJ_Edges = { new StringEdge("G", "H"), new StringEdge("H", "I"), new StringEdge("I", "J"), new StringEdge("J", "G") };
  private Edge[] XYZW_Edges = { new StringEdge("X", "Y"), new StringEdge("Y", "Z"), new StringEdge("Z", "W"), new StringEdge("W", "X") };

  private Cycle AB_Cycle = new Cycle(Arrays.asList(AB_Edges));
  private Cycle ABC_Cycle = new Cycle(Arrays.asList(ABC_Edges));
  private Cycle BCDA_Cycle = new Cycle(Arrays.asList(BCDA_Edges));
  private Cycle EF_Cycle = new Cycle(Arrays.asList(EF_Edges));
  private Cycle GHIJ_Cycle = new Cycle(Arrays.asList(GHIJ_Edges));
  private Cycle XYZW_Cycle = new Cycle(Arrays.asList(XYZW_Edges));

  @Test
  public void testBuildFeedbackCycles() {
    Set<Cycle> cycles = new HashSet<Cycle>();
    cycles.add(AB_Cycle);
    cycles.add(ABC_Cycle);
    cycles.add(BCDA_Cycle);
    cycles.add(EF_Cycle);
    cycles.add(GHIJ_Cycle);
    cycles.add(XYZW_Cycle);

    List<FeedbackCycle> feedbackCycles = FeedbackCycle.buildFeedbackCycles(cycles);
    assertEquals(6, feedbackCycles.size());
    assertEquals(EF_Cycle, feedbackCycles.get(0).getCycle());
    assertEquals(AB_Cycle, feedbackCycles.get(1).getCycle());
    assertEquals(GHIJ_Cycle, feedbackCycles.get(2).getCycle());
    assertEquals(XYZW_Cycle, feedbackCycles.get(3).getCycle());
    assertEquals(ABC_Cycle, feedbackCycles.get(4).getCycle());
    assertEquals(BCDA_Cycle, feedbackCycles.get(5).getCycle());
  }
}
