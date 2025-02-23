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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultScaIssueIdentityTest {

  @Test
  void test_constructWithValidValues() {
    var issueIdentity = new DefaultScaIssueIdentity(ScaIssueType.VULNERABILITY, "packageUrl", "vulnerabilityId", "spdxLicenseId");
    assertEquals(ScaIssueType.VULNERABILITY, issueIdentity.scaIssueType());
    assertEquals("packageUrl", issueIdentity.packageUrl());
    assertEquals("vulnerabilityId", issueIdentity.vulnerabilityId());
    assertEquals("spdxLicenseId", issueIdentity.spdxLicenseId());
  }

  @Test
  void test_throwsOnInvalidValues() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultScaIssueIdentity(ScaIssueType.VULNERABILITY, "", "vulnerabilityId", "spdxLicenseId"));
    assertThrows(IllegalArgumentException.class, () -> new DefaultScaIssueIdentity(ScaIssueType.VULNERABILITY, null, "vulnerabilityId", "spdxLicenseId"));
    assertThrows(IllegalArgumentException.class, () -> new DefaultScaIssueIdentity(ScaIssueType.VULNERABILITY, "packageUrl", "", "spdxLicenseId"));
    assertThrows(IllegalArgumentException.class, () -> new DefaultScaIssueIdentity(ScaIssueType.VULNERABILITY, "packageUrl", null, "spdxLicenseId"));
    assertThrows(IllegalArgumentException.class, () -> new DefaultScaIssueIdentity(ScaIssueType.VULNERABILITY, "packageUrl", "vulnerabilityId", ""));
    assertThrows(IllegalArgumentException.class, () -> new DefaultScaIssueIdentity(ScaIssueType.VULNERABILITY, "packageUrl", "vulnerabilityId", null));
  }
}
