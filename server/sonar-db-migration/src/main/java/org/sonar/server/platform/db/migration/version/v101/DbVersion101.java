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
package org.sonar.server.platform.db.migration.version.v101;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

// ignoring bad number formatting, as it's indented that we align the migration numbers to SQ versions
@SuppressWarnings("java:S3937")
public class DbVersion101 implements DbVersion {

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
      .add(10_1_000, "Add 'scm_accounts' table", CreateScmAccountsTable.class)
      .add(10_1_001, "Migrate scm accounts from 'users' to 'scm_accounts' table", MigrateScmAccountsFromUsersToScmAccounts.class)
      .add(10_1_002, "Add index on 'scm_accounts.scm_account'", CreateIndexForScmAccountOnScmAccountsTable.class)
      .add(10_1_003, "Add index on 'users.email'", CreateIndexForEmailOnUsersTable.class)
      .add(10_1_004, "Drop 'scm_accounts' column in 'users' table", DropScmAccountsInUsers.class)
      .add(10_1_005, "Add column 'is_main' to 'project_branches' table", AddIsMainColumnInProjectBranches.class)
      .add(10_1_006, "Update value of 'is_main' in 'project_branches' table", UpdateIsMainColumnInProjectBranches.class)
      .add(10_1_007, "Alter column 'is_main' in 'project_branches' table - make it not nullable", AlterIsMainColumnInProjectBranches.class)
      .add(10_1_008, "Increase size of 'internal_properties.kee' from 20 to 40 characters", IncreaseKeeColumnSizeInInternalProperties.class)
      .add(10_1_009, "Create column 'project_uuid' in 'user_tokens", CreateProjectUuidInUserTokens.class)
      .add(10_1_010, "Remove user tokens linked to unexistent project", RemoveOrphanUserTokens.class)
      .add(10_1_011, "Populate 'project_key' in 'user_tokens'", PopulateProjectUuidInUserTokens.class)
      .add(10_1_012, "Drop column 'project_key' in 'user_tokens", DropProjectKeyInUserTokens.class)
      .add(10_1_013, "Increase size of 'ce_queue.task_type' from 15 to 40 characters", IncreaseTaskTypeColumnSizeInCeQueue.class)
      .add(10_1_014, "Increase size of 'ce_activity.task_type' from 15 to 40 characters", IncreaseTaskTypeColumnSizeInCeActivity.class)
      .add(10_1_015, "Add 'external_groups' table.", CreateExternalGroupsTable.class)
      .add(10_1_016, "Add index on 'external_groups(external_identity_provider, external_id).", CreateIndexOnExternalIdAndIdentityOnExternalGroupsTable.class)
      .add(10_1_017, "Add 'code_variants' column in 'issues' table", AddCodeVariantsColumnInIssuesTable.class)
      .add(10_1_018, "Fix different uuids for subportfolios", FixDifferentUuidsForSubportfolios.class)
      .add(10_1_019, "Add report_schedules table", AddReportSchedulesTable.class)
      .add(10_1_020, "Add report_subscriptions table", AddReportSubscriptionsTable.class)
      .add(10_1_021, "Add report_schedules unique index", CreateUniqueIndexForReportSchedulesTable.class)
      .add(10_1_022, "Add report_subscriptions unique index", CreateUniqueIndexForReportSubscriptionsTable.class)
      .add(10_1_023, "Rename column 'component_uuid' to 'entity_uuid' in the 'properties' table", RenameColumnComponentUuidInProperties.class)
      .add(10_1_024, "Populate report_schedules table", PopulateReportSchedules.class)
      .add(10_1_025, "Populate report_subscriptions table", PopulateReportSubscriptions.class)
      .add(10_1_026, "Remove report properties", RemoveReportProperties.class)
      ;
  }
}
