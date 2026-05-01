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
package org.sonar.server.platform.db.migration.version.v202603;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202603.CreateAdminAlertStatusTable.ALERT_KEY_SIZE;
import static org.sonar.server.platform.db.migration.version.v202603.CreateAdminAlertStatusTable.COLUMN_ACTIVATED_AT;
import static org.sonar.server.platform.db.migration.version.v202603.CreateAdminAlertStatusTable.COLUMN_ALERT_KEY;
import static org.sonar.server.platform.db.migration.version.v202603.CreateAdminAlertStatusTable.COLUMN_DEACTIVATED_AT;
import static org.sonar.server.platform.db.migration.version.v202603.CreateAdminAlertStatusTable.COLUMN_IS_ACTIVE;
import static org.sonar.server.platform.db.migration.version.v202603.CreateAdminAlertStatusTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202603.CreateAdminAlertStatusTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202603.CreateAdminAlertStatusTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202603.CreateAdminAlertStatusTable.UUID_SIZE;

class CreateAdminAlertStatusTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateAdminAlertStatusTable.class);

  private final CreateAdminAlertStatusTable underTest = new CreateAdminAlertStatusTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_admin_alert_status", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ALERT_KEY, Types.VARCHAR, ALERT_KEY_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_IS_ACTIVE, Types.BOOLEAN, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ACTIVATED_AT, Types.BIGINT, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_DEACTIVATED_AT, Types.BIGINT, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertIndex(TABLE_NAME, "idx_admin_alert_status_key", COLUMN_ALERT_KEY);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
