/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v104;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

// ignoring bad number formatting, as it's indented that we align the migration numbers to SQ versions
@SuppressWarnings("java:S3937")
public class DbVersion104 implements DbVersion {

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
      .add(10_4_000, "Delete redundant Failed Alerts for Applications", DeleteRedundantFailedAlertsForApplications.class)
      .add(10_4_001, "Rename metric 'wont_fix_issues' to 'accepted_issues'", RenameWontFixIssuesMetric.class)
      .add(10_4_002, "Create table 'rules_tags'", CreateRuleTagsTable.class)
      .add(10_4_003, "Populate 'rule_tags' table", PopulateRuleTagsTable.class)
      .add(10_4_004, "Drop column 'tags' in the 'rules' table", DropTagsInRules.class)
      .add(10_4_005, "Drop column 'system_tags' in the 'rules' table", DropSystemTagsInRules.class)
      .add(10_4_006, "Add 'uuid' column to 'groups_users'", AddUuidColumnToGroupsUsers.class)
      .add(10_4_007, "Populate 'uuid' column in 'groups_users'", PopulateGroupsUsersUuid.class)
      .add(10_4_008, "Make 'uuid' column in 'groups_users' table non-nullable", MakeUuidInGroupsUsersNotNullable.class)
      .add(10_4_009, "Create primary key on 'groups_users.uuid'", CreatePrimaryKeyOnGroupsUsersTable.class);
  }
}
