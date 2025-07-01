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
package org.sonar.server.platform.db.migration.version.v202504;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion202504 implements DbVersion {
  // ignoring bad number formatting, as it's intended that we align the migration numbers to SQ versions
  @SuppressWarnings("java:S3937")

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2025_04_000, "Add 'withdrawn' column to 'sca_vulnerability_issues' table", AddWithdrawnToScaVulnerabilityIssues.class)
      .add(2025_04_001, "Add 'organization_uuid' column to 'sca_license_profiles' table", AddOrganizationUuidToScaLicenseProfiles.class)
      .add(2025_04_002, "Drop unique index from 'sca_license_profiles'", DropUniqueIndexOnScaLicenseProfiles.class)
      .add(2025_04_003, "Create unique index from 'sca_license_profiles'", CreateUniqueIndexOnScaLicenseProfiles.class)

    ;
  }
}
