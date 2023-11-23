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

import static java.sql.Types.VARCHAR;
import static org.sonar.server.platform.db.migration.version.v104.AddUuidColumnToGroupsUsers.GROUPS_USERS_TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v104.AddUuidColumnToGroupsUsers.GROUPS_USERS_UUID_COLUMN_NAME;

public class MakeUuidInGroupsUsersNotNullableIT {

  @Rule
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep( MakeUuidInGroupsUsersNotNullable.class);
  private final  MakeUuidInGroupsUsersNotNullable underTest = new  MakeUuidInGroupsUsersNotNullable(db.database());

  @Test
  public void execute_whenUuidColumnIsNullable_shouldMakeItNonNullable() throws SQLException {
    db.assertColumnDefinition(GROUPS_USERS_TABLE_NAME, GROUPS_USERS_UUID_COLUMN_NAME, VARCHAR, null, true);
    underTest.execute();
    db.assertColumnDefinition(GROUPS_USERS_TABLE_NAME, GROUPS_USERS_UUID_COLUMN_NAME, VARCHAR, null, false);
  }

  @Test
  public void execute_whenUuidColumnIsNullable_shouldKeepItNullableAndNotFail() throws SQLException {
    db.assertColumnDefinition(GROUPS_USERS_TABLE_NAME, GROUPS_USERS_UUID_COLUMN_NAME, VARCHAR, null, true);
    underTest.execute();
    underTest.execute();
    db.assertColumnDefinition(GROUPS_USERS_TABLE_NAME, GROUPS_USERS_UUID_COLUMN_NAME, VARCHAR, null, false);
  }
}
