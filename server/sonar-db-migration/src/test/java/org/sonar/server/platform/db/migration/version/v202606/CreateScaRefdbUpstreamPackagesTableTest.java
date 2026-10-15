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
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.ECOSYSTEM_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.PACKAGE_NAME_MAX_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.COLUMN_CANONICAL_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.COLUMN_DESCRIPTION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.COLUMN_DISPLAY_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.COLUMN_HOMEPAGE_URL;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.COLUMN_PACKAGE_LOOKUP_KEY;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.COLUMN_PURL_TYPE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.COLUMN_RENAMED_TO;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.COLUMN_REPOSITORY_URL;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.COLUMN_SOURCE_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.INDEX_PACKAGE_LOOKUP_KEY_UNIQ;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbUpstreamPackagesTable.URL_MAX_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

class CreateScaRefdbUpstreamPackagesTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateScaRefdbUpstreamPackagesTable.class);

  private final CreateScaRefdbUpstreamPackagesTable underTest = new CreateScaRefdbUpstreamPackagesTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_sca_ref_upstream_pkgs", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PURL_TYPE, Types.VARCHAR, ECOSYSTEM_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CANONICAL_NAME, Types.VARCHAR, PACKAGE_NAME_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PACKAGE_LOOKUP_KEY, Types.VARCHAR, PACKAGE_LOOKUP_KEY_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_DISPLAY_NAME, Types.VARCHAR, PACKAGE_NAME_MAX_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_REPOSITORY_URL, Types.VARCHAR, URL_MAX_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_HOMEPAGE_URL, Types.VARCHAR, URL_MAX_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_DESCRIPTION, Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_RENAMED_TO, Types.VARCHAR, PACKAGE_NAME_MAX_SIZE, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SOURCE_UPDATED_AT, Types.BIGINT, null, false);
    db.assertUniqueIndex(TABLE_NAME, INDEX_PACKAGE_LOOKUP_KEY_UNIQ, COLUMN_PACKAGE_LOOKUP_KEY);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
