/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v108;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

// ignoring bad number formatting, as it's indented that we align the migration numbers to SQ versions
@SuppressWarnings("java:S3937")
public class DbVersion108 implements DbVersion {

  /**
   * We use the start of the 10.X cycle as an opportunity to align migration numbers with the SQ version number.
   * Please follow this pattern:
   * 10_0_000
   * 10_0_001
   * 10_0_002
   * 10_1_000
   * 10_1_001
   * 10_1_002
   * 10_2_000
   */
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(10_8_000, "Create 'measures' table", CreateMeasuresTable.class)
      .add(10_8_001, "Add 'measures_migrated' column on 'project_branches' table", AddMeasuresMigratedColumnToProjectBranchesTable.class)
      .add(10_8_002, "Create index on 'project_branches.measures_migrated'", CreateIndexOnProjectBranchesMeasuresMigrated.class)
      .add(10_8_003, "Migrate the content of 'live_measures' to 'measures' for branches", MigrateBranchesLiveMeasuresToMeasures.class)
      .add(10_8_004, "Add 'measures_migrated' column on 'portfolios' table", AddMeasuresMigratedColumnToPortfoliosTable.class)
      .add(10_8_005, "Create index on 'portfolios.measures_migrated'", CreateIndexOnPortfoliosMeasuresMigrated.class)
      .add(10_8_006, "Migrate the content of 'live_measures' to 'measures' for portfolios", MigratePortfoliosLiveMeasuresToMeasures.class)
      .add(10_8_007, "Create primary key on 'measures' table", CreatePrimaryKeyOnMeasuresTable.class)
      .add(10_8_008, "Create index on column 'branch_uuid' in 'measures' table", CreateIndexOnMeasuresTable.class)
      .add(10_8_009, "Drop column 'from_hotspot' in the 'issues' table", DropColumnFromHotspotInIssues.class)
      .add(10_8_010, "Drop 'live_measures' table", DropLiveMeasuresTable.class)
      .add(10_8_011, "Drop index on 'portfolios.measures_migrated'", DropIndexOnPortfoliosMeasuresMigrated.class)
      .add(10_8_012, "Drop 'measures_migrated' column on 'portfolios' table", DropMeasuresMigratedColumnInPortfoliosTable.class)
      .add(10_8_013, "Drop index on 'project_branches.measures_migrated'", DropIndexOnProjectBranchesMeasuresMigrated.class)
      .add(10_8_014, "Drop 'measures_migrated' column on 'project_branches' table", DropMeasuresMigratedColumnInProjectBranchesTable.class)
      .add(10_8_015, "Add column 'impacts' in 'active_rules' table", AddImpactsColumnInActiveRulesTable.class)
      .add(10_8_016, "Create 'project_dependencies' table", CreateProjectDependenciesTable.class)
      .add(10_8_017, "Enable specific MQR mode", EnableSpecificMqrMode.class)
      .add(10_8_018, "Make columns 'published_at' and 'last_modified_at' nullable on the 'cves' table", AlterCveColumnsToNullable.class)
      .add(10_8_019, "Delete Software Quality ratings from project_measures", DeleteSoftwareQualityRatingFromProjectMeasures.class)
      .add(10_8_020, "Create new software quality metrics", CreateNewSoftwareQualityMetrics.class)
      .add(10_8_021, "Migrate deprecated project_measures to replacement metrics", MigrateProjectMeasuresDeprecatedMetrics.class)
      .add(10_8_022, "Add 'manual_severity' column in 'issues_impacts' table", AddManualSeverityColumnInIssuesImpactsTable.class)
      .add(10_8_023, "Add 'ai_code_fix_enabled' column to 'projects' table", AddAICodeFixEnabledColumnToProjectsTable.class)
      .add(10_8_024, "Migrate boolean values of 'sonar.ai.suggestions.enabled' property to new enum values", MigrateAiSuggestionEnabledValues.class)
      .add(10_8_025, "Add 'ai_code_supported' column in 'quality_gates' table", AddAICodeSupportedColumnToQualityGatesTable.class)
      .add(10_8_026, "Rename 'ai_code_assurance' column in 'projects' table to 'contains_ai_code", RenameAiCodeAssuranceColumnInProjects.class)
      .add(10_8_027, "Add 'invite_users_enabled' column in 'organizations' table", AddInviteUsersEnabledColumnOrganizationsTable.class)
    ;
  }

}
