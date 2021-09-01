/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;
import org.sonar.server.platform.db.migration.version.v84.issuechanges.DropIssueChangesTable;

public class DbVersion85 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(4000, "Delete 'project_alm_settings' orphans", DeleteProjectAlmSettingsOrphans.class)
      .add(4001, "Drop 'period', 'value_warning' columns from 'quality_gates_conditions' table", DropPeriodAndValueWarningColumnsFromQualityGateConditionsTable.class)
      .add(4002, "Drop 'project_alm_bindings' table", DropProjectAlmBindings.class)
      .add(4003, "Drop unused variation values columns in 'project_measures' table", DropUnusedVariationsInProjectMeasures.class)
      .add(4004, "Drop unused periods in 'snapshots' table", DropUnusedPeriodsInSnapshots.class)
      .add(4005, "Drop orphan favorites from 'properties' table", DropOrphanFavoritesFromProperties.class)
      .add(4006, "create 'tmp_issue_changes' table", CreateTmpIssueChangesTable.class)
      .add(4007, "drop 'issue_changes' table", DropIssueChangesTable.class)
      .add(4008, "rename 'tmp_issue_changes' table to 'issue_changes'", RenameTmpIssueChangesToIssueChanges.class)
      .add(4009, "Make 'issueKey' not nullable for 'issue_changes' table", MakeIssueKeyNotNullOnIssueChangesTable.class)
      .add(4010, "Make 'uuid' not nullable for 'issue_changes' table", MakeUuidNotNullOnIssueChangesTable.class)
      .add(4011, "Make 'project_uuid' not nullable for 'issue_changes' table", MakeProjectUuidNotNullOnIssueChangesTable.class)
      .add(4012, "add PK table to 'issue_changes'", AddPrimaryKeyOnUuidForIssueChangesTable.class)
      .add(4013, "add index on 'issue_key' for table 'issue_changes'", AddIndexOnIssueKeyForIssueChangesTable.class)
      .add(4014, "add index on 'kee' for table 'issue_changes'", AddIndexOnKeeForIssueChangesTable.class)
      .add(4015, "add index on 'project_uuid' for table 'issue_changes'", AddIndexOnProjectUuidOnIssueChangesTable.class)
      .add(4016, "Add 'type' column to 'plugins' table", AddTypeToPlugins.class)
      .add(4017, "Populate 'type' column in 'plugins' table", PopulateTypeInPlugins.class)
      .add(4018, "Alter 'type' column in 'plugins' to not nullable", AlterTypeInPluginNotNullable.class)
      .add(4019, "Add 'message_type' column to 'ce_task_message' table", AddMessageTypeColumnToCeTaskMessageTable.class)
      .add(4020, "Populate 'message_type' column of 'ce_task_message' table", PopulateMessageTypeColumnOfCeTaskMessageTable.class)
      .add(4021, "Make 'message_type' column not nullable for `ce_task_message` table", MakeMessageTypeColumnNotNullableOnCeTaskMessageTable.class)
      .add(4022, "Add index on 'message_type' column of `ce_task_message` table", AddIndexOnMessageTypeColumnOfCeTaskMessageTable.class)
      .add(4023, "Create 'user_dismissed_messages' table", CreateUserDismissedMessagesTable.class)
      .add(4024, "Populate 'branch_type' in 'project_branches'", FillProjectBranchesBranchType.class)
      .add(4025, "Make 'branch_type' in 'project_branches' not nullable", MakeProjectBranchesBranchTypeNotNullable.class)
      .add(4026, "Drop column 'key_type' in table 'project_branches'", DropProjectBranchesKeyType.class)
      .add(4027, "Add 'organization_uuid' column in table 'rules'", AddOrganizationUuidColumnToRulesTable.class)

    ;
  }
}
