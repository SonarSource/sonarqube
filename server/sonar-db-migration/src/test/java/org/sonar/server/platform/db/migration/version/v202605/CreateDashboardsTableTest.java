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

class CreateDashboardsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateDashboardsTable.class);

  private final CreateDashboardsTable underTest = new CreateDashboardsTable(db.database());

  @Test
  void migration_should_create_dashboard_schema() throws SQLException {
    underTest.execute();

    db.assertTableExists(CreateDashboardsTable.TABLE_NAME);
    db.assertPrimaryKey(CreateDashboardsTable.TABLE_NAME, "pk_dashboards", CreateDashboardsTable.ID);
    db.assertColumnDefinition(CreateDashboardsTable.TABLE_NAME, CreateDashboardsTable.ID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(CreateDashboardsTable.TABLE_NAME, CreateDashboardsTable.NAME, Types.VARCHAR, 300, false);
    db.assertColumnDefinition(CreateDashboardsTable.TABLE_NAME, CreateDashboardsTable.LAYOUT_STATE, Types.CLOB, null, false);
    db.assertColumnDefinition(CreateDashboardsTable.TABLE_NAME, CreateDashboardsTable.DESCRIPTION, Types.VARCHAR, 500, true);
    db.assertColumnDefinition(CreateDashboardsTable.TABLE_NAME, CreateDashboardsTable.CREATED_AT, Types.TIMESTAMP, null, false);
    db.assertColumnDefinition(CreateDashboardsTable.TABLE_NAME, CreateDashboardsTable.RESOURCE_TYPE, Types.VARCHAR, 40, false);
    db.assertIndex(CreateDashboardsTable.TABLE_NAME, CreateDashboardsTable.DASHBOARDS_NAME_RESOURCE_IDX,
      CreateDashboardsTable.NAME, CreateDashboardsTable.RESOURCE_TYPE, CreateDashboardsTable.RESOURCE_ID);
  }
}
