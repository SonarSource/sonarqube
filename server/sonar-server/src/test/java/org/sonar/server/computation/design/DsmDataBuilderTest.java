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

package org.sonar.server.computation.design;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.sonar.graph.DirectedGraph;
import org.sonar.graph.Dsm;
import org.sonar.server.computation.step.MockComponentUuidsCache;
import org.sonar.server.design.db.DsmDb;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

public class DsmDataBuilderTest {

  @Test
  public void build_empty_dsm_db() throws Exception {
    DirectedGraph<Integer, FileDependency> graph = new DirectedGraph<>();
    DsmDb.Data result = DsmDataBuilder.build(new Dsm<>(graph), new MockComponentUuidsCache(Collections.<Integer, String>emptyMap()));
    assertThat(result.getUuidCount()).isEqualTo(0);
    assertThat(result.getCellCount()).isEqualTo(0);
  }

  /**
   * / | A | B | C |
   * A |   | 1 |   |
   * B | 2 |   |   |
   * C | 4 |   |   |
   */
  @Test
  public void build_dsm_db() throws Exception {
    DirectedGraph<Integer, FileDependency> graph = new DirectedGraph<>();
    graph.addVertex(1);
    graph.addVertex(2);
    graph.addVertex(3);
    graph.addEdge(new FileDependency(1, 2, 1));
    graph.addEdge(new FileDependency(2, 1, 2));
    graph.addEdge(new FileDependency(3, 1, 4));

    DsmDb.Data result = DsmDataBuilder.build(new Dsm<>(graph), new MockComponentUuidsCache(ImmutableMap.of(1, "A", 2, "B", 3, "C")));
    assertThat(result.getUuidCount()).isEqualTo(3);
    assertThat(result.getCellCount()).isEqualTo(3);
    assertThat(result.getCell(0).getOffset()).isEqualTo(3);
    assertThat(result.getCell(0).getWeight()).isEqualTo(2);
    assertThat(result.getCell(1).getOffset()).isEqualTo(6);
    assertThat(result.getCell(1).getWeight()).isEqualTo(4);
    assertThat(result.getCell(2).getOffset()).isEqualTo(1);
    assertThat(result.getCell(2).getWeight()).isEqualTo(1);
  }

  @Test
  public void fail_on_unknown_uuid() throws Exception {
    DirectedGraph<Integer, FileDependency> graph = new DirectedGraph<>();
    graph.addVertex(1);
    graph.addVertex(2);
    graph.addEdge(new FileDependency(1, 2, 1));

    try {
      DsmDataBuilder.build(new Dsm<>(graph), new MockComponentUuidsCache(ImmutableMap.of(1, "A")));
      failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Reference '2' has no associate uuid");
    }
  }
}
