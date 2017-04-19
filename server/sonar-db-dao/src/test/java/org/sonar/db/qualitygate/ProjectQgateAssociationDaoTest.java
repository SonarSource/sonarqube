/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.qualitygate;

import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.property.PropertyDto;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectQgateAssociationDaoTest {

  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private ProjectQgateAssociationDao underTest = db.getDbClient().projectQgateAssociationDao();

  @Test
  public void select_all_projects_by_query() {
    db.prepareDbUnit(getClass(), "shared.xml");

    ProjectQgateAssociationQuery query = ProjectQgateAssociationQuery.builder().gateId("42").build();
    List<ProjectQgateAssociationDto> result = underTest.selectProjects(dbSession, query);
    assertThat(result).hasSize(5);
  }

  @Test
  public void select_projects_by_query() {
    db.prepareDbUnit(getClass(), "shared.xml");

    assertThat(underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder().gateId("42").membership(ProjectQgateAssociationQuery.IN).build())).hasSize(3);
    assertThat(underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder().gateId("42").membership(ProjectQgateAssociationQuery.OUT).build())).hasSize(2);
  }

  @Test
  public void search_by_project_name() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ProjectQgateAssociationDto> result = underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder().gateId("42").projectSearch("one").build());
    assertThat(result).hasSize(1);

    assertThat(result.get(0).getName()).isEqualTo("Project One");

    result = underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder().gateId("42").projectSearch("one").build());
    assertThat(result).hasSize(1);
    result = underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder().gateId("42").projectSearch("project").build());
    assertThat(result).hasSize(5);
  }

  @Test
  public void should_be_sorted_by_project_name() {
    db.prepareDbUnit(getClass(), "shared.xml");

    List<ProjectQgateAssociationDto> result = underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder().gateId("42").build());
    assertThat(result).hasSize(5);
    assertThat(result.get(0).getName()).isEqualTo("Project Five");
    assertThat(result.get(1).getName()).isEqualTo("Project Four");
    assertThat(result.get(2).getName()).isEqualTo("Project One");
    assertThat(result.get(3).getName()).isEqualTo("Project Three");
    assertThat(result.get(4).getName()).isEqualTo("Project Two");
  }

  @Test
  public void select_qgate_id_is_absent() {
    ComponentDto project = db.components().insertPrivateProject();

    Optional<Long> result = underTest.selectQGateIdByComponentId(dbSession, project.getId());

    assertThat(result.isPresent()).isFalse();
  }

  @Test
  public void select_qgate_id() {
    associateProjectToQualityGate(10L, 1L);
    associateProjectToQualityGate(11L, 2L);

    Optional<Long> result = underTest.selectQGateIdByComponentId(dbSession, 10L);

    assertThat(result).contains(1L);
  }

  private void associateProjectToQualityGate(long componentId, long qualityGateId) {
    dbClient.propertiesDao().saveProperty(dbSession, new PropertyDto()
      .setKey("sonar.qualitygate")
      .setResourceId(componentId)
      .setValue(String.valueOf(qualityGateId)));
    db.commit();
  }
}
