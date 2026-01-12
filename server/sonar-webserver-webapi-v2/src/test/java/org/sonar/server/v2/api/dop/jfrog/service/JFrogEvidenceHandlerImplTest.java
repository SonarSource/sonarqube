/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.v2.api.dop.jfrog.service;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ce.CeActivityDao;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.measure.ProjectMeasureDao;
import org.sonar.db.measure.ProjectMeasureDto;
import org.sonar.db.permission.GlobalPermission;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.db.project.ProjectDao;
import org.sonar.db.project.ProjectDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.v2.api.dop.jfrog.JFrogEvidenceSonarQubeFeature;
import org.sonar.server.v2.api.dop.jfrog.response.GateCondition;
import org.sonar.server.v2.api.dop.jfrog.response.GateStatus;
import org.sonar.server.v2.api.dop.jfrog.response.SonarQubeStatement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JFrogEvidenceHandlerImplTest {

  private static final String TASK_ID = "task-uuid-123";
  private static final String ANALYSIS_UUID = "analysis-uuid-456";
  private static final String COMPONENT_UUID = "component-uuid-789";
  private static final String PROJECT_UUID = "project-uuid-abc";

  private final DbClient dbClient = mock();
  private final DbSession dbSession = mock();
  private final CeActivityDao ceActivityDao = mock();
  private final ProjectDao projectDao = mock();
  private final ProjectMeasureDao projectMeasureDao = mock();
  private final JFrogEvidenceMarkdownService markdownService = new JFrogEvidenceMarkdownService();
  private final JFrogEvidenceSonarQubeFeature feature = mock();

  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private JFrogEvidenceHandlerImpl underTest;

  @BeforeEach
  void setUp() {
    when(dbClient.openSession(false)).thenReturn(dbSession);
    when(dbClient.ceActivityDao()).thenReturn(ceActivityDao);
    when(dbClient.projectDao()).thenReturn(projectDao);
    when(dbClient.projectMeasureDao()).thenReturn(projectMeasureDao);
    when(feature.isAvailable()).thenReturn(true);

    underTest = new JFrogEvidenceHandlerImpl(dbClient, userSession, markdownService, feature);
  }

  @Test
  void getEvidence_whenFeatureNotAvailable_shouldThrow() {
    when(feature.isAvailable()).thenReturn(false);

    assertThatThrownBy(() -> underTest.getEvidence(TASK_ID))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("JFrog evidence is only available in Enterprise Edition and above");
  }

  @Test
  void getEvidence_whenTaskNotFound_shouldThrow() {
    when(ceActivityDao.selectByUuid(dbSession, TASK_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.getEvidence(TASK_ID))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Task 'task-uuid-123' not found");
  }

  @Test
  void getEvidence_whenProjectNotFound_shouldThrow() {
    CeActivityDto ceActivity = createCeActivityDto();
    when(ceActivityDao.selectByUuid(dbSession, TASK_ID)).thenReturn(Optional.of(ceActivity));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> underTest.getEvidence(TASK_ID))
      .isInstanceOf(NotFoundException.class)
      .hasMessage("Project 'project-uuid-abc' not found");
  }

  @Test
  void getEvidence_whenUserNotAuthorized_shouldThrow() {
    CeActivityDto ceActivity = createCeActivityDto();
    ProjectDto project = createProjectDto();

    when(ceActivityDao.selectByUuid(dbSession, TASK_ID)).thenReturn(Optional.of(ceActivity));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(project));

    userSession.logIn();

    assertThatThrownBy(() -> underTest.getEvidence(TASK_ID))
      .isInstanceOf(ForbiddenException.class)
      .hasMessage("Insufficient privileges");
  }

  @ParameterizedTest
  @MethodSource("permissionTestCases")
  void getEvidence_withValidPermission_shouldSucceed(ProjectPermission permission) {
    CeActivityDto ceActivity = createCeActivityDto();
    ProjectDto project = createProjectDto();

    when(ceActivityDao.selectByUuid(dbSession, TASK_ID)).thenReturn(Optional.of(ceActivity));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(project));
    when(projectMeasureDao.selectMeasure(dbSession, ANALYSIS_UUID, COMPONENT_UUID, CoreMetrics.QUALITY_GATE_DETAILS_KEY))
      .thenReturn(Optional.empty());

    userSession.logIn().addProjectPermission(permission, project);

    SonarQubeStatement result = underTest.getEvidence(TASK_ID);

    assertThat(result).isNotNull();
    assertThat(result.type()).isEqualTo(SonarQubeStatement.STATEMENT_TYPE);
    assertThat(result.predicateType()).isEqualTo(SonarQubeStatement.PREDICATE_TYPE);
  }

  static Stream<Arguments> permissionTestCases() {
    return Stream.of(
      Arguments.of(ProjectPermission.USER),
      Arguments.of(ProjectPermission.ADMIN),
      Arguments.of(ProjectPermission.SCAN));
  }

  @Test
  void getEvidence_withGlobalScanPermission_shouldSucceed() {
    CeActivityDto ceActivity = createCeActivityDto();
    ProjectDto project = createProjectDto();

    when(ceActivityDao.selectByUuid(dbSession, TASK_ID)).thenReturn(Optional.of(ceActivity));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(project));
    when(projectMeasureDao.selectMeasure(dbSession, ANALYSIS_UUID, COMPONENT_UUID, CoreMetrics.QUALITY_GATE_DETAILS_KEY))
      .thenReturn(Optional.empty());

    userSession.logIn().addPermission(GlobalPermission.SCAN);

    SonarQubeStatement result = underTest.getEvidence(TASK_ID);

    assertThat(result).isNotNull();
  }

  @Test
  void getEvidence_withNoMeasureData_shouldReturnNoneStatus() {
    CeActivityDto ceActivity = createCeActivityDto();
    ProjectDto project = createProjectDto();

    when(ceActivityDao.selectByUuid(dbSession, TASK_ID)).thenReturn(Optional.of(ceActivity));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(project));
    when(projectMeasureDao.selectMeasure(dbSession, ANALYSIS_UUID, COMPONENT_UUID, CoreMetrics.QUALITY_GATE_DETAILS_KEY))
      .thenReturn(Optional.empty());

    userSession.logIn().addProjectPermission(ProjectPermission.USER, project);

    SonarQubeStatement result = underTest.getEvidence(TASK_ID);

    assertThat(result.predicate().gates()).hasSize(1);
    assertThat(result.predicate().gates().get(0).status()).isEqualTo(GateStatus.NONE);
  }

  @ParameterizedTest
  @MethodSource("qualityGateStatusTestCases")
  void getEvidence_shouldParseQualityGateStatus(String level, GateStatus expectedStatus) {
    CeActivityDto ceActivity = createCeActivityDto();
    ProjectDto project = createProjectDto();
    String qualityGateDetails = """
      {
        "level": "%s",
        "conditions": [],
        "ignoredConditions": false
      }
      """.formatted(level);

    ProjectMeasureDto measure = new ProjectMeasureDto();
    measure.setData(qualityGateDetails);

    when(ceActivityDao.selectByUuid(dbSession, TASK_ID)).thenReturn(Optional.of(ceActivity));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(project));
    when(projectMeasureDao.selectMeasure(dbSession, ANALYSIS_UUID, COMPONENT_UUID, CoreMetrics.QUALITY_GATE_DETAILS_KEY))
      .thenReturn(Optional.of(measure));

    userSession.logIn().addProjectPermission(ProjectPermission.USER, project);

    SonarQubeStatement result = underTest.getEvidence(TASK_ID);

    assertThat(result.predicate().gates().get(0).status()).isEqualTo(expectedStatus);
  }

  static Stream<Arguments> qualityGateStatusTestCases() {
    return Stream.of(
      Arguments.of("OK", GateStatus.OK),
      Arguments.of("ERROR", GateStatus.ERROR),
      Arguments.of("WARN", GateStatus.WARN),
      Arguments.of("UNKNOWN", GateStatus.NONE));
  }

  @Test
  void getEvidence_shouldParseConditions() {
    CeActivityDto ceActivity = createCeActivityDto();
    ProjectDto project = createProjectDto();
    String qualityGateDetails = """
      {
        "level": "OK",
        "conditions": [
          {
            "metric": "new_coverage",
            "op": "LT",
            "error": "80",
            "actual": "85",
            "level": "OK"
          }
        ],
        "ignoredConditions": false
      }
      """;

    ProjectMeasureDto measure = new ProjectMeasureDto();
    measure.setData(qualityGateDetails);

    when(ceActivityDao.selectByUuid(dbSession, TASK_ID)).thenReturn(Optional.of(ceActivity));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(project));
    when(projectMeasureDao.selectMeasure(dbSession, ANALYSIS_UUID, COMPONENT_UUID, CoreMetrics.QUALITY_GATE_DETAILS_KEY))
      .thenReturn(Optional.of(measure));

    userSession.logIn().addProjectPermission(ProjectPermission.USER, project);

    SonarQubeStatement result = underTest.getEvidence(TASK_ID);

    assertThat(result.predicate().gates().get(0).conditions()).hasSize(1);

    GateCondition condition = result.predicate().gates().get(0).conditions().get(0);
    assertThat(condition.metricKey()).isEqualTo("new_coverage");
    assertThat(condition.comparator()).isEqualTo(GateCondition.Comparator.LT);
    assertThat(condition.errorThreshold()).isEqualTo("80");
    assertThat(condition.actualValue()).isEqualTo("85");
    assertThat(condition.status()).isEqualTo(GateStatus.OK);
  }

  @Test
  void getEvidence_withIgnoredConditions_shouldSetFlag() {
    CeActivityDto ceActivity = createCeActivityDto();
    ProjectDto project = createProjectDto();
    String qualityGateDetails = """
      {
        "level": "OK",
        "conditions": [],
        "ignoredConditions": true
      }
      """;

    ProjectMeasureDto measure = new ProjectMeasureDto();
    measure.setData(qualityGateDetails);

    when(ceActivityDao.selectByUuid(dbSession, TASK_ID)).thenReturn(Optional.of(ceActivity));
    when(projectDao.selectByUuid(dbSession, PROJECT_UUID)).thenReturn(Optional.of(project));
    when(projectMeasureDao.selectMeasure(dbSession, ANALYSIS_UUID, COMPONENT_UUID, CoreMetrics.QUALITY_GATE_DETAILS_KEY))
      .thenReturn(Optional.of(measure));

    userSession.logIn().addProjectPermission(ProjectPermission.USER, project);

    SonarQubeStatement result = underTest.getEvidence(TASK_ID);

    assertThat(result.predicate().gates().get(0).ignoredConditions()).isTrue();
  }

  private CeActivityDto createCeActivityDto() {
    CeActivityDto ceActivity = mock(CeActivityDto.class);
    when(ceActivity.getUuid()).thenReturn(TASK_ID);
    when(ceActivity.getAnalysisUuid()).thenReturn(ANALYSIS_UUID);
    when(ceActivity.getComponentUuid()).thenReturn(COMPONENT_UUID);
    when(ceActivity.getEntityUuid()).thenReturn(PROJECT_UUID);
    return ceActivity;
  }

  private ProjectDto createProjectDto() {
    ProjectDto project = new ProjectDto();
    project.setUuid(PROJECT_UUID);
    project.setKey("my-project");
    project.setName("My Project");
    project.setPrivate(true);
    return project;
  }

}
