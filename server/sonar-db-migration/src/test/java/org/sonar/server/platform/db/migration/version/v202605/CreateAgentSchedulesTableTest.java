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

import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.COLUMN_DAY_OF_WEEK;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.COLUMN_ENABLED;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.COLUMN_FREQUENCY;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.COLUMN_MAX_ISSUES_PER_RUN;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.COLUMN_MAX_OPEN_PRS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.COLUMN_NEXT_RUN_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.COLUMN_PROJECT_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.COLUMN_PROJECT_SELECTION_MODE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.COLUMN_RUN_HOUR;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.COLUMN_TIMEZONE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.DAY_OF_WEEK_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.FREQUENCY_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.INDEX_DUE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.PROJECT_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.PROJECT_SELECTION_MODE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateAgentSchedulesTable.TIMEZONE_SIZE;

class CreateAgentSchedulesTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateAgentSchedulesTable.class);

  private final CreateAgentSchedulesTable underTest = new CreateAgentSchedulesTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_agent_schedules", COLUMN_PROJECT_ID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_ID, Types.VARCHAR, PROJECT_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ENABLED, Types.BOOLEAN, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NEXT_RUN_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_FREQUENCY, Types.VARCHAR, FREQUENCY_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_DAY_OF_WEEK, Types.VARCHAR, DAY_OF_WEEK_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_RUN_HOUR, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_TIMEZONE, Types.VARCHAR, TIMEZONE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_SELECTION_MODE, Types.VARCHAR, PROJECT_SELECTION_MODE_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_MAX_ISSUES_PER_RUN, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_MAX_OPEN_PRS, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertIndex(TABLE_NAME, INDEX_DUE, COLUMN_NEXT_RUN_AT);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
