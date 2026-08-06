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

import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_CRON;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_IS_ENABLED;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_IS_SCHEDULING_ENABLED;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_ORGANIZATION_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_ORGANIZATION_LEGACY_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_PROJECT_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_PROJECT_LEGACY_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_SCHEDULING_BRANCH_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.COLUMN_SCHEDULING_TIMEZONE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.CRON_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.INDEX_ORGANIZATION_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.INDEX_PROJECT_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.ORGANIZATION_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.ORGANIZATION_LEGACY_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.PROJECT_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.PROJECT_LEGACY_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.SCHEDULING_BRANCH_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.SCHEDULING_TIMEZONE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateProjectConfigsTable.TABLE_NAME;

class CreateProjectConfigsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateProjectConfigsTable.class);

  private final CreateProjectConfigsTable underTest = new CreateProjectConfigsTable(db.database());

  @Test
  void migration_should_create_table_and_columns() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_project_configs", COLUMN_ID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ID, Types.VARCHAR, ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ORGANIZATION_ID, Types.VARCHAR, ORGANIZATION_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_ID, Types.VARCHAR, PROJECT_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_IS_ENABLED, Types.BOOLEAN, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CRON, Types.VARCHAR, CRON_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_LEGACY_ID, Types.VARCHAR, PROJECT_LEGACY_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ORGANIZATION_LEGACY_ID, Types.VARCHAR, ORGANIZATION_LEGACY_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_IS_SCHEDULING_ENABLED, Types.BOOLEAN, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SCHEDULING_BRANCH_ID, Types.VARCHAR, SCHEDULING_BRANCH_ID_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SCHEDULING_TIMEZONE, Types.VARCHAR, SCHEDULING_TIMEZONE_SIZE, true);
  }

  @Test
  void migration_should_create_indexes() throws SQLException {
    underTest.execute();

    db.assertUniqueIndex(TABLE_NAME, INDEX_PROJECT_ID, COLUMN_PROJECT_ID);
    db.assertIndex(TABLE_NAME, INDEX_ORGANIZATION_ID, COLUMN_ORGANIZATION_ID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
