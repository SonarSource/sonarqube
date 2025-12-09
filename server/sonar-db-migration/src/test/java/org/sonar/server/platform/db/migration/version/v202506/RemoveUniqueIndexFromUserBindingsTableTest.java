/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202506.CreateUserBindingsTable.COLUMN_INTEGRATION_CONFIG_UUID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateUserBindingsTable.COLUMN_USER_UUID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateUserBindingsTable.USER_BINDINGS_TABLE_NAME;

class RemoveUniqueIndexFromUserBindingsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(RemoveUniqueIndexFromUserBindingsTable.class);

  private final CreateUserBindingsTable createTableMigration = new CreateUserBindingsTable(db.database());
  private final RemoveUniqueIndexFromUserBindingsTable underTest = new RemoveUniqueIndexFromUserBindingsTable(db.database());

  @Test
  void migration_should_remove_unique_index() throws SQLException {
    createTableMigration.execute();
    db.assertUniqueIndex(USER_BINDINGS_TABLE_NAME, "ub_user_integration_config", COLUMN_USER_UUID, COLUMN_INTEGRATION_CONFIG_UUID);

    underTest.execute();

    db.assertIndexDoesNotExist(USER_BINDINGS_TABLE_NAME, "ub_user_integration_config");
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    createTableMigration.execute();

    underTest.execute();
    underTest.execute();

    db.assertIndexDoesNotExist(USER_BINDINGS_TABLE_NAME, "ub_user_integration_config");
  }

}