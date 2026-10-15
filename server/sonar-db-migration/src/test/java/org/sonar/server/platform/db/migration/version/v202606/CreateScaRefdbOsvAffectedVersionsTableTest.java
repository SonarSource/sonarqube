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

import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_LOOKUP_KEY_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VERSION_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.DATABASE_PREFIX_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.ECOSYSTEM_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_NAME_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_PURL_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedVersionsTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedVersionsTable.COLUMN_DATABASE_PREFIX;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedVersionsTable.COLUMN_ECOSYSTEM;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedVersionsTable.COLUMN_OSV_VULNERABILITY_UUID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedVersionsTable.COLUMN_PACKAGE_LOOKUP_KEY;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedVersionsTable.COLUMN_PACKAGE_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedVersionsTable.COLUMN_PACKAGE_PURL;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedVersionsTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedVersionsTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedVersionsTable.COLUMN_VERSION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedVersionsTable.INDEX_PACKAGE_VERSION_PREFIX;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedVersionsTable.INDEX_UNIQ;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedVersionsTable.TABLE_NAME;

class CreateScaRefdbOsvAffectedVersionsTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateScaRefdbOsvAffectedVersionsTable.class);

  private final CreateScaRefdbOsvAffectedVersionsTable underTest = new CreateScaRefdbOsvAffectedVersionsTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_sca_ref_osv_aff_versions", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_OSV_VULNERABILITY_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ECOSYSTEM, Types.VARCHAR, ECOSYSTEM_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PACKAGE_NAME, Types.VARCHAR, PACKAGE_NAME_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PACKAGE_PURL, Types.VARCHAR, PACKAGE_PURL_MAX_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VERSION, Types.VARCHAR, VERSION_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PACKAGE_LOOKUP_KEY, Types.VARCHAR, PACKAGE_LOOKUP_KEY_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_DATABASE_PREFIX, Types.VARCHAR, DATABASE_PREFIX_MAX_SIZE, true);
    db.assertUniqueIndex(TABLE_NAME, INDEX_UNIQ, COLUMN_OSV_VULNERABILITY_UUID, COLUMN_PACKAGE_NAME, COLUMN_ECOSYSTEM,
      COLUMN_VERSION);
    db.assertIndex(TABLE_NAME, INDEX_PACKAGE_VERSION_PREFIX, COLUMN_PACKAGE_LOOKUP_KEY, COLUMN_VERSION, COLUMN_DATABASE_PREFIX);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
