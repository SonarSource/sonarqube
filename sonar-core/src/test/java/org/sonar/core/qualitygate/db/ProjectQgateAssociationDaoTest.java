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

package org.sonar.core.qualitygate.db;

import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectQgateAssociationDaoTest extends AbstractDaoTestCase {

  private ProjectQgateAssociationDao dao;

  @Before
  public void setUp() {
    dao = new ProjectQgateAssociationDao(getMyBatis());
  }

  @Test
  public void select_all_projects_by_query() throws Exception {
    setupData("shared");

    ProjectQgateAssociationQuery query = ProjectQgateAssociationQuery.builder().gateId("42").build();
    List<ProjectQgateAssociationDto> result = dao.selectProjects(query, 42L);
    assertThat(result).hasSize(5);
  }

  @Test
  public void select_projects_by_query() throws Exception {
    setupData("shared");

    assertThat(dao.selectProjects(ProjectQgateAssociationQuery.builder().gateId("42").membership(ProjectQgateAssociationQuery.IN).build(), 42L)).hasSize(3);
    assertThat(dao.selectProjects(ProjectQgateAssociationQuery.builder().gateId("42").membership(ProjectQgateAssociationQuery.OUT).build(), 42L)).hasSize(2);
  }

  @Test
  public void search_by_project_name() throws Exception {
    setupData("shared");

    List<ProjectQgateAssociationDto> result = dao.selectProjects(ProjectQgateAssociationQuery.builder().gateId("42").projectSearch("one").build(), 42L);
    assertThat(result).hasSize(1);

    assertThat(result.get(0).getName()).isEqualTo("Project One");

    result = dao.selectProjects(ProjectQgateAssociationQuery.builder().gateId("42").projectSearch("one").build(), 42L);
    assertThat(result).hasSize(1);
    result = dao.selectProjects(ProjectQgateAssociationQuery.builder().gateId("42").projectSearch("project").build(), 42L);
    assertThat(result).hasSize(2);
  }

  @Test
  public void should_be_sorted_by_project_name() throws Exception {
    setupData("shared");

    List<ProjectQgateAssociationDto> result = dao.selectProjects(ProjectQgateAssociationQuery.builder().gateId("42").build(), 42L);
    assertThat(result).hasSize(5);
    assertThat(result.get(0).getName()).isEqualTo("Project Five");
    assertThat(result.get(1).getName()).isEqualTo("Project Four");
    assertThat(result.get(2).getName()).isEqualTo("Project One");
    assertThat(result.get(3).getName()).isEqualTo("Project Three");
    assertThat(result.get(4).getName()).isEqualTo("Project Two");
  }
}
