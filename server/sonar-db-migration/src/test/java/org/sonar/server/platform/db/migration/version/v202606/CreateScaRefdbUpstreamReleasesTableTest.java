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
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamReleasesTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamReleasesTable.COLUMN_PACKAGE_LOOKUP_KEY;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamReleasesTable.COLUMN_PUBLISHED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamReleasesTable.COLUMN_RAW_LICENSE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamReleasesTable.COLUMN_SOURCE_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamReleasesTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamReleasesTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamReleasesTable.COLUMN_VERSION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamReleasesTable.COLUMN_YANKED;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamReleasesTable.INDEX_UNIQ;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamReleasesTable.TABLE_NAME;

class CreateScaRefdbUpstreamReleasesTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateScaRefdbUpstreamReleasesTable.class);

  private final CreateScaRefdbUpstreamReleasesTable underTest = new CreateScaRefdbUpstreamReleasesTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_sca_ref_upstream_releases", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PACKAGE_LOOKUP_KEY, Types.VARCHAR, PACKAGE_LOOKUP_KEY_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VERSION, Types.VARCHAR, VERSION_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PUBLISHED_AT, Types.BIGINT, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_RAW_LICENSE, Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_YANKED, Types.BOOLEAN, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SOURCE_UPDATED_AT, Types.BIGINT, null, false);
    db.assertUniqueIndex(TABLE_NAME, INDEX_UNIQ, COLUMN_PACKAGE_LOOKUP_KEY, COLUMN_VERSION);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
