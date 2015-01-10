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
package org.sonar.core.graph.jdbc;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import static org.assertj.core.api.Assertions.assertThat;

public class GraphDaoTest extends AbstractDaoTestCase {
  private GraphDao dao;

  @Before
  public void createDao() {
    dao = new GraphDao(getMyBatis());
    setupData("shared");
  }

  @Test
  public void select_graph_by_snapshot() {
    GraphDto testPlan = dao.selectBySnapshot("testplan", 11L);

    assertThat(testPlan.getId()).isEqualTo(101L);
    assertThat(testPlan.getResourceId()).isEqualTo(1L);
    assertThat(testPlan.getSnapshotId()).isEqualTo(11L);
    assertThat(testPlan.getFormat()).isEqualTo("graphson");
    assertThat(testPlan.getVersion()).isEqualTo(1);
    assertThat(testPlan.getPerspective()).isEqualTo("testplan");
    assertThat(testPlan.getRootVertexId()).isEqualTo("3456");
    assertThat(testPlan.getData()).isEqualTo("{testplan of snapshot 123}");
  }

  @Test
  public void select_by_snapshot_and_missing_perspective() {
    assertThat(dao.selectBySnapshot("duplicable", 123L)).isNull();
  }

  @Test
  public void select_by_missing_snapshot() {
    assertThat(dao.selectBySnapshot("duplicable", 7777L)).isNull();
  }

  @Test
  public void select_by_component() {
    GraphDto testPlan = dao.selectByComponent("testplan", "org.apache.struts:struts");

    assertThat(testPlan.getId()).isEqualTo(101L);
    assertThat(testPlan.getResourceId()).isEqualTo(1L);
    assertThat(testPlan.getSnapshotId()).isEqualTo(11L);
    assertThat(testPlan.getFormat()).isEqualTo("graphson");
    assertThat(testPlan.getVersion()).isEqualTo(1);
    assertThat(testPlan.getPerspective()).isEqualTo("testplan");
    assertThat(testPlan.getRootVertexId()).isEqualTo("3456");
    assertThat(testPlan.getData()).isEqualTo("{testplan of snapshot 123}");
  }

  @Test
  public void select_by_missing_component() {
    assertThat(dao.selectByComponent("testplan", "org.other:unknown")).isNull();
  }
}
