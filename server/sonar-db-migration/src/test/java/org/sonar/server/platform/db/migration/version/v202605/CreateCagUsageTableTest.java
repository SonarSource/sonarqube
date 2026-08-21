/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202605;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202605.CreateCagUsageTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateCagUsageTable.COLUMN_INVOCATION_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateCagUsageTable.COLUMN_PROJECT_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateCagUsageTable.COLUMN_USER_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateCagUsageTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateCagUsageTable.INDEX_CREATED_PROJECT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateCagUsageTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateCagUsageTable.UUID_SIZE;

class CreateCagUsageTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateCagUsageTable.class);

  private final CreateCagUsageTable underTest = new CreateCagUsageTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_cag_usage", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_INVOCATION_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_USER_UUID, Types.VARCHAR, UUID_SIZE, true);
    db.assertIndex(TABLE_NAME, INDEX_CREATED_PROJECT, COLUMN_CREATED_AT, COLUMN_PROJECT_UUID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
