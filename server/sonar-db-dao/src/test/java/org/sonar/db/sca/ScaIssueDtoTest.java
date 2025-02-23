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

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScaIssueDtoTest {

  @Test
  void test_constructWithValidValues() {
    var dto = new ScaIssueDto("uuid", ScaIssueType.VULNERABILITY, "packageUrl", "vulnerabilityId", "spdxLicenseId", 1L, 2L);
    assertEquals("uuid", dto.uuid());
    assertEquals(ScaIssueType.VULNERABILITY, dto.scaIssueType());
    assertEquals("packageUrl", dto.packageUrl());
    assertEquals("vulnerabilityId", dto.vulnerabilityId());
    assertEquals("spdxLicenseId", dto.spdxLicenseId());
    assertEquals(1L, dto.createdAt());
    assertEquals(2L, dto.updatedAt());
  }

  @Test
  void test_throwsOnInvalidValues() {
    assertThrows(IllegalArgumentException.class, () -> new ScaIssueDto("uuid", ScaIssueType.VULNERABILITY, "", "vulnerabilityId", "spdxLicenseId", 1L, 2L));
    assertThrows(IllegalArgumentException.class, () -> new ScaIssueDto("uuid", ScaIssueType.VULNERABILITY, null, "vulnerabilityId", "spdxLicenseId", 1L, 2L));
    assertThrows(IllegalArgumentException.class, () -> new ScaIssueDto("uuid", ScaIssueType.VULNERABILITY, "packageUrl", "", "spdxLicenseId", 1L, 2L));
    assertThrows(IllegalArgumentException.class, () -> new ScaIssueDto("uuid", ScaIssueType.VULNERABILITY, "packageUrl", null, "spdxLicenseId", 1L, 2L));
    assertThrows(IllegalArgumentException.class, () -> new ScaIssueDto("uuid", ScaIssueType.VULNERABILITY, "packageUrl", "vulnerabilityId", "", 1L, 2L));
    assertThrows(IllegalArgumentException.class, () -> new ScaIssueDto("uuid", ScaIssueType.VULNERABILITY, "packageUrl", "vulnerabilityId", null, 1L, 2L));
  }

  @Test
  void test_constructFromIdentity() {
    var identity = new DefaultScaIssueIdentity(ScaIssueType.VULNERABILITY, "packageUrl", "vulnerabilityId", "spdxLicenseId");
    var dto = new ScaIssueDto("uuid", identity, 1L, 2L);
    assertEquals("uuid", dto.uuid());
    assertEquals(ScaIssueType.VULNERABILITY, dto.scaIssueType());
    assertEquals("packageUrl", dto.packageUrl());
    assertEquals("vulnerabilityId", dto.vulnerabilityId());
    assertEquals("spdxLicenseId", dto.spdxLicenseId());
    assertEquals(1L, dto.createdAt());
    assertEquals(2L, dto.updatedAt());
  }

  @Test
  void test_toBuilder_build_shouldRoundTrip() {
    var dto = new ScaIssueDto("uuid", ScaIssueType.VULNERABILITY, "packageUrl", "vulnerabilityId", "spdxLicenseId", 1L, 2L);
    assertEquals(dto.toBuilder().build(), dto);
  }
}
