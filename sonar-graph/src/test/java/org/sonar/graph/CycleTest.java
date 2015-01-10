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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CycleTest {
  static List<Edge> AB_BA = list(new StringEdge("A", "B"), new StringEdge("B", "A"));
  static List<Edge> BA_AB = list(new StringEdge("B", "A"), new StringEdge("A", "B"));
  static List<Edge> AB_BC_CA = list(new StringEdge("A", "B"), new StringEdge("B", "C"), new StringEdge("C", "A"));
  static List<Edge> HI_IJ_JH = list(new StringEdge("H", "I"), new StringEdge("I", "J"), new StringEdge("J", "H"));
  static List<Edge> AB_BC_CD_DA = list(new StringEdge("A", "B"), new StringEdge("B", "C"), new StringEdge("C", "D"), new StringEdge("D", "A"));
  static List<Edge> BC_CD_DA_AB = list(new StringEdge("B", "C"), new StringEdge("C", "D"), new StringEdge("D", "A"), new StringEdge("A", "B"));

  @Test
  public void testHashCode() {
    assertThat(new Cycle(AB_BA).hashCode()).isEqualTo(new Cycle(BA_AB).hashCode());
    assertThat(new Cycle(BC_CD_DA_AB).hashCode()).isEqualTo(new Cycle(AB_BC_CD_DA).hashCode());
    assertThat(new Cycle(AB_BA).hashCode()).isNotEqualTo(new Cycle(AB_BC_CA).hashCode());
  }

  @Test
  public void testContains() {
    assertThat(new Cycle(AB_BC_CD_DA).contains(new StringEdge("B", "C"))).isTrue();
  }

  @Test
  public void testEqualsObject() {
    assertThat(new Cycle(AB_BA)).isEqualTo(new Cycle(BA_AB));
    assertThat(new Cycle(BC_CD_DA_AB)).isEqualTo(new Cycle(AB_BC_CD_DA));
  }

  @Test
  public void testNotEqualsObject() {
    assertThat(new Cycle(BC_CD_DA_AB)).isNotEqualTo(new Cycle(AB_BA));
    assertThat(new Cycle(AB_BC_CA)).isNotEqualTo(new Cycle(HI_IJ_JH));
  }

  static List<Edge> list(StringEdge... edges) {
    return Arrays.<Edge> asList(edges);
  }
}
