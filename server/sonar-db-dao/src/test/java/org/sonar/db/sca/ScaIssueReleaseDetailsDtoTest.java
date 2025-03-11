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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ScaIssueReleaseDetailsDtoTest {
  @Test
  void test_toBuilder_build_shouldRoundTrip() {
    var dto = new ScaIssueReleaseDetailsDto("scaIssueReleaseUuid",
      ScaSeverity.INFO,
      "scaIssueUuid",
      "scaReleaseUuid",
      ScaIssueType.VULNERABILITY,
      true,
      "packageUrl",
      "vulnerabilityId",
      "spdxLicenseId",
      ScaSeverity.BLOCKER,
      List.of("cwe1"),
      BigDecimal.ONE,
      42L);
    assertThat(dto.toBuilder().build()).isEqualTo(dto);
  }
}
