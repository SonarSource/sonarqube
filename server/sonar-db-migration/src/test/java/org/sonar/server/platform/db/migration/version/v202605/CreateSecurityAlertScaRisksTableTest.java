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

import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertScaRisksTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertScaRisksTable.COLUMN_ISSUE_DESCRIPTION;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertScaRisksTable.COLUMN_SCA_ISSUE_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertScaRisksTable.COLUMN_SECURITY_ALERT_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertScaRisksTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertScaRisksTable.INDEX_SCA_ISSUE_UUID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertScaRisksTable.ISSUE_DESCRIPTION_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertScaRisksTable.SCA_ISSUE_UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertScaRisksTable.SECURITY_ALERT_UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateSecurityAlertScaRisksTable.TABLE_NAME;

class CreateSecurityAlertScaRisksTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateSecurityAlertScaRisksTable.class);

  private final CreateSecurityAlertScaRisksTable underTest = new CreateSecurityAlertScaRisksTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_security_alert_sca_risks", COLUMN_SECURITY_ALERT_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SECURITY_ALERT_UUID, Types.VARCHAR, SECURITY_ALERT_UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SCA_ISSUE_UUID, Types.VARCHAR, SCA_ISSUE_UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ISSUE_DESCRIPTION, Types.VARCHAR, ISSUE_DESCRIPTION_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertUniqueIndex(TABLE_NAME, INDEX_SCA_ISSUE_UUID, COLUMN_SCA_ISSUE_UUID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
