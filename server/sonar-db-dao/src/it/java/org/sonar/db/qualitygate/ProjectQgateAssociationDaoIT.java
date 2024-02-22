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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ProjectQgateAssociationDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create();

  private final DbSession dbSession = db.getSession();
  private final ProjectQgateAssociationDao underTest = db.getDbClient().projectQgateAssociationDao();

  @Test
  void select_all_projects_by_query() {
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject().getProjectDto();
    db.qualityGates().associateProjectToQualityGate(project1, qualityGate1);
    db.qualityGates().associateProjectToQualityGate(project2, qualityGate1);
    db.qualityGates().associateProjectToQualityGate(project3, qualityGate2);

    List<ProjectQgateAssociationDto> result = underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate1)
      .build());

    assertThat(result)
      .extracting(ProjectQgateAssociationDto::getUuid, ProjectQgateAssociationDto::getKey, ProjectQgateAssociationDto::getName,
        ProjectQgateAssociationDto::getGateUuid)
      .containsExactlyInAnyOrder(
        tuple(project1.getUuid(), project1.getKey(), project1.getName(), qualityGate1.getUuid()),
        tuple(project2.getUuid(), project2.getKey(), project2.getName(), qualityGate1.getUuid()),
        tuple(project3.getUuid(), project3.getKey(), project3.getName(), null));
  }

  @Test
  void select_all_projects_by_query_should_have_deterministic_order() {
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    ProjectDto project1 = db.components().insertPrivateProject(d -> d.setName("p1").setKey("key1")).getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject(d -> d.setName("p1").setKey("key2")).getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject(d -> d.setName("p2").setKey("key3")).getProjectDto();
    db.qualityGates().associateProjectToQualityGate(project1, qualityGate1);
    db.qualityGates().associateProjectToQualityGate(project2, qualityGate1);
    db.qualityGates().associateProjectToQualityGate(project3, qualityGate1);

    List<ProjectQgateAssociationDto> result = underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate1)
      .build());

    assertThat(result)
      .extracting(ProjectQgateAssociationDto::getUuid)
      .containsExactlyInAnyOrder(project1.getUuid(), project2.getUuid(), project3.getUuid());
  }

  @Test
  void select_projects_by_query() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject().getProjectDto();
    db.qualityGates().associateProjectToQualityGate(project1, qualityGate);
    db.qualityGates().associateProjectToQualityGate(project2, qualityGate);

    assertThat(underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate)
      .membership(ProjectQgateAssociationQuery.IN)
      .build()))
      .extracting(ProjectQgateAssociationDto::getUuid, ProjectQgateAssociationDto::getName, ProjectQgateAssociationDto::getGateUuid)
      .containsExactlyInAnyOrder(
        tuple(project1.getUuid(), project1.getName(), qualityGate.getUuid()),
        tuple(project2.getUuid(), project2.getName(), qualityGate.getUuid()));

    assertThat(underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate)
      .membership(ProjectQgateAssociationQuery.OUT)
      .build()))
      .extracting(ProjectQgateAssociationDto::getUuid, ProjectQgateAssociationDto::getName, ProjectQgateAssociationDto::getGateUuid)
      .containsExactlyInAnyOrder(tuple(project3.getUuid(), project3.getName(), null));
  }

  @Test
  void search_by_project_name() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project1 = db.components().insertPrivateProject(p -> p.setName("Project One")).getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject(p -> p.setName("Project Two")).getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject(p -> p.setName("Project Three")).getProjectDto();
    db.qualityGates().associateProjectToQualityGate(project1, qualityGate);
    db.qualityGates().associateProjectToQualityGate(project2, qualityGate);

    assertThat(underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate)
      .projectSearch("one")
      .build()))
      .extracting(ProjectQgateAssociationDto::getUuid)
      .containsExactlyInAnyOrder(project1.getUuid());

    assertThat(underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate)
      .projectSearch("project")
      .build()))
      .extracting(ProjectQgateAssociationDto::getUuid)
      .containsExactlyInAnyOrder(project1.getUuid(), project2.getUuid(), project3.getUuid());
  }

  @Test
  void sorted_by_project_name() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project1 = db.components().insertPrivateProject(p -> p.setName("Project One")).getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject(p -> p.setName("Project Two")).getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject(p -> p.setName("Project Three")).getProjectDto();

    assertThat(underTest.selectProjects(dbSession, ProjectQgateAssociationQuery.builder()
      .qualityGate(qualityGate)
      .build()))
      .extracting(ProjectQgateAssociationDto::getUuid)
      .containsExactly(project1.getUuid(), project3.getUuid(), project2.getUuid());
  }

  @Test
  void select_all() {
    List<ProjectQgateAssociationDto> t = underTest.selectAll(dbSession);

    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project3 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project4 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project5 = db.components().insertPrivateProject().getProjectDto();
    db.qualityGates().associateProjectToQualityGate(project1, qualityGate1);
    db.qualityGates().associateProjectToQualityGate(project2, qualityGate2);
    db.qualityGates().associateProjectToQualityGate(project3, qualityGate1);
    db.qualityGates().associateProjectToQualityGate(project4, qualityGate2);
    db.qualityGates().associateProjectToQualityGate(project5, qualityGate1);

    List<ProjectQgateAssociationDto> result = underTest.selectAll(dbSession);

    assertThat(result)
      .extracting(ProjectQgateAssociationDto::getUuid, ProjectQgateAssociationDto::getGateUuid)
      .containsExactlyInAnyOrder(
        tuple(project1.getUuid(), qualityGate1.getUuid()),
        tuple(project2.getUuid(), qualityGate2.getUuid()),
        tuple(project3.getUuid(), qualityGate1.getUuid()),
        tuple(project4.getUuid(), qualityGate2.getUuid()),
        tuple(project5.getUuid(), qualityGate1.getUuid()));
  }

  @Test
  void select_qgate_uuid_is_absent() {
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    Optional<String> result = underTest.selectQGateUuidByProjectUuid(dbSession, project.getUuid());

    assertThat(result).isEmpty();
  }

  @Test
  void select_qgate_uuid() {
    QualityGateDto qualityGate1 = db.qualityGates().insertQualityGate();
    QualityGateDto qualityGate2 = db.qualityGates().insertQualityGate();
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    ProjectDto project2 = db.components().insertPrivateProject().getProjectDto();
    db.qualityGates().associateProjectToQualityGate(project1, qualityGate1);
    db.qualityGates().associateProjectToQualityGate(project2, qualityGate2);

    Optional<String> result = underTest.selectQGateUuidByProjectUuid(dbSession, project1.getUuid());

    assertThat(result).contains(qualityGate1.getUuid());
  }

  @Test
  void delete_by_project_uuid() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    db.qualityGates().associateProjectToQualityGate(project, qualityGate);

    underTest.deleteByProjectUuid(dbSession, project.getUuid());

    Optional<String> deletedQualityGate = db.qualityGates().selectQGateUuidByProjectUuid(project.getUuid());

    assertThat(deletedQualityGate).isEmpty();
  }

  @Test
  void delete_by_qgate_uuid() {
    QualityGateDto qualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    db.qualityGates().associateProjectToQualityGate(project, qualityGate);

    underTest.deleteByQGateUuid(dbSession, qualityGate.getUuid());

    Optional<String> deletedQualityGate = db.qualityGates().selectQGateUuidByProjectUuid(project.getUuid());

    assertThat(deletedQualityGate).isEmpty();
  }

  @Test
  void update_project_qgate_association() {
    QualityGateDto firstQualityGate = db.qualityGates().insertQualityGate();
    QualityGateDto secondQualityGate = db.qualityGates().insertQualityGate();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();

    db.qualityGates().associateProjectToQualityGate(project, firstQualityGate);

    underTest.updateProjectQGateAssociation(dbSession, project.getUuid(), secondQualityGate.getUuid());

    Optional<String> updatedQualityGateUuid = db.qualityGates().selectQGateUuidByProjectUuid(project.getUuid());

    assertThat(updatedQualityGateUuid).contains(secondQualityGate.getUuid());
  }
}
