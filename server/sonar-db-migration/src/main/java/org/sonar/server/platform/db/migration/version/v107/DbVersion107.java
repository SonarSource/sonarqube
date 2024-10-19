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
package org.sonar.server.platform.db.migration.version.v107;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

// ignoring bad number formatting, as it's indented that we align the migration numbers to SQ versions
@SuppressWarnings("java:S3937")
public class DbVersion107 implements DbVersion {

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
      .add(10_7_000, "Create 'telemetry_metrics_sent' table", CreateTelemetryMetricsSentTable.class)
      .add(10_7_001, "sonar.auth.gitlab.userConsentForPermissionProvisioningRequired", AddUserConsentRequiredIfGitlabAutoProvisioningEnabled.class)
      .add(10_7_002, "Migrate SMTP configuration into internal_properties", MigrateSmtpConfiguration.class)
      .add(10_7_003, "Drop 'github_perms_mapping' table if exists and 'devops_perms_mapping' table exists", DropGithubPermsMappingTableIfDevopsPermsMappingTableExists.class)
      .add(10_7_004, "Rename 'github_perms_mapping' table to 'devops_perms_mapping'", RenameGithubPermsMappingTable.class)
      .add(10_7_005, "Rename 'github_role' column to 'devops_platform_role' in devops_perms_mapping", RenameGithubRoleInDevopsPermsMapping.class)
      .add(10_7_006, "Add 'devops_platform' column to 'devops_perms_mapping' table", AddDevopsPlatformColumnInDevopsPermsMapping.class)
      .add(10_7_007, "Drop constraint on 'uuid' for 'devops_perms_mapping' table", DropPrimaryKeyOnDevopsPermsMappingTable.class)
      .add(10_7_008, "Create primary key on 'devops_perms_mapping.uuid'", CreatePrimaryKeyConstraintOnDevopsPermsMappingTable.class)
      .add(10_7_009, "Drop index 'uniq_github_perm_mappings' in the 'devops_perms_mapping' table", DropIndexUniqGithubPermsMappingInDevopsPermsMappingTable.class)
      .add(10_7_010, "Create uniq index on 'devops_perms_mapping' table for columns 'devops_platform_role', 'sonarqube_permission' and 'devops_platform'",
        CreateUniqueIndexOnDevopsPermsMappingTable.class)
      .add(10_7_011, "Add default permissions for GitLab in 'devops_perms_mapping'", PopulateGitlabDevOpsPermissionsMapping.class)
      .add(10_7_012, "Create 'cves' table", CreateCvesTable.class)
      .add(10_7_013, "Create 'cve_cwe' table", CreateCveCweTable.class)
      .add(10_7_014, "Create 'issues_dependency' table", CreateIssuesDependencyTable.class)
      .add(10_7_015, "Add 'ai_code_assurance' column to 'projects' table", AddAiCodeAssuranceColumnInProjectsTable.class)
    ;
  }

}
