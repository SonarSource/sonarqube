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
package org.sonar.server.platform.db.migration.version.v101;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;
import org.sonar.server.platform.db.migration.version.v100.AddIsMainColumnInProjectBranches;
import org.sonar.server.platform.db.migration.version.v100.AlterIsMainColumnInProjectBranches;
import org.sonar.server.platform.db.migration.version.v100.UpdateIsMainColumnInProjectBranches;

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
      .add(10_1_009, "Add column 'characteristic' to 'rules' table", AddCharacteristicInRules.class);
  }
}
