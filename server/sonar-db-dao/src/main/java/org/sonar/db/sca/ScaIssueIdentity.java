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

/**
 * <p>
 *   Contains those fields which are in the unique index of the sca_issues table.
 *   This will be a subset of fields in the {@link ScaIssueDto} class.
 *   These fields are used to assign a global uuid to each issue, such as
 *   each vulnerability or each prohibited license.
 * </p>
 * <p>
 *   None of the fields are nullable; if not relevant to the issue's identity
 *   they must be empty string instead. Nulls are not usable in a unique index
 *   in standard sql.
 * </p>
 * <p>
 *   Implementations of this interface are allowed to include fields other than
 *   the identity fields in their equals and hashCode, so it is probably not
 *   appropriate to use instances of this interface as a hash key. You can likely
 *   use a concrete implementation of this interface as a hash key, though.
 * </p>
 */
public interface ScaIssueIdentity {
  ScaIssueType scaIssueType();

  String packageUrl();

  String vulnerabilityId();

  String spdxLicenseId();
}
