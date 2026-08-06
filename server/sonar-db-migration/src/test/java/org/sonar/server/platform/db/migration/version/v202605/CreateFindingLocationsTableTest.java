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

import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.COLUMN_END_COLUMN;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.COLUMN_END_LINE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.COLUMN_FILE_PATH;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.COLUMN_FINDING_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.COLUMN_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.COLUMN_MESSAGE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.COLUMN_START_COLUMN;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.COLUMN_START_LINE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.COLUMN_TYPE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.FILE_PATH_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.FINDING_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.INDEX_FINDING_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.MESSAGE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateFindingLocationsTable.TYPE_SIZE;

class CreateFindingLocationsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateFindingLocationsTable.class);

  private final CreateFindingLocationsTable underTest = new CreateFindingLocationsTable(db.database());

  @Test
  void migration_should_create_table_and_columns() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_finding_locations", COLUMN_ID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ID, Types.VARCHAR, ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_FINDING_ID, Types.VARCHAR, FINDING_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_TYPE, Types.VARCHAR, TYPE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_MESSAGE, Types.VARCHAR, MESSAGE_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_FILE_PATH, Types.VARCHAR, FILE_PATH_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_START_LINE, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_START_COLUMN, Types.INTEGER, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_END_LINE, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_END_COLUMN, Types.INTEGER, null, true);
  }

  @Test
  void migration_should_create_index() throws SQLException {
    underTest.execute();

    db.assertIndex(TABLE_NAME, INDEX_FINDING_ID, COLUMN_FINDING_ID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
