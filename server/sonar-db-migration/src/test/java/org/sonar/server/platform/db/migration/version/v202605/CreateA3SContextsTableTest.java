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

import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.BRANCH_NAME_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.COLUMN_ANALYSIS_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.COLUMN_BRANCH_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.COLUMN_BRANCH_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.COLUMN_EXPIRES_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.COLUMN_KIND;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.COLUMN_PROJECT_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.KIND_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.UUID_SIZE;

class CreateA3SContextsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateA3SContextsTable.class);

  private final CreateA3SContextsTable underTest = new CreateA3SContextsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_a3s_contexts", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ANALYSIS_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BRANCH_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BRANCH_NAME, Types.VARCHAR, BRANCH_NAME_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_KIND, Types.VARCHAR, KIND_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_EXPIRES_AT, Types.BIGINT, null, true);
    db.assertIndex(TABLE_NAME, "idx_a3s_contexts_proj_branch", COLUMN_PROJECT_UUID, COLUMN_BRANCH_NAME);
    db.assertIndex(TABLE_NAME, "idx_a3s_contexts_expires_at", COLUMN_EXPIRES_AT);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
