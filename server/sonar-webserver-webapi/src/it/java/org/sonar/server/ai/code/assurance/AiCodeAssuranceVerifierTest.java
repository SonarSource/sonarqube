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

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.core.platform.EditionProvider.Edition;
import org.sonar.db.DbClient;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.qualitygate.QualityGateDao;
import org.sonar.db.qualitygate.QualityGateDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiCodeAssuranceVerifierTest {
  private final ProjectDto projectDto = mock(ProjectDto.class);
  private final AiCodeAssuranceEntitlement aiCodeAssuranceEntitlement = mock(AiCodeAssuranceEntitlement.class);
  private final DbClient dbClient = mock(DbClient.class);
  private final QualityGateDao qualityGateDao = mock(QualityGateDao.class);
  private AiCodeAssuranceVerifier underTest;

  @ParameterizedTest
  @MethodSource("isAiCodeAssuredForProject")
  void isAiCodeAssuredForProject(boolean containsAiCode, boolean aiCodeSupportedQg, boolean expected) {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(true);
    underTest = new AiCodeAssuranceVerifier(aiCodeAssuranceEntitlement, dbClient);
    mockProjectAndQualityGate(containsAiCode, aiCodeSupportedQg);

    when(projectDto.getContainsAiCode()).thenReturn(containsAiCode);

    assertThat(underTest.isAiCodeAssured(projectDto)).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("paramsForGetAiCodeAssurance")
  void getAiCodeAssurance(boolean isFeatureEnabled, boolean containsAiCode, boolean aiCodeSupportedQg, AiCodeAssurance expected) {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(isFeatureEnabled);
    underTest = new AiCodeAssuranceVerifier(aiCodeAssuranceEntitlement, dbClient);
    mockProjectAndQualityGate(containsAiCode, aiCodeSupportedQg);

    assertThat(underTest.getAiCodeAssurance(projectDto)).isEqualTo(expected);
    assertThat(underTest.getAiCodeAssurance(containsAiCode, aiCodeSupportedQg)).isEqualTo(expected);
  }

  @ParameterizedTest
  @MethodSource("paramsForDefaultQualityGate")
  void getAiCodeAssurance_fallback_on_default_qg_when_no_qg_defined_and_contains_ai_code(boolean aiCodeSupportedQg,
    AiCodeAssurance expected) {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(true);
    underTest = new AiCodeAssuranceVerifier(aiCodeAssuranceEntitlement, dbClient);
    when(projectDto.getContainsAiCode()).thenReturn(true);
    mockDefaultQg(aiCodeSupportedQg);

    assertThat(underTest.getAiCodeAssurance(projectDto)).isEqualTo(expected);
  }

  @Test
  void getAiCodeAssurance_no_exception_when_no_default_qg_and_no_qg_defined_and_contains_ai_code() {
    when(aiCodeAssuranceEntitlement.isEnabled()).thenReturn(true);
    underTest = new AiCodeAssuranceVerifier(aiCodeAssuranceEntitlement, dbClient);
    when(projectDto.getContainsAiCode()).thenReturn(true);
    when(dbClient.qualityGateDao()).thenReturn(qualityGateDao);
    when(qualityGateDao.selectByProjectUuid(any(), any())).thenReturn(null);
    when(qualityGateDao.selectDefault(any())).thenReturn(null);

    assertThat(underTest.getAiCodeAssurance(projectDto)).isEqualTo(AiCodeAssurance.CONTAINS_AI_CODE);
  }

  private void mockDefaultQg(boolean aiCodeSupportedQg) {
    when(dbClient.qualityGateDao()).thenReturn(qualityGateDao);
    when(qualityGateDao.selectByProjectUuid(any(), any())).thenReturn(null);
    QualityGateDto defaultQualityGate = mock(QualityGateDto.class);
    when(defaultQualityGate.isAiCodeSupported()).thenReturn(aiCodeSupportedQg);
    when(qualityGateDao.selectDefault(any())).thenReturn(defaultQualityGate);
  }

  private void mockProjectAndQualityGate(boolean containsAiCode, boolean aiCodeSupportedQg) {
    when(projectDto.getContainsAiCode()).thenReturn(containsAiCode);
    when(dbClient.qualityGateDao()).thenReturn(qualityGateDao);
    QualityGateDto qualityGateDto = mock(QualityGateDto.class);
    when(qualityGateDto.isAiCodeSupported()).thenReturn(aiCodeSupportedQg);
    when(qualityGateDao.selectByProjectUuid(any(), any())).thenReturn(qualityGateDto);
  }

  private static Stream<Arguments> paramsForDefaultQualityGate() {
    return Stream.of(
      Arguments.of(true, AiCodeAssurance.AI_CODE_ASSURED),
      Arguments.of(false, AiCodeAssurance.CONTAINS_AI_CODE)
    );
  }

  private static Stream<Arguments> paramsForGetAiCodeAssurance() {
    return Stream.of(
      Arguments.of(false, true, true, AiCodeAssurance.NONE),
      Arguments.of(false, true, false, AiCodeAssurance.NONE),
      Arguments.of(false, false, false, AiCodeAssurance.NONE),
      Arguments.of(false, false, true, AiCodeAssurance.NONE),
      Arguments.of(true, true, true, AiCodeAssurance.AI_CODE_ASSURED),
      Arguments.of(true, true, false, AiCodeAssurance.CONTAINS_AI_CODE),
      Arguments.of(true, false, false, AiCodeAssurance.NONE),
      Arguments.of(true, false, true, AiCodeAssurance.NONE)
    );
  }

  private static Stream<Arguments> isAiCodeAssuredForProject() {
    return Stream.of(
      Arguments.of(true, true, true),
      Arguments.of(true, false, false),
      Arguments.of(false, false, false),
      Arguments.of(false, true, false)
    );
  }

}
