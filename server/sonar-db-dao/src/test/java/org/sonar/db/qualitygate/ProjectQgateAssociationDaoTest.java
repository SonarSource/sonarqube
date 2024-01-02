/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ProjectQgateAssociationDaoTest {

  @Rule
  public DbTester db = DbTester.create();

  private DbSession dbSession = db.getSession();
  private ProjectQgateAssociationDao underTest = db.getDbClient().projectQgateAssociationDao();

  @Test
  public void select_all_projects_by_query() {
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project1), qualityGate1);
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project2), qualityGate1);
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project3), qualityGate2);

    List<ProjectQgateAssociationDto> result = underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate1)
      .build());

    assertThat(result)
      .extracting(ProjectQgateAssociationDto::getUuid, ProjectQgateAssociationDto::getKey, ProjectQgateAssociationDto::getName, ProjectQgateAssociationDto::getGateUuid)
      .containsExactlyInAnyOrder(
        tuple(project1.uuid(), project1.getKey(), project1.name(), qualityGate1.getUuid()),
        tuple(project2.uuid(), project2.getKey(), project2.name(), qualityGate1.getUuid()),
        tuple(project3.uuid(), project3.getKey(), project3.name(), null));
  }

  @Test
  public void select_all_projects_by_query_should_have_deterministic_order() {
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    ComponentDto project1 = db.components().insertPrivateProject(d -> d.setName("p1").setKey("key1"));
    ComponentDto project2 = db.components().insertPrivateProject(d -> d.setName("p1").setKey("key2"));
    ComponentDto project3 = db.components().insertPrivateProject(d -> d.setName("p2").setKey("key3"));
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project1), qualityGate1);
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project2), qualityGate1);
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project3), qualityGate1);

    List<ProjectQgateAssociationDto> result = underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate1)
      .build());

    assertThat(result)
      .extracting(ProjectQgateAssociationDto::getUuid)
      .containsExactlyInAnyOrder(project1.uuid(), project2.uuid(), project3.uuid());
  }

  @Test
  public void select_projects_by_query() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project1), qualityGate);
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project2), qualityGate);

    assertThat(underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate)
      .membership(ProjectQgateAssociationQuery.IN)
      .build()))
      .extracting(ProjectQgateAssociationDto::getUuid, ProjectQgateAssociationDto::getName, ProjectQgateAssociationDto::getGateUuid)
      .containsExactlyInAnyOrder(
        tuple(project1.uuid(), project1.name(), qualityGate.getUuid()),
        tuple(project2.uuid(), project2.name(), qualityGate.getUuid()));

    assertThat(underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate)
      .membership(ProjectQgateAssociationQuery.OUT)
      .build()))
      .extracting(ProjectQgateAssociationDto::getUuid, ProjectQgateAssociationDto::getName, ProjectQgateAssociationDto::getGateUuid)
      .containsExactlyInAnyOrder(tuple(project3.uuid(), project3.name(), null));
  }

  @Test
  public void search_by_project_name() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project1 = db.components().insertPrivateProject(p -> p.setName("Project One"));
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setName("Project Two"));
    ComponentDto project3 = db.components().insertPrivateProject(p -> p.setName("Project Three"));
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project1), qualityGate);
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project2), qualityGate);

    assertThat(underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate)
      .projectSearch("one")
      .build()))
      .extracting(ProjectQgateAssociationDto::getUuid)
      .containsExactlyInAnyOrder(project1.uuid());

    assertThat(underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate)
      .projectSearch("project")
      .build()))
      .extracting(ProjectQgateAssociationDto::getUuid)
      .containsExactlyInAnyOrder(project1.uuid(), project2.uuid(), project3.uuid());
  }

  @Test
  public void sorted_by_project_name() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ComponentDto project1 = db.components().insertPrivateProject(p -> p.setName("Project One"));
    ComponentDto project2 = db.components().insertPrivateProject(p -> p.setName("Project Two"));
    ComponentDto project3 = db.components().insertPrivateProject(p -> p.setName("Project Three"));

    assertThat(underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate)
      .build()))
      .extracting(ProjectQgateAssociationDto::getUuid)
      .containsExactly(project1.uuid(), project3.uuid(), project2.uuid());
  }

  @Test
  public void select_all() {
    List<ProjectQgateAssociationDto> t = underTest.selectAll(dbSession);

    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    ComponentDto project3 = db.components().insertPrivateProject();
    ComponentDto project4 = db.components().insertPrivateProject();
    ComponentDto project5 = db.components().insertPrivateProject();
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project1), qualityGate1);
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project2), qualityGate2);
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project3), qualityGate1);
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project4), qualityGate2);
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project5), qualityGate1);

    List<ProjectQgateAssociationDto> result = underTest.selectAll(dbSession);

    assertThat(result)
      .extracting(ProjectQgateAssociationDto::getUuid, ProjectQgateAssociationDto::getGateUuid)
      .containsExactlyInAnyOrder(
        tuple(project1.uuid(), qualityGate1.getUuid()),
        tuple(project2.uuid(), qualityGate2.getUuid()),
        tuple(project3.uuid(), qualityGate1.getUuid()),
        tuple(project4.uuid(), qualityGate2.getUuid()),
        tuple(project5.uuid(), qualityGate1.getUuid()));
  }

  @Test
  public void select_qgate_uuid_is_absent() {
    ComponentDto project = db.components().insertPrivateProject();

    Optional<String> result = underTest.selectQGateUuidByProjectUuid(dbSession, project.uuid());

    assertThat(result).isEmpty();
  }

  @Test
  public void select_qgate_uuid() {
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate();
    ComponentDto project1 = db.components().insertPrivateProject();
    ComponentDto project2 = db.components().insertPrivateProject();
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project1), qualityGate1);
    db.qualityGates().associateProjectToQualityGate(db.components().getProjectDto(project2), qualityGate2);

    Optional<String> result = underTest.selectQGateUuidByProjectUuid(dbSession, project1.uuid());

    assertThat(result).contains(qualityGate1.getUuid());
  }

  @Test
  public void delete_by_project_uuid() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProjectDto();

    db.qualityGates().associateProjectToQualityGate(project, qualityGate);

    underTest.deleteByProjectUuid(dbSession, project.getUuid());

    Optional<String> deletedQualityGate = db.qualityGates().selectQGateUuidByComponentUuid(project.getUuid());

    assertThat(deletedQualityGate).isEmpty();
  }

  @Test
  public void delete_by_qgate_uuid() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProjectDto();

    db.qualityGates().associateProjectToQualityGate(project, qualityGate);

    underTest.deleteByQGateUuid(dbSession, qualityGate.getUuid());

    Optional<String> deletedQualityGate = db.qualityGates().selectQGateUuidByComponentUuid(project.getUuid());

    assertThat(deletedQualityGate).isEmpty();
  }

  @Test
  public void update_project_qgate_association() {
    QualityGateDto firstQualityGate = db.qualityGates().insertQualityGate();
    QualityGateDto secondQualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProjectDto();

    db.qualityGates().associateProjectToQualityGate(project, firstQualityGate);

    underTest.updateProjectQGateAssociation(dbSession, project.getUuid(), secondQualityGate.getUuid());

    Optional<String> updatedQualityGateUuid = db.qualityGates().selectQGateUuidByComponentUuid(project.getUuid());

    assertThat(updatedQualityGateUuid).contains(secondQualityGate.getUuid());
  }
}
