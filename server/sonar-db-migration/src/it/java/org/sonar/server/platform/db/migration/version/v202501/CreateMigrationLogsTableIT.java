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
package org.sonar.server.platform.db.migration.version.v202501;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202501.CreateMigrationLogsTable.COLUMN_DURATION_IN_MS;
import static org.sonar.server.platform.db.migration.version.v202501.CreateMigrationLogsTable.COLUMN_STARTED_AT;
import static org.sonar.server.platform.db.migration.version.v202501.CreateMigrationLogsTable.COLUMN_STEP;
import static org.sonar.server.platform.db.migration.version.v202501.CreateMigrationLogsTable.COLUMN_SUCCESS;
import static org.sonar.server.platform.db.migration.version.v202501.CreateMigrationLogsTable.COLUMN_TARGET_VERSION;
import static org.sonar.server.platform.db.migration.version.v202501.CreateMigrationLogsTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202501.CreateMigrationLogsTable.MIGRATION_LOGS_TABLE_NAME;

class CreateMigrationLogsTableIT {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateMigrationLogsTable.class);

  private final DdlChange underTest = new CreateMigrationLogsTable(db.database());

  @Test
  void migration_should_create_a_table() throws SQLException {
    db.assertTableDoesNotExist(MIGRATION_LOGS_TABLE_NAME);

    underTest.execute();

    db.assertTableExists(MIGRATION_LOGS_TABLE_NAME);
    db.assertColumnDefinition(MIGRATION_LOGS_TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(MIGRATION_LOGS_TABLE_NAME, COLUMN_STEP, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(MIGRATION_LOGS_TABLE_NAME, COLUMN_DURATION_IN_MS, Types.BIGINT, null, false);
    db.assertColumnDefinition(MIGRATION_LOGS_TABLE_NAME, COLUMN_SUCCESS, Types.BOOLEAN, null, false);
    db.assertColumnDefinition(MIGRATION_LOGS_TABLE_NAME, COLUMN_STARTED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(MIGRATION_LOGS_TABLE_NAME, COLUMN_TARGET_VERSION, Types.VARCHAR, 40, false);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(MIGRATION_LOGS_TABLE_NAME);

    underTest.execute();
    // re-entrant
    underTest.execute();

    db.assertTableExists(MIGRATION_LOGS_TABLE_NAME);
  }
}
