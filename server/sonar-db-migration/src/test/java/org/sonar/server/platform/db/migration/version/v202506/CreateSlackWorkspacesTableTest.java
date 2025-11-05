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

import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackWorkspacesTable.COLUMN_ACCESS_TOKEN;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackWorkspacesTable.COLUMN_BOT_USER_ID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackWorkspacesTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackWorkspacesTable.COLUMN_INTEGRATION_CONFIG_UUID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackWorkspacesTable.COLUMN_REFRESH_TOKEN;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackWorkspacesTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackWorkspacesTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackWorkspacesTable.COLUMN_WORKSPACE_ID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackWorkspacesTable.COLUMN_WORKSPACE_NAME;
import static org.sonar.server.platform.db.migration.version.v202506.CreateSlackWorkspacesTable.SLACK_WORKSPACES_TABLE_NAME;

class CreateSlackWorkspacesTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateSlackWorkspacesTable.class);

  private final CreateSlackWorkspacesTable underTest = new CreateSlackWorkspacesTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(SLACK_WORKSPACES_TABLE_NAME);

    underTest.execute();

    db.assertTableExists(SLACK_WORKSPACES_TABLE_NAME);
    db.assertPrimaryKey(SLACK_WORKSPACES_TABLE_NAME, "pk_slack_workspaces", COLUMN_UUID);
    db.assertColumnDefinition(SLACK_WORKSPACES_TABLE_NAME, COLUMN_UUID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(SLACK_WORKSPACES_TABLE_NAME, COLUMN_INTEGRATION_CONFIG_UUID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(SLACK_WORKSPACES_TABLE_NAME, COLUMN_WORKSPACE_ID, Types.VARCHAR, 255, false);
    db.assertColumnDefinition(SLACK_WORKSPACES_TABLE_NAME, COLUMN_WORKSPACE_NAME, Types.VARCHAR, 255, true);
    db.assertColumnDefinition(SLACK_WORKSPACES_TABLE_NAME, COLUMN_ACCESS_TOKEN, Types.VARCHAR, 2000, true);
    db.assertColumnDefinition(SLACK_WORKSPACES_TABLE_NAME, COLUMN_REFRESH_TOKEN, Types.VARCHAR, 2000, true);
    db.assertColumnDefinition(SLACK_WORKSPACES_TABLE_NAME, COLUMN_BOT_USER_ID, Types.VARCHAR, 255, true);
    db.assertColumnDefinition(SLACK_WORKSPACES_TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(SLACK_WORKSPACES_TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);

    db.assertUniqueIndex(SLACK_WORKSPACES_TABLE_NAME, "sw_workspace_id", COLUMN_WORKSPACE_ID);
    db.assertIndex(SLACK_WORKSPACES_TABLE_NAME, "sw_integration_config_uuid", COLUMN_INTEGRATION_CONFIG_UUID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(SLACK_WORKSPACES_TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(SLACK_WORKSPACES_TABLE_NAME);
  }

}
