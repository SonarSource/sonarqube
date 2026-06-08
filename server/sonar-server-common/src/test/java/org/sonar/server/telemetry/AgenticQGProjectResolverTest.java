/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.telemetry;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.ProjectQgateAssociationDao;
import org.sonar.db.qualitygate.ProjectQgateAssociationDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.sonar.server.telemetry.TelemetryAgenticQGAdoptionProvider.AGENTIC_QUALITY_GATE_NAME;

@ExtendWith(MockitoExtension.class)
class AgenticQGProjectResolverTest {

  private static final String AGENTIC_GATE_UUID = "agentic-gate-uuid";
  private static final String OTHER_GATE_UUID = "other-gate-uuid";
  private static final String PROJECT_UUID_1 = "project-uuid-1";
  private static final String PROJECT_UUID_2 = "project-uuid-2";
  private static final String PROJECT_UUID_3 = "project-uuid-3";

  @Mock
  private DbClient dbClient;
  @Mock
  private DbSession dbSession;
  @Mock
  private QualityGateDao qualityGateDao;
  @Mock
  private ProjectQgateAssociationDao projectQgateAssociationDao;
  @Mock
  private ProjectDao projectDao;

  private AgenticQGProjectResolver underTest;

  @BeforeEach
  void setUp() {
    lenient().when(dbClient.qualityGateDao()).thenReturn(qualityGateDao);
    lenient().when(dbClient.projectQgateAssociationDao()).thenReturn(projectQgateAssociationDao);
    lenient().when(dbClient.projectDao()).thenReturn(projectDao);
    underTest = new AgenticQGProjectResolver(dbClient);
  }

  @Test
  void resolveAgenticProjectUuids_whenAgenticGateNotFound_returnsEmpty() {
    when(qualityGateDao.selectByName(dbSession, AGENTIC_QUALITY_GATE_NAME)).thenReturn(null);

    assertThat(underTest.resolveAgenticProjectUuids(dbSession)).isEmpty();
  }

  @Test
  void resolveAgenticProjectUuids_whenNoProjectsUseAgenticGate_returnsEmpty() {
    when(qualityGateDao.selectByName(dbSession, AGENTIC_QUALITY_GATE_NAME)).thenReturn(createQualityGateDto(AGENTIC_GATE_UUID));
    when(projectQgateAssociationDao.selectAll(dbSession)).thenReturn(emptyList());
    when(qualityGateDao.selectDefault(dbSession)).thenReturn(createQualityGateDto(OTHER_GATE_UUID));

    assertThat(underTest.resolveAgenticProjectUuids(dbSession)).isEmpty();
  }

  @Test
  void resolveAgenticProjectUuids_whenProjectsHaveExplicitAgenticGate_returnsThem() {
    when(qualityGateDao.selectByName(dbSession, AGENTIC_QUALITY_GATE_NAME)).thenReturn(createQualityGateDto(AGENTIC_GATE_UUID));
    when(projectQgateAssociationDao.selectAll(dbSession)).thenReturn(List.of(
      createAssociation(PROJECT_UUID_1, AGENTIC_GATE_UUID),
      createAssociation(PROJECT_UUID_2, OTHER_GATE_UUID)
    ));
    when(qualityGateDao.selectDefault(dbSession)).thenReturn(createQualityGateDto(OTHER_GATE_UUID));

    assertThat(underTest.resolveAgenticProjectUuids(dbSession)).containsExactly(PROJECT_UUID_1);
  }

  @Test
  void resolveAgenticProjectUuids_whenAgenticGateIsDefault_includesProjectsWithoutExplicitOverride() {
    when(qualityGateDao.selectByName(dbSession, AGENTIC_QUALITY_GATE_NAME)).thenReturn(createQualityGateDto(AGENTIC_GATE_UUID));
    when(projectQgateAssociationDao.selectAll(dbSession)).thenReturn(List.of(
      createAssociation(PROJECT_UUID_2, OTHER_GATE_UUID)
    ));
    when(qualityGateDao.selectDefault(dbSession)).thenReturn(createQualityGateDto(AGENTIC_GATE_UUID));
    when(projectDao.selectProjects(dbSession)).thenReturn(List.of(
      createProjectDto(PROJECT_UUID_1),
      createProjectDto(PROJECT_UUID_2),
      createProjectDto(PROJECT_UUID_3)
    ));

    assertThat(underTest.resolveAgenticProjectUuids(dbSession))
      .containsExactlyInAnyOrder(PROJECT_UUID_1, PROJECT_UUID_3);
  }

  @Test
  void resolveAgenticProjectUuids_whenAgenticGateIsDefaultAndSomeHaveExplicitAgenticGate_countsBoth() {
    when(qualityGateDao.selectByName(dbSession, AGENTIC_QUALITY_GATE_NAME)).thenReturn(createQualityGateDto(AGENTIC_GATE_UUID));
    when(projectQgateAssociationDao.selectAll(dbSession)).thenReturn(List.of(
      createAssociation(PROJECT_UUID_1, AGENTIC_GATE_UUID),
      createAssociation(PROJECT_UUID_2, OTHER_GATE_UUID)
    ));
    when(qualityGateDao.selectDefault(dbSession)).thenReturn(createQualityGateDto(AGENTIC_GATE_UUID));
    when(projectDao.selectProjects(dbSession)).thenReturn(List.of(
      createProjectDto(PROJECT_UUID_1),
      createProjectDto(PROJECT_UUID_2),
      createProjectDto(PROJECT_UUID_3)
    ));

    assertThat(underTest.resolveAgenticProjectUuids(dbSession))
      .containsExactlyInAnyOrder(PROJECT_UUID_1, PROJECT_UUID_3);
  }

  @Test
  void resolveAgenticProjectUuids_whenDefaultGateIsNull_returnsOnlyExplicitAdoptions() {
    when(qualityGateDao.selectByName(dbSession, AGENTIC_QUALITY_GATE_NAME)).thenReturn(createQualityGateDto(AGENTIC_GATE_UUID));
    when(projectQgateAssociationDao.selectAll(dbSession)).thenReturn(List.of(
      createAssociation(PROJECT_UUID_1, AGENTIC_GATE_UUID)
    ));
    when(qualityGateDao.selectDefault(dbSession)).thenReturn(null);

    assertThat(underTest.resolveAgenticProjectUuids(dbSession)).containsExactly(PROJECT_UUID_1);
  }

  @Test
  void isAgenticQGProject_whenProjectHasExplicitAgenticGate_returnsTrue() {
    when(qualityGateDao.selectByName(dbSession, AGENTIC_QUALITY_GATE_NAME)).thenReturn(createQualityGateDto(AGENTIC_GATE_UUID));
    when(qualityGateDao.selectByProjectUuid(dbSession, PROJECT_UUID_1)).thenReturn(createQualityGateDto(AGENTIC_GATE_UUID));

    assertThat(underTest.isAgenticQGProject(dbSession, PROJECT_UUID_1)).isTrue();
  }

  @Test
  void isAgenticQGProject_whenProjectHasExplicitOtherGate_returnsFalse() {
    when(qualityGateDao.selectByName(dbSession, AGENTIC_QUALITY_GATE_NAME)).thenReturn(createQualityGateDto(AGENTIC_GATE_UUID));
    when(qualityGateDao.selectByProjectUuid(dbSession, PROJECT_UUID_1)).thenReturn(createQualityGateDto(OTHER_GATE_UUID));

    assertThat(underTest.isAgenticQGProject(dbSession, PROJECT_UUID_1)).isFalse();
  }

  @Test
  void isAgenticQGProject_whenProjectHasNoExplicitGateAndDefaultIsAgentic_returnsTrue() {
    when(qualityGateDao.selectByName(dbSession, AGENTIC_QUALITY_GATE_NAME)).thenReturn(createQualityGateDto(AGENTIC_GATE_UUID));
    when(qualityGateDao.selectByProjectUuid(dbSession, PROJECT_UUID_1)).thenReturn(null);
    when(qualityGateDao.selectDefault(dbSession)).thenReturn(createQualityGateDto(AGENTIC_GATE_UUID));

    assertThat(underTest.isAgenticQGProject(dbSession, PROJECT_UUID_1)).isTrue();
  }

  @Test
  void isAgenticQGProject_whenAgenticGateNotFound_returnsFalse() {
    when(qualityGateDao.selectByName(dbSession, AGENTIC_QUALITY_GATE_NAME)).thenReturn(null);

    assertThat(underTest.isAgenticQGProject(dbSession, PROJECT_UUID_1)).isFalse();
  }

  private static QualityGateDto createQualityGateDto(String uuid) {
    return new QualityGateDto().setUuid(uuid);
  }

  private static ProjectQgateAssociationDto createAssociation(String projectUuid, String gateUuid) {
    return new ProjectQgateAssociationDto().setUuid(projectUuid).setGateUuid(gateUuid);
  }

  private static ProjectDto createProjectDto(String uuid) {
    return new ProjectDto().setUuid(uuid);
  }
}
