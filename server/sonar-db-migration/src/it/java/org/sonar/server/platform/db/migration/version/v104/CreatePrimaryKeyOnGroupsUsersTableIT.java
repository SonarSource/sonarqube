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

import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v104.AddUuidColumnToGroupsUsers.GROUPS_USERS_TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v104.AddUuidColumnToGroupsUsers.GROUPS_USERS_UUID_COLUMN_NAME;
import static org.sonar.server.platform.db.migration.version.v104.CreatePrimaryKeyOnGroupsUsersTable.PK_NAME;

public class CreatePrimaryKeyOnGroupsUsersTableIT {
  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreatePrimaryKeyOnGroupsUsersTable.class);
  private final CreatePrimaryKeyOnGroupsUsersTable createIndex = new CreatePrimaryKeyOnGroupsUsersTable(db.database());

  @Test
  public void execute_whenPrimaryKeyDoesntExist_shouldCreatePrimaryKey() throws SQLException {
    db.assertNoPrimaryKey(GROUPS_USERS_TABLE_NAME);

    createIndex.execute();
    db.assertPrimaryKey(GROUPS_USERS_TABLE_NAME, PK_NAME, GROUPS_USERS_UUID_COLUMN_NAME);
  }

  @Test
  public void  execute_whenPrimaryKeyAlreadyExist_shouldKeepThePrimaryKeyAndNotFail() throws SQLException {
    createIndex.execute();
    createIndex.execute();

    db.assertPrimaryKey(GROUPS_USERS_TABLE_NAME, PK_NAME, GROUPS_USERS_UUID_COLUMN_NAME);
  }

}
