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

import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.COLUMN_CONSECUTIVE_FAILURES;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.COLUMN_EXECUTION_TIME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.COLUMN_LAST_FAILURE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.COLUMN_LAST_HEARTBEAT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.COLUMN_LAST_SUCCESS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.COLUMN_PICKED;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.COLUMN_PICKED_BY;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.COLUMN_TASK_DATA;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.COLUMN_TASK_INSTANCE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.COLUMN_TASK_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.COLUMN_VERSION;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.INDEX_EXECUTION_TIME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.INDEX_LAST_HEARTBEAT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.PICKED_BY_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.TASK_INSTANCE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateHunterScheduledTasksTable.TASK_NAME_SIZE;

class CreateHunterScheduledTasksTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateHunterScheduledTasksTable.class);

  private final CreateHunterScheduledTasksTable underTest = new CreateHunterScheduledTasksTable(db.database());

  @Test
  void migration_should_create_table_and_columns() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_" + TABLE_NAME, COLUMN_TASK_NAME, COLUMN_TASK_INSTANCE);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_TASK_NAME, Types.VARCHAR, TASK_NAME_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_TASK_INSTANCE, Types.VARCHAR, TASK_INSTANCE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_TASK_DATA, Types.BLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_EXECUTION_TIME, Types.TIMESTAMP, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PICKED, Types.BOOLEAN, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PICKED_BY, Types.VARCHAR, PICKED_BY_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LAST_SUCCESS, Types.TIMESTAMP, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LAST_FAILURE, Types.TIMESTAMP, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CONSECUTIVE_FAILURES, Types.INTEGER, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LAST_HEARTBEAT, Types.TIMESTAMP, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VERSION, Types.BIGINT, null, false);
  }

  @Test
  void migration_should_create_execution_time_index() throws SQLException {
    underTest.execute();

    db.assertIndex(TABLE_NAME, INDEX_EXECUTION_TIME, COLUMN_EXECUTION_TIME);
  }

  @Test
  void migration_should_create_last_heartbeat_index() throws SQLException {
    underTest.execute();

    db.assertIndex(TABLE_NAME, INDEX_LAST_HEARTBEAT, COLUMN_LAST_HEARTBEAT);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
