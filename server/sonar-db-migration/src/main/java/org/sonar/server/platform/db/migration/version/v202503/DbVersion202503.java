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
package org.sonar.server.platform.db.migration.version.v202503;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

// ignoring bad number formatting, as it's indented that we align the migration numbers to SQ versions
@SuppressWarnings("java:S3937")
public class DbVersion202503 implements DbVersion {

  /**
   * We use the start of the 10.X cycle as an opportunity to align migration numbers with the SQ version number.
   * Please follow this pattern:
   * 2025_03_000
   * 2025_03_001
   * 2025_03_002
   */
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2025_03_000, "Add known_package column to SCA releases", AddKnownPackageToScaReleasesTable.class)
      .add(2025_03_001, "Populate known_package column to SCA releases", PopulateKnownPackageColumnForScaReleasesTable.class)
      .add(2025_03_002, "Update known_package on SCA release to be not nullable", UpdateKnownPackageColumnNotNullable.class)
      .add(2025_03_003, "Add status column to SCA issues releases join table", AddStatusToScaIssuesReleasesTable.class)
      .add(2025_03_004, "Populate status column to SCA issues releases join table", PopulateStatusColumnForScaIssuesReleasesTable.class)
      .add(2025_03_005, "Update status column on SCA issues releases join table to be not nullable", UpdateScaIssuesReleasesStatusColumnNotNullable.class)
      .add(2025_03_006, "Add is_new column to SCA releases", AddIsNewToScaReleasesTable.class)
      .add(2025_03_007, "Add is_new column to SCA dependencies", AddIsNewToScaDependenciesTable.class)
      .add(2025_03_008, "Migrate to is_new on SCA releases", MigrateToIsNewOnScaReleases.class)
      .add(2025_03_009, "Migrate to is_new on SCA dependencies", MigrateToIsNewOnScaDependencies.class)
      .add(2025_03_010, "Drop new_in_pull_request column from SCA releases", DropNewInPullRequestFromScaReleasesTable.class)
      .add(2025_03_011, "Drop new_in_pull_request column from SCA dependencies", DropNewInPullRequestFromScaDependenciesTable.class)
      .add(2025_03_012, "Add assignee to SCA issues releases", AddAssigneeToScaIssuesReleases.class)
      .add(2025_03_013, "Create ScaIssuesReleasesHistory table", CreateScaIssuesReleasesChangesTable.class)
      .add(2025_03_014, "Create index for sca_issues_releases UUID on changes table", CreateIndexOnScaIssuesReleaseChangesReleaseId.class)
      .add(2025_03_015, "Update default SCA dependency issue status to OPEN from TO_REVIEW", UpdateScaIssuesReleasesOpenStatus.class)
      // TODO: change all these migration numbers before merge to master
      .add(9999_99_001, "Create SCA license profiles table", CreateScaLicenseProfilesTable.class)
      .add(9999_99_002, "Create SCA license profiles projects join table", CreateScaLicenseProfileProjectsTable.class)
      .add(9999_99_003, "Create unique index on SCA license profiles projects join table", CreateUniqueIndexOnScaLicenseProfileProjects.class)
      .add(9999_99_004, "Create SCA license profile customizations table", CreateScaLicenseProfileCustomizationsTable.class)
      .add(9999_99_005, "Create SCA license profile categories table", CreateScaLicenseProfileCategoriesTable.class)
      .add(9999_99_006, "Create unique index on SCA license profile categories table", CreateUniqueIndexOnScaLicenseProfileCategories.class)
      .add(9999_99_007, "Create unique index on SCA license profile customizations table", CreateUniqueIndexOnScaLicenseProfileCustomizations.class);
  }
}
