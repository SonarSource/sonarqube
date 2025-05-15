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
      .add(2025_03_016, "Create SCA license profiles table", CreateScaLicenseProfilesTable.class)
      .add(2025_03_017, "Create SCA license profiles projects join table", CreateScaLicenseProfileProjectsTable.class)
      .add(2025_03_018, "Create unique index on SCA license profiles projects join table", CreateUniqueIndexOnScaLicenseProfileProjects.class)
      .add(2025_03_019, "Create SCA license profile customizations table", CreateScaLicenseProfileCustomizationsTable.class)
      .add(2025_03_020, "Create SCA license profile categories table", CreateScaLicenseProfileCategoriesTable.class)
      .add(2025_03_021, "Create unique index on SCA license profile categories table", CreateUniqueIndexOnScaLicenseProfileCategories.class)
      .add(2025_03_022, "Create unique index on SCA license profile customizations table", CreateUniqueIndexOnScaLicenseProfileCustomizations.class)
      .add(2025_03_023, "Create unique index on SCA license profiles table", CreateUniqueIndexOnScaLicenseProfiles.class)
      .add(2025_03_024, "Create SCA encountered licenses table", CreateScaEncounteredLicensesTable.class)
      .add(2025_03_025, "Create SCA encountered licenses unique index", CreateUniqueIndexOnScaEncounteredLicenses.class)
      .add(2025_03_026, "Add change_comment to SCA issues releases changes", AddCommentToScaIssuesReleasesChangesTable.class)
      .add(2025_03_027, "Drop change_type from SCA issues releases changes", DropChangeTypeFromScaIssuesReleasesChangesTable.class)
      .add(2025_03_028, "Remove duplicates from SCA releases table", MigrateRemoveDuplicateScaReleases.class)
      .add(2025_03_029, "Create unique index on SCA releases table", CreateUniqueIndexOnScaReleases.class)
      .add(2025_03_030, "Create SCA analyses table", CreateScaAnalysesTable.class)
      .add(2025_03_031, "Create unique index on SCA analyses table", CreateUniqueIndexOnScaAnalyses.class)
      .add(2025_03_032, "Add 'analysis_uuid' column to 'architecture_graphs' table", AddAnalysisUuidOnArchitectureGraphs.class)
      .add(2025_03_033, "Add 'perspective_key' column to 'architecture_graphs' table", AddPerspectiveKeyOnArchitectureGraphs.class)
      .add(2025_03_034, "Drop unique index on 'architecture_graphs' table", DropIndexOnArchitectureGraphs.class)
      .add(2025_03_035, "Rename column 'source' to 'ecosystem' on 'architecture_graphs' table", UpdateArchitectureGraphsSourceColumnRename.class)
      .add(2025_03_036, "Create unique index on 'architecture_graphs' table", CreateUniqueIndexOnArchitectureGraphs.class)
      .add(2025_03_037, "Add previous_manual_status to SCA issues releases", AddPreviousManualStatusToScaIssuesReleases.class)
      .add(2025_03_038, "Add 'graph_version' column to 'architecture_graphs' table", AddGraphVersionOnArchitectureGraphsTable.class)
      .add(2025_03_039, "Add assignee name to SCA issues releases", AddAssigneeNameToScaIssuesReleases.class)
      .add(2025_03_040, "Drop assignee name from SCA issues releases", DropAssigneeNameFromScaIssuesReleases.class)
      .add(2025_03_041, "Remove AssigneeName from Sca issue release changes", BackfillRemoveAssigneeNameFromIssueReleaseChanges.class)
      .add(2025_03_042, "Remove non-canonical from Sca encountered licenses", MigrateRemoveNonCanonicalScaEncounteredLicenses.class)

    ;
  }
}
