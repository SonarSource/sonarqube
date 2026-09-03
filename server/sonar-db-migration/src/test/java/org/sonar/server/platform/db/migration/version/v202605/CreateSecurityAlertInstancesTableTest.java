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

import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.BRANCH_UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.COLUMN_BRANCH_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.COLUMN_ORGANIZATION_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.COLUMN_PROJECT_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.COLUMN_SECURITY_ALERT_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.COLUMN_SEVERITY;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.COLUMN_STATUS;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.INDEX_BRANCH;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.INDEX_PROJECT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.INDEX_UNIQ;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.ORGANIZATION_UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.PROJECT_UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.SECURITY_ALERT_UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.SEVERITY_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.STATUS_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertInstancesTable.UUID_SIZE;

class CreateSecurityAlertInstancesTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateSecurityAlertInstancesTable.class);

  private final CreateSecurityAlertInstancesTable underTest = new CreateSecurityAlertInstancesTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_security_alert_instances", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SECURITY_ALERT_UUID, Types.VARCHAR, SECURITY_ALERT_UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ORGANIZATION_UUID, Types.VARCHAR, ORGANIZATION_UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BRANCH_UUID, Types.VARCHAR, BRANCH_UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PROJECT_UUID, Types.VARCHAR, PROJECT_UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_STATUS, Types.VARCHAR, STATUS_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SEVERITY, Types.VARCHAR, SEVERITY_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertUniqueIndex(TABLE_NAME, INDEX_UNIQ, COLUMN_SECURITY_ALERT_UUID, COLUMN_ORGANIZATION_UUID, COLUMN_BRANCH_UUID);
    db.assertIndex(TABLE_NAME, INDEX_BRANCH, COLUMN_BRANCH_UUID);
    db.assertIndex(TABLE_NAME, INDEX_PROJECT, COLUMN_PROJECT_UUID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
