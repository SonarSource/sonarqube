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

import static org.sonar.server.platform.db.migration.version.v202605.CreateOrganizationConfigsTable.COLUMN_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateOrganizationConfigsTable.COLUMN_IS_BETA_ENROLLED;
import static org.sonar.server.platform.db.migration.version.v202605.CreateOrganizationConfigsTable.COLUMN_IS_ENABLED;
import static org.sonar.server.platform.db.migration.version.v202605.CreateOrganizationConfigsTable.COLUMN_ORGANIZATION_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateOrganizationConfigsTable.ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateOrganizationConfigsTable.INDEX_ORGANIZATION_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateOrganizationConfigsTable.ORGANIZATION_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateOrganizationConfigsTable.TABLE_NAME;

class CreateOrganizationConfigsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateOrganizationConfigsTable.class);

  private final CreateOrganizationConfigsTable underTest = new CreateOrganizationConfigsTable(db.database());

  @Test
  void migration_should_create_table_and_columns() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_organization_configs", COLUMN_ID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ID, Types.VARCHAR, ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ORGANIZATION_ID, Types.VARCHAR, ORGANIZATION_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_IS_ENABLED, Types.BOOLEAN, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_IS_BETA_ENROLLED, Types.BOOLEAN, null, false);
  }

  @Test
  void migration_should_create_unique_index() throws SQLException {
    underTest.execute();

    db.assertUniqueIndex(TABLE_NAME, INDEX_ORGANIZATION_ID, COLUMN_ORGANIZATION_ID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
