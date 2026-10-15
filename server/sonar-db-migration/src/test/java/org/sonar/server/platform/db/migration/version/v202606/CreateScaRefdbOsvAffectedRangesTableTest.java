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

import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_LOOKUP_KEY_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VERSION_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_DATABASE_PREFIX;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_ECOSYSTEM;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_FIXED_VERSION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_INTRODUCED_VERSION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_LAST_AFFECTED_VERSION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_LIMIT_VERSION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_OSV_VULNERABILITY_UUID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_PACKAGE_LOOKUP_KEY;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_PACKAGE_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_PACKAGE_PURL;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_RANGE_TYPE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.DATABASE_PREFIX_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.ECOSYSTEM_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.INDEX_OSV_AFF_RANGES_PKG_PREFIX;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_NAME_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_PURL_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.RANGE_TYPE_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvAffectedRangesTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

class CreateScaRefdbOsvAffectedRangesTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateScaRefdbOsvAffectedRangesTable.class);

  private final CreateScaRefdbOsvAffectedRangesTable underTest = new CreateScaRefdbOsvAffectedRangesTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_sca_ref_osv_aff_ranges", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_OSV_VULNERABILITY_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ECOSYSTEM, Types.VARCHAR, ECOSYSTEM_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PACKAGE_NAME, Types.VARCHAR, PACKAGE_NAME_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PACKAGE_PURL, Types.VARCHAR, PACKAGE_PURL_MAX_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_INTRODUCED_VERSION, Types.VARCHAR, VERSION_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LIMIT_VERSION, Types.VARCHAR, VERSION_MAX_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_FIXED_VERSION, Types.VARCHAR, VERSION_MAX_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LAST_AFFECTED_VERSION, Types.VARCHAR, VERSION_MAX_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_RANGE_TYPE, Types.VARCHAR, RANGE_TYPE_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PACKAGE_LOOKUP_KEY, Types.VARCHAR, PACKAGE_LOOKUP_KEY_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_DATABASE_PREFIX, Types.VARCHAR, DATABASE_PREFIX_MAX_SIZE, true);
    db.assertIndex(TABLE_NAME, INDEX_OSV_AFF_RANGES_PKG_PREFIX, COLUMN_PACKAGE_LOOKUP_KEY, COLUMN_DATABASE_PREFIX);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
