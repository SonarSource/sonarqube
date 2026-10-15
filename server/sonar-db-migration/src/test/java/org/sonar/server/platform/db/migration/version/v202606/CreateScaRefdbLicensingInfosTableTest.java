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

import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.CATEGORIZED_SOURCE_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.COLUMN_CATEGORIZED_SOURCE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.COLUMN_LICENSE_EXPRESSION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.COLUMN_ORIGINAL_LICENSE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.COLUMN_ORIGINAL_LICENSE_SOURCE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.COLUMN_PACKAGE_LOOKUP_KEY;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.COLUMN_REPOSITORY_LICENSE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.COLUMN_VERSION_RANGE_END;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.COLUMN_VERSION_RANGE_START;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.COLUMN_VERSION_TYPE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.INDEX_PKG_LOOKUP_KEY;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.ORIGINAL_LICENSE_SOURCE_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_LOOKUP_KEY_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbLicensingInfosTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VERSION_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VERSION_TYPE_MAX_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

class CreateScaRefdbLicensingInfosTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateScaRefdbLicensingInfosTable.class);

  private final CreateScaRefdbLicensingInfosTable underTest = new CreateScaRefdbLicensingInfosTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_sca_ref_licensing_infos", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PACKAGE_LOOKUP_KEY, Types.VARCHAR, PACKAGE_LOOKUP_KEY_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VERSION_TYPE, Types.VARCHAR, VERSION_TYPE_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VERSION_RANGE_START, Types.VARCHAR, VERSION_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VERSION_RANGE_END, Types.VARCHAR, VERSION_MAX_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CATEGORIZED_SOURCE, Types.VARCHAR, CATEGORIZED_SOURCE_MAX_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_LICENSE_EXPRESSION, Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ORIGINAL_LICENSE, Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ORIGINAL_LICENSE_SOURCE, Types.VARCHAR, ORIGINAL_LICENSE_SOURCE_MAX_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_REPOSITORY_LICENSE, Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertIndex(TABLE_NAME, INDEX_PKG_LOOKUP_KEY, COLUMN_PACKAGE_LOOKUP_KEY);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
