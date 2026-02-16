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
package org.sonar.server.platform.db.migration.version.v202601;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.COLUMN_ACCESS_TOKEN;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.COLUMN_ENTITY_TYPE;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.COLUMN_ENTITY_UUID;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.COLUMN_EXPIRES_IN_SECONDS;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.COLUMN_EXTRA_DETAILS;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.COLUMN_ID;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.COLUMN_INTEGRATION_TYPE;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.COLUMN_REFRESH_TOKEN;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.COLUMN_SCOPE;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.COLUMN_TOKEN_TYPE;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.COLUMN_UPDATED_BY;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.ENTITY_TYPE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.INTEGRATION_TYPE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.TOKEN_TYPE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202601.CreateIntegrationBindingsTable.UUID_SIZE;

class CreateIntegrationBindingsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateIntegrationBindingsTable.class);

  private final CreateIntegrationBindingsTable underTest = new CreateIntegrationBindingsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_integration_bindings", COLUMN_ID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_INTEGRATION_TYPE, Types.VARCHAR, INTEGRATION_TYPE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ENTITY_TYPE, Types.VARCHAR, ENTITY_TYPE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ENTITY_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_TOKEN_TYPE, Types.VARCHAR, TOKEN_TYPE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_BY, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ACCESS_TOKEN, Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_REFRESH_TOKEN, Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SCOPE, Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_EXPIRES_IN_SECONDS, Types.BIGINT, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_EXTRA_DETAILS, Types.CLOB, null, true);

    db.assertUniqueIndex(TABLE_NAME, "integration_bindings_idx", COLUMN_INTEGRATION_TYPE, COLUMN_ENTITY_TYPE, COLUMN_ENTITY_UUID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }

}
