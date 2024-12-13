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
package org.sonar.server.ai.code.assurance;

import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.db.DbClient;
import org.sonar.db.component.BranchDao;
import org.sonar.db.component.BranchDto;
import org.sonar.db.measure.MeasureDao;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;
import org.sonar.server.ai.code.assurance.AiCodeAssuranceVerifier.QualityGateStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.server.ai.code.assurance.AiCodeAssuranceVerifier.QualityGateStatus.ERROR;
import static org.sonar.server.ai.code.assurance.AiCodeAssuranceVerifier.QualityGateStatus.OK;

class AiCodeAssuranceVerifierTest {
  public static final String BRANCH_KEY = "branchKey";
  private final ProjectDto projectDto = mock(ProjectDto.class);
  private final AiCodeAssuranceEntitlement aiCodeAssuranceEntitlement = mock(AiCodeAssuranceEntitlement.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final QualityGateDao qualityGateDao = mock(QualityGateDao.class);
  private final MeasureDao measureDao = mock(MeasureDao.class);
  private final BranchDao branchDao = mock(BranchDao.class);
  private AiCodeAssuranceVerifier underTest;

  @BeforeEach
  void setUp() {
    when(dbClient.qualityGateDao()).thenReturn(qualityGateDao);
    when(dbClient.branchDao()).thenReturn(branchDao);
    when(dbClient.measureDao()).thenReturn(measureDao);
  }

  @ParameterizedTest
  @MethodSource("isAiCodeAssuredForProject")
  void isAiCodeAssuredForProject(boolean containsAiCode, boolean aiCodeSupportedQg, MeasureDto qualityGateMeasure, boolean expected) {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(true);
    underTest = new AiCodeAssuranceVerifier(aiCodeAssuranceEntitlement, dbClient);
    mockProjectAndQualityGate(containsAiCode, aiCodeSupportedQg );
    mockQualityGateStatus(qualityGateMeasure);

    when(projectDto.getContainsAiCode()).thenReturn(containsAiCode);

    assertThat(underTest.isAiCodeAssured(projectDto)).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("paramsForGetAiCodeAssurance")
  void getAiCodeAssuranceForProject(boolean isFeatureEnabled, boolean containsAiCode, boolean aiCodeSupportedQg,
    MeasureDto qualityGateMeasure, AiCodeAssurance expected) {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(isFeatureEnabled);
    underTest = new AiCodeAssuranceVerifier(aiCodeAssuranceEntitlement, dbClient);
    mockProjectAndQualityGate(containsAiCode, aiCodeSupportedQg);
    mockQualityGateStatus(qualityGateMeasure);

    assertThat(underTest.getAiCodeAssurance(projectDto, null)).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("paramsForGetAiCodeAssurance")
  void getAiCodeAssuranceForBranch(boolean isFeatureEnabled, boolean containsAiCode, boolean aiCodeSupportedQg,
    MeasureDto qualityGateMeasure, AiCodeAssurance expected) {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(isFeatureEnabled);
    underTest = new AiCodeAssuranceVerifier(aiCodeAssuranceEntitlement, dbClient);
    mockProjectAndQualityGate(containsAiCode, aiCodeSupportedQg);
    mockBranch(BRANCH_KEY);
    mockQualityGateStatus(qualityGateMeasure);

    assertThat(underTest.getAiCodeAssurance(projectDto, BRANCH_KEY)).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("paramsForDefaultQualityGate")
  void getAiCodeAssurance_fallback_on_default_qg_when_no_qg_defined_and_contains_ai_code(boolean aiCodeSupportedQg,
    AiCodeAssurance expected) {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(true);
    underTest = new AiCodeAssuranceVerifier(aiCodeAssuranceEntitlement, dbClient);
    mockProject(true);
    mockQualityGateStatus(null);
    mockDefaultQg(aiCodeSupportedQg);

    assertThat(underTest.getAiCodeAssurance(projectDto)).isEqualTo(expected);
  }

  @Test
  void getAiCodeAssurance_no_exception_when_no_default_qg_and_no_qg_defined_and_contains_ai_code() {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(true);
    underTest = new AiCodeAssuranceVerifier(aiCodeAssuranceEntitlement, dbClient);
    mockProject(true);
    when(qualityGateDao.selectByProjectUuid(any(), any())).thenReturn(null);
    when(qualityGateDao.selectDefault(any())).thenReturn(null);

    assertThat(underTest.getAiCodeAssurance(projectDto)).isEqualTo(AiCodeAssurance.AI_CODE_ASSURANCE_OFF);
  }

  private void mockDefaultQg(boolean aiCodeSupportedQg) {
    when(qualityGateDao.selectByProjectUuid(any(), any())).thenReturn(null);
    QualityGateDto defaultQualityGate = mock(QualityGateDto.class);
    when(defaultQualityGate.isAiCodeSupported()).thenReturn(aiCodeSupportedQg);
    when(qualityGateDao.selectDefault(any())).thenReturn(defaultQualityGate);
  }

  private void mockProjectAndQualityGate(boolean containsAiCode, boolean aiCodeSupportedQg) {
    mockProject(containsAiCode);
    QualityGateDto qualityGateDto = mock(QualityGateDto.class);
    when(qualityGateDto.isAiCodeSupported()).thenReturn(aiCodeSupportedQg);
    when(qualityGateDao.selectByProjectUuid(any(), any())).thenReturn(qualityGateDto);
  }

  private void mockQualityGateStatus(@Nullable MeasureDto qgMeasure) {
    when(measureDao.selectByComponentUuidAndMetricKeys(any(), any(), any())).thenReturn(Optional.ofNullable(qgMeasure));
  }

  private void mockProject(boolean containsAiCode) {
    when(projectDto.getContainsAiCode()).thenReturn(containsAiCode);
    BranchDto branchDto = mock(BranchDto.class);
    when(branchDto.isMain()).thenReturn(true);
    when(branchDao.selectMainBranchByProjectUuid(any(), any())).thenReturn(Optional.of(branchDto));
  }

  private void mockBranch(String branchKey) {
    when(branchDao.selectByBranchKey(any(), any(), eq(branchKey))).thenReturn(Optional.of(mock(BranchDto.class)));
  }

  private static Stream<Arguments> paramsForDefaultQualityGate() {
    return Stream.of(
      Arguments.of(true, AiCodeAssurance.AI_CODE_ASSURANCE_ON),
      Arguments.of(false, AiCodeAssurance.AI_CODE_ASSURANCE_OFF)
    );
  }

  private static Stream<Arguments> paramsForGetAiCodeAssurance() {
    return Stream.of(
      Arguments.of(false, true, true, null, AiCodeAssurance.NONE),
      Arguments.of(false, true, false, null, AiCodeAssurance.NONE),
      Arguments.of(false, false, false, null, AiCodeAssurance.NONE),
      Arguments.of(false, false, true, null, AiCodeAssurance.NONE),
      Arguments.of(true, true, true, null, AiCodeAssurance.AI_CODE_ASSURANCE_ON),
      Arguments.of(true, true, false, null, AiCodeAssurance.AI_CODE_ASSURANCE_OFF),
      Arguments.of(true, false, false, null, AiCodeAssurance.NONE),
      Arguments.of(true, false, true, null, AiCodeAssurance.NONE),
      Arguments.of(false, true, true, mockQualityGateMeasure(null), AiCodeAssurance.NONE),
      Arguments.of(false, true, false, mockQualityGateMeasure(null), AiCodeAssurance.NONE),
      Arguments.of(false, false, false, mockQualityGateMeasure(null), AiCodeAssurance.NONE),
      Arguments.of(false, false, true, mockQualityGateMeasure(null), AiCodeAssurance.NONE),
      Arguments.of(true, true, true, mockQualityGateMeasure(null), AiCodeAssurance.AI_CODE_ASSURANCE_ON),
      Arguments.of(true, true, false, mockQualityGateMeasure(null), AiCodeAssurance.AI_CODE_ASSURANCE_OFF),
      Arguments.of(true, false, false, mockQualityGateMeasure(null), AiCodeAssurance.NONE),
      Arguments.of(true, false, true, mockQualityGateMeasure(null), AiCodeAssurance.NONE),
      Arguments.of(false, true, true, mockQualityGateMeasure(OK), AiCodeAssurance.NONE),
      Arguments.of(false, true, false, mockQualityGateMeasure(OK), AiCodeAssurance.NONE),
      Arguments.of(false, false, false, mockQualityGateMeasure(OK), AiCodeAssurance.NONE),
      Arguments.of(false, false, true, mockQualityGateMeasure(OK), AiCodeAssurance.NONE),
      Arguments.of(true, true, true, mockQualityGateMeasure(OK), AiCodeAssurance.AI_CODE_ASSURANCE_PASS),
      Arguments.of(true, true, false, mockQualityGateMeasure(OK), AiCodeAssurance.AI_CODE_ASSURANCE_OFF),
      Arguments.of(true, false, false, mockQualityGateMeasure(OK), AiCodeAssurance.NONE),
      Arguments.of(true, false, true, mockQualityGateMeasure(OK), AiCodeAssurance.NONE),
      Arguments.of(false, true, true, mockQualityGateMeasure(ERROR), AiCodeAssurance.NONE),
      Arguments.of(false, true, false, mockQualityGateMeasure(ERROR), AiCodeAssurance.NONE),
      Arguments.of(false, false, false, mockQualityGateMeasure(ERROR), AiCodeAssurance.NONE),
      Arguments.of(false, false, true, mockQualityGateMeasure(ERROR), AiCodeAssurance.NONE),
      Arguments.of(true, true, true, mockQualityGateMeasure(ERROR), AiCodeAssurance.AI_CODE_ASSURANCE_FAIL),
      Arguments.of(true, true, false, mockQualityGateMeasure(ERROR), AiCodeAssurance.AI_CODE_ASSURANCE_OFF),
      Arguments.of(true, false, false, mockQualityGateMeasure(ERROR), AiCodeAssurance.NONE),
      Arguments.of(true, false, true, mockQualityGateMeasure(ERROR), AiCodeAssurance.NONE)
    );
  }

  private static Stream<Arguments> isAiCodeAssuredForProject() {
    return Stream.of(
      Arguments.of(true, true, null, true),
      Arguments.of(true, false, null, false),
      Arguments.of(false, false, null, false),
      Arguments.of(false, true, null, false),
      Arguments.of(true, true, mockQualityGateMeasure(null), true),
      Arguments.of(true, false, mockQualityGateMeasure(null), false),
      Arguments.of(false, false, mockQualityGateMeasure(null), false),
      Arguments.of(false, true, mockQualityGateMeasure(null), false),
      Arguments.of(true, true, mockQualityGateMeasure(OK), true),
      Arguments.of(true, false, mockQualityGateMeasure(OK), false),
      Arguments.of(false, false, mockQualityGateMeasure(OK), false),
      Arguments.of(false, true, mockQualityGateMeasure(OK), false),
      Arguments.of(true, true, mockQualityGateMeasure(ERROR), true),
      Arguments.of(true, false, mockQualityGateMeasure(ERROR), false),
      Arguments.of(false, false, mockQualityGateMeasure(ERROR), false),
      Arguments.of(false, true, mockQualityGateMeasure(ERROR), false)
    );
  }

  private static MeasureDto mockQualityGateMeasure(@Nullable QualityGateStatus qualityGateStatus) {
    MeasureDto measureDto = mock(MeasureDto.class);
    when(measureDto.getString(ALERT_STATUS_KEY)).thenReturn(qualityGateStatus != null ? qualityGateStatus.name() : null);
    return measureDto;
  }
}
