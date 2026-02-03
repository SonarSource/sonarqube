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

import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.COLUMN_CVE_ID;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.COLUMN_END_LINE;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.COLUMN_END_LINE_OFFSET;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.COLUMN_FILE_PATH;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.COLUMN_SCA_ISSUES_RELEASES_UUID;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.COLUMN_SIGNATURE;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.COLUMN_START_LINE;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.COLUMN_START_LINE_OFFSET;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.CVE_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.FILE_PATH_SIZE;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.SIGNATURE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202601.CreateScaIrCveLocationsTable.TABLE_NAME;

class CreateScaIrCveLocationsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateScaIrCveLocationsTable.class);

  private final CreateScaIrCveLocationsTable underTest = new CreateScaIrCveLocationsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_sca_ir_cve_locations", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SCA_ISSUES_RELEASES_UUID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CVE_ID, Types.VARCHAR, CVE_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SIGNATURE, Types.VARCHAR, SIGNATURE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_FILE_PATH, Types.VARCHAR, FILE_PATH_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_START_LINE, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_START_LINE_OFFSET, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_END_LINE, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_END_LINE_OFFSET, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);

    db.assertIndex(TABLE_NAME, "sca_ir_cve_loc_ir_uuid", COLUMN_SCA_ISSUES_RELEASES_UUID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }

}
