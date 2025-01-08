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
package org.sonar.server.platform.db.migration.version.v103;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

// ignoring bad number formatting, as it's indented that we align the migration numbers to SQ versions
@SuppressWarnings("java:S3937")
public class DbVersion103 implements DbVersion {

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
      .add(10_3_000, "Set 'sonar.qualityProfiles.allowDisableInheritedRules' to false for upgraded instances", SetAllowQualityProfileDisableInheritedRules.class)
      .add(10_3_001, "Add table 'github_perms_mapping'", CreateGithubPermissionsMappingTable.class)
      .add(10_3_002, "Create unique index on 'github_perms_mapping'", CreateUniqueIndexForGithubPermissionsMappingTable.class)
      .add(10_3_003, "Add default mappings to 'github_perms_mapping'", PopulateGithubPermissionsMapping.class)
      .add(10_3_004, "Add 'clean_code_attribute' column in 'issues' table", AddCleanCodeAttributeColumnInIssuesTable.class)
      .add(10_3_005, "Add 'creation_method' column in 'projects' table", AddCreationMethodColumnInProjectsTable.class)
      .add(10_3_006, "Populate 'creation_method' column in 'projects' table", PopulateCreationMethodColumnInProjectsTable.class)
      .add(10_3_007, "Make 'creation_method' column in 'projects' table non-nullable", MakeCreationMethodColumnInProjectsNotNullable.class)
      .add(10_3_008, "Add 'rule_changes_uuid' column in 'qprofile_changes'", AddRuleChangesUuidColumnInQProfileChanges.class)
      .add(10_3_009, "Create table 'rule_changes'", CreateRuleChangesTable.class)
      .add(10_3_010, "Create table 'rule_impact_changes'", CreateRuleImpactChangesTable.class)
      .add(10_3_011, "Create index for 'rule_impact_changes'", CreateIndexForRuleImpactChangesTable.class)
      .add(10_3_012, "Add 'sq_version' column in 'qprofile_changes' table", AddSqVersionColumnInQprofileChangesTable.class)
      .add(10_3_013, "Deduplicate potential records in 'properties' table", DeduplicatePropertiesTable.class)
      .add(10_3_014, "Create unique index to 'properties' table", CreateUniqueIndexForPropertiesTable.class);
  }
}
