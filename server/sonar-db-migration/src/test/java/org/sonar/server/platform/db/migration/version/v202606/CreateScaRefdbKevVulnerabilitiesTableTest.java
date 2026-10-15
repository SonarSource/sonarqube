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
package org.sonar.server.platform.db.migration.version.v202606;

import java.sql.SQLException;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VULNERABILITY_ID_MAX_LENGTH;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_CWES;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_DATE_ADDED;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_DUE_DATE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_KNOWN_RANSOMWARE_CAMPAIGN_USE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_NOTES;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_PRODUCT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_REQUIRED_ACTION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_SHORT_DESCRIPTION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_VENDOR_PROJECT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_VULNERABILITY_ID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.COLUMN_VULNERABILITY_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.INDEX_VULN_ID_UNIQ;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.KNOWN_RANSOMWARE_CAMPAIGN_USE_MAX_LENGTH;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.PRODUCT_MAX_LENGTH;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.VENDOR_PROJECT_MAX_LENGTH;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbKevVulnerabilitiesTable.VULNERABILITY_NAME_MAX_LENGTH;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

class CreateScaRefdbKevVulnerabilitiesTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateScaRefdbKevVulnerabilitiesTable.class);

  private final CreateScaRefdbKevVulnerabilitiesTable underTest = new CreateScaRefdbKevVulnerabilitiesTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_sca_ref_kev_vulns", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VULNERABILITY_ID, Types.VARCHAR, VULNERABILITY_ID_MAX_LENGTH, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CWES, Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_DATE_ADDED, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_DUE_DATE, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_KNOWN_RANSOMWARE_CAMPAIGN_USE, Types.VARCHAR, KNOWN_RANSOMWARE_CAMPAIGN_USE_MAX_LENGTH, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_NOTES, Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PRODUCT, Types.VARCHAR, PRODUCT_MAX_LENGTH, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_REQUIRED_ACTION, Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SHORT_DESCRIPTION, Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VENDOR_PROJECT, Types.VARCHAR, VENDOR_PROJECT_MAX_LENGTH, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VULNERABILITY_NAME, Types.VARCHAR, VULNERABILITY_NAME_MAX_LENGTH, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertUniqueIndex(TABLE_NAME, INDEX_VULN_ID_UNIQ, COLUMN_VULNERABILITY_ID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
