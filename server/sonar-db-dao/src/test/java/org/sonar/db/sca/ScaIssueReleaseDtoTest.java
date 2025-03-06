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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ScaIssueReleaseDtoTest {

  @Test
  void test_toBuilder_build_shouldRoundTrip() {
    var scaIssueReleaseDto = new ScaIssueReleaseDto("sca-issue-release-uuid",
      "sca-issue-uuid",
      "sca-release-uuid",
      ScaSeverity.INFO,
      1L,
      2L);
    assertThat(scaIssueReleaseDto.toBuilder().build()).isEqualTo(scaIssueReleaseDto);
  }

  @Test
  void test_identity_shouldIgnoreUuidAndUpdatableFields() {
    var scaIssueReleaseDto = new ScaIssueReleaseDto("sca-issue-release-uuid",
      "sca-issue-uuid",
      "sca-release-uuid",
      ScaSeverity.INFO,
      1L,
      2L);
    var scaIssueReleaseDtoDifferentButSameIdentity = new ScaIssueReleaseDto("differentUuid",
      "sca-issue-uuid",
      "sca-release-uuid",
      ScaSeverity.HIGH,
      10L,
      20L);
    assertThat(scaIssueReleaseDto.identity()).isEqualTo(scaIssueReleaseDtoDifferentButSameIdentity.identity());
    assertThat(scaIssueReleaseDto).isNotEqualTo(scaIssueReleaseDtoDifferentButSameIdentity);
  }
}
