/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.sca;

import java.math.BigDecimal;
import java.util.List;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class ScaIssueReleaseDetailsDtoTest {
  private static ScaIssueDto newSampleIssueDto() {
    return new ScaIssueDto("issueUuid",
      ScaIssueType.VULNERABILITY,
      "packageUrl",
      "vulnerabilityId",
      "spdxLicenseId",
      1L,
      2L);
  }

  private static ScaReleaseDto newSampleReleaseDto() {
    return new ScaReleaseDto("releaseUuid",
      "componentUuid",
      "packageUrl",
      PackageManager.MAVEN,
      "foo:bar",
      "1.0.0",
      "MIT",
      "NOASSERTION",
      true,
      false,
      2L,
      3L);
  }

  private static ScaIssueReleaseDto newSampleIssueReleaseDto(ScaIssueDto issueDto, ScaReleaseDto releaseDto) {
    return new ScaIssueReleaseDto(
      "issueReleaseUuid",
      issueDto.uuid(),
      releaseDto.uuid(),
      ScaSeverity.INFO,
      3L,
      4L);
  }

  private static ScaVulnerabilityIssueDto newSampleVulnerabilityIssueDto(ScaIssueDto issueDto) {
    return new ScaVulnerabilityIssueDto(
      issueDto.uuid(),
      ScaSeverity.HIGH,
      List.of("cwe1"),
      BigDecimal.ONE,
      5L,
      6L);
  }

  private static ScaIssueReleaseDetailsDto newSampleIssueReleaseDetailsDto() {
    var issueDto = newSampleIssueDto();
    var releaseDto = newSampleReleaseDto();
    var issueReleaseDto = newSampleIssueReleaseDto(issueDto, releaseDto);
    var vulnerabilityIssueDto = newSampleVulnerabilityIssueDto(issueDto);
    return new ScaIssueReleaseDetailsDto(
      issueReleaseDto.uuid(),
      issueReleaseDto,
      issueDto,
      releaseDto,
      vulnerabilityIssueDto);
  }

  @Test
  void test_toBuilder_build_shouldRoundTrip() {
    var dto = newSampleIssueReleaseDetailsDto();
    assertThat(dto.toBuilder().build()).isEqualTo(dto);
  }

  @Test
  void test_withMismatchedReleaseInIssueReleaseDto_throwsIllegalArgumentException() {
    var validDto = newSampleIssueReleaseDetailsDto();
    var differentIssueReleaseDto = validDto.issueReleaseDto().toBuilder().setScaReleaseUuid("differentUuid").build();
    var invalidBuilder = validDto.toBuilder().setIssueReleaseDto(differentIssueReleaseDto);
    assertThatThrownBy(invalidBuilder::build).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void test_withMismatchedIssueInIssueReleaseDto_throwsIllegalArgumentException() {
    var validDto = newSampleIssueReleaseDetailsDto();
    var differentIssueReleaseDto = validDto.issueReleaseDto().toBuilder().setScaIssueUuid("differentUuid").build();
    var invalidBuilder = validDto.toBuilder().setIssueReleaseDto(differentIssueReleaseDto);
    assertThatThrownBy(invalidBuilder::build).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void test_withMismatchedIssueReleaseUuid_throwsIllegalArgumentException() {
    var validDto = newSampleIssueReleaseDetailsDto();
    ThrowableAssert.ThrowingCallable constructInvalid = () -> new ScaIssueReleaseDetailsDto("differentUuid",
      validDto.issueReleaseDto(), validDto.issueDto(), validDto.releaseDto(), validDto.vulnerabilityIssueDto());
    assertThatThrownBy(constructInvalid).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void test_withMismatchedVulnerabilityIssue_throwsIllegalArgumentException() {
    var validDto = newSampleIssueReleaseDetailsDto();
    var differentVulnerabiiltyIssue = validDto.vulnerabilityIssueDto().toBuilder().setUuid("differentUuid").build();
    var invalidBuilder = validDto.toBuilder().setVulnerabilityIssueDto(differentVulnerabiiltyIssue);
    assertThatThrownBy(invalidBuilder::build).isInstanceOf(IllegalArgumentException.class);
  }
}
