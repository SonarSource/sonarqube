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
package org.sonar.server.platform.db.migration.version.v202506;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202506.CreateUserBindingsTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202506.CreateUserBindingsTable.COLUMN_INTEGRATION_CONFIG_UUID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateUserBindingsTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202506.CreateUserBindingsTable.COLUMN_USER_UUID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateUserBindingsTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateUserBindingsTable.USER_BINDINGS_TABLE_NAME;

class CreateUserBindingsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateUserBindingsTable.class);

  private final CreateUserBindingsTable underTest = new CreateUserBindingsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(USER_BINDINGS_TABLE_NAME);

    underTest.execute();

    db.assertTableExists(USER_BINDINGS_TABLE_NAME);
    db.assertPrimaryKey(USER_BINDINGS_TABLE_NAME, "pk_user_bindings", COLUMN_UUID);
    db.assertColumnDefinition(USER_BINDINGS_TABLE_NAME, COLUMN_UUID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(USER_BINDINGS_TABLE_NAME, COLUMN_USER_UUID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(USER_BINDINGS_TABLE_NAME, COLUMN_INTEGRATION_CONFIG_UUID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(USER_BINDINGS_TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(USER_BINDINGS_TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertUniqueIndex(USER_BINDINGS_TABLE_NAME, "ub_user_integration_config", COLUMN_USER_UUID, COLUMN_INTEGRATION_CONFIG_UUID);
    db.assertIndex(USER_BINDINGS_TABLE_NAME, "ub_integration_config_uuid", COLUMN_INTEGRATION_CONFIG_UUID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(USER_BINDINGS_TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(USER_BINDINGS_TABLE_NAME);
  }

}
