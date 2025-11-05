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

import static org.sonar.server.platform.db.migration.version.v202506.CreateIntegrationConfigurationsTable.COLUMN_APP_ID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIntegrationConfigurationsTable.COLUMN_CLIENT_ID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIntegrationConfigurationsTable.COLUMN_CLIENT_SECRET;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIntegrationConfigurationsTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIntegrationConfigurationsTable.COLUMN_INTEGRATION_TYPE;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIntegrationConfigurationsTable.COLUMN_SIGNING_SECRET;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIntegrationConfigurationsTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIntegrationConfigurationsTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202506.CreateIntegrationConfigurationsTable.INTEGRATION_CONFIGURATIONS_TABLE_NAME;

class CreateIntegrationConfigurationsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateIntegrationConfigurationsTable.class);

  private final CreateIntegrationConfigurationsTable underTest = new CreateIntegrationConfigurationsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(INTEGRATION_CONFIGURATIONS_TABLE_NAME);

    underTest.execute();

    db.assertTableExists(INTEGRATION_CONFIGURATIONS_TABLE_NAME);
    db.assertPrimaryKey(INTEGRATION_CONFIGURATIONS_TABLE_NAME, "pk_integration_configs", COLUMN_UUID);
    db.assertColumnDefinition(INTEGRATION_CONFIGURATIONS_TABLE_NAME, COLUMN_UUID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(INTEGRATION_CONFIGURATIONS_TABLE_NAME, COLUMN_INTEGRATION_TYPE, Types.VARCHAR, 20, false);
    db.assertColumnDefinition(INTEGRATION_CONFIGURATIONS_TABLE_NAME, COLUMN_CLIENT_ID, Types.VARCHAR, 255, true);
    db.assertColumnDefinition(INTEGRATION_CONFIGURATIONS_TABLE_NAME, COLUMN_CLIENT_SECRET, Types.VARCHAR, 255, true);
    db.assertColumnDefinition(INTEGRATION_CONFIGURATIONS_TABLE_NAME, COLUMN_SIGNING_SECRET, Types.VARCHAR, 255, true);
    db.assertColumnDefinition(INTEGRATION_CONFIGURATIONS_TABLE_NAME, COLUMN_APP_ID, Types.VARCHAR, 255, true);
    db.assertColumnDefinition(INTEGRATION_CONFIGURATIONS_TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(INTEGRATION_CONFIGURATIONS_TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(INTEGRATION_CONFIGURATIONS_TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(INTEGRATION_CONFIGURATIONS_TABLE_NAME);
  }

}
