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

import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.BRANCH_NAME_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.CAG_INSTANCE_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.COLUMN_BRANCH_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.COLUMN_CAG_INSTANCE_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.COLUMN_EVENT_TYPE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.COLUMN_EVENT_VERSION;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.COLUMN_INVOCATION_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.COLUMN_PAYLOAD;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.COLUMN_PROJECT_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.COLUMN_SUCCESS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.COLUMN_TRANSPORT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.COLUMN_USER_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.EVENT_TYPE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.INDEX_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.INDEX_PROJECT_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.TRANSPORT_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateVortexCagEventsTable.UUID_SIZE;

class CreateVortexCagEventsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateVortexCagEventsTable.class);

  private final CreateVortexCagEventsTable underTest = new CreateVortexCagEventsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_vortex_cag_events", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_EVENT_TYPE, Types.VARCHAR, EVENT_TYPE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_EVENT_VERSION, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SUCCESS, Types.BOOLEAN, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_TRANSPORT, Types.VARCHAR, TRANSPORT_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BRANCH_NAME, Types.VARCHAR, BRANCH_NAME_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_USER_UUID, Types.VARCHAR, UUID_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_INVOCATION_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CAG_INSTANCE_ID, Types.VARCHAR, CAG_INSTANCE_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PAYLOAD, Types.CLOB, null, false);
    db.assertIndex(TABLE_NAME, INDEX_PROJECT_CREATED_AT, COLUMN_PROJECT_UUID, COLUMN_CREATED_AT);
    db.assertIndex(TABLE_NAME, INDEX_CREATED_AT, COLUMN_CREATED_AT);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
