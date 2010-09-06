/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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
package org.sonar.graph;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CycleTest {

  private Edge[] AB_Cycle = { new StringEdge("A", "B"), new StringEdge("B", "A") };
  private Edge[] BA_Cycle = { new StringEdge("B", "A"), new StringEdge("A", "B") };
  private Edge[] ABC_Cycle = { new StringEdge("A", "B"), new StringEdge("B", "C"), new StringEdge("C", "A") };
  private Edge[] HIJ_Cycle = { new StringEdge("H", "I"), new StringEdge("I", "J"), new StringEdge("J", "H") };
  private Edge[] ABCD_Cycle = { new StringEdge("A", "B"), new StringEdge("B", "C"), new StringEdge("C", "D"), new StringEdge("D", "A") };
  private Edge[] BCDA_Cycle = { new StringEdge("B", "C"), new StringEdge("C", "D"), new StringEdge("D", "A"), new StringEdge("A", "B"), };

  @Test
  public void testHashCode() {
    assertTrue(new Cycle(Arrays.asList(AB_Cycle)).hashCode() == new Cycle(Arrays.asList(BA_Cycle)).hashCode());
    assertTrue(new Cycle(Arrays.asList(BCDA_Cycle)).hashCode() == new Cycle(Arrays.asList(ABCD_Cycle)).hashCode());
    assertFalse(new Cycle(Arrays.asList(AB_Cycle)).hashCode() == new Cycle(Arrays.asList(ABC_Cycle)).hashCode());
  }

  @Test
  public void testContains() {
    assertTrue(new Cycle(Arrays.asList(ABCD_Cycle)).contains(new StringEdge("B", "C")));
  }

  @Test
  public void testEqualsObject() {
    assertEquals(new Cycle(Arrays.asList(AB_Cycle)), new Cycle(Arrays.asList(BA_Cycle)));
    assertEquals(new Cycle(Arrays.asList(BCDA_Cycle)), new Cycle(Arrays.asList(ABCD_Cycle)));
    assertFalse(new Cycle(Arrays.asList(BCDA_Cycle)).equals(new Cycle(Arrays.asList(AB_Cycle))));
    assertFalse(new Cycle(Arrays.asList(ABC_Cycle)).equals(new Cycle(Arrays.asList(HIJ_Cycle))));
  }
}
