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

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.USER_UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v101.CreateScmAccountsTable.SCM_ACCOUNTS_TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v101.CreateScmAccountsTable.SCM_ACCOUNT_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v101.CreateScmAccountsTable.SCM_ACCOUNT_SIZE;
import static org.sonar.server.platform.db.migration.version.v101.CreateScmAccountsTable.USER_UUID_COLUMN_NAME;

class CreateScmAccountsTableIT {
  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateScmAccountsTable.class);

  private final DdlChange createScmAccountsTable = new CreateScmAccountsTable(db.database());

  @Test
  void migration_should_create_a_table() throws SQLException {
    db.assertTableDoesNotExist(SCM_ACCOUNTS_TABLE_NAME);

    createScmAccountsTable.execute();

    db.assertTableExists(SCM_ACCOUNTS_TABLE_NAME);
    db.assertColumnDefinition(SCM_ACCOUNTS_TABLE_NAME, USER_UUID_COLUMN_NAME, Types.VARCHAR, USER_UUID_SIZE, false);
    db.assertColumnDefinition(SCM_ACCOUNTS_TABLE_NAME, SCM_ACCOUNT_COLUMN_NAME, Types.VARCHAR, SCM_ACCOUNT_SIZE, false);
    db.assertPrimaryKey(SCM_ACCOUNTS_TABLE_NAME, "pk_scm_accounts", USER_UUID_COLUMN_NAME, SCM_ACCOUNT_COLUMN_NAME);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(SCM_ACCOUNTS_TABLE_NAME);

    createScmAccountsTable.execute();
    // re-entrant
    createScmAccountsTable.execute();

    db.assertTableExists(SCM_ACCOUNTS_TABLE_NAME);
  }
}
