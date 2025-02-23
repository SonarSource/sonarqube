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

import static com.google.common.base.Preconditions.checkArgument;

/**
 * <p>
 *   Default implementation of {@link ScaIssueIdentity}.
 * </p>
 * <p>
 *   Caution: missing fields are empty string, not null, so db unique constraint works.
 * </p>
 * @param scaIssueType the issue type
 * @param packageUrl the package url (may or may not have a version)
 * @param vulnerabilityId the vulnerability id such as CVE-12345
 * @param spdxLicenseId the SPDX license identifier (not license expression)
 */
public record DefaultScaIssueIdentity(ScaIssueType scaIssueType,
  String packageUrl,
  String vulnerabilityId,
  String spdxLicenseId) implements ScaIssueIdentity {
  public DefaultScaIssueIdentity {
    checkIdentityColumn(packageUrl, "packageUrl");
    checkIdentityColumn(vulnerabilityId, "vulnerabilityId");
    checkIdentityColumn(spdxLicenseId, "spdxLicenseId");
  }

  private static void checkIdentityColumn(String value, String name) {
    checkArgument(value != null, "DefaultScaIssueIdentity.%s cannot be null", name);
    checkArgument(!value.isBlank(), "DefaultScaIssueIdentity.%s cannot be blank, use ScaIssueDto.NULL_VALUE", name);
  }
}
