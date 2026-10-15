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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VULNERABILITY_ID_MAX_LENGTH;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_ALIASES;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_CVSS_SCORE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_CWES;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_DETAILS;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_MODIFIED;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_PUBLISHED;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_SUMMARY;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_SYNC_GENERATION;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_UPSTREAMS;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_VULNERABILITY_ID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.COLUMN_WITHDRAWN;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.INDEX_ID_UNIQ;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.INDEX_MODIFIED_ID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.INDEX_SYNC_GENERATION_MODIFIED_ID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbOsvVulnerabilitiesTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;

class CreateScaRefdbOsvVulnerabilitiesTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateScaRefdbOsvVulnerabilitiesTable.class);

  private final CreateScaRefdbOsvVulnerabilitiesTable underTest = new CreateScaRefdbOsvVulnerabilitiesTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_sca_ref_osv_vulns", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VULNERABILITY_ID, Types.VARCHAR, VULNERABILITY_ID_MAX_LENGTH, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_MODIFIED, Types.TIMESTAMP, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CREATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPDATED_AT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SYNC_GENERATION, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_WITHDRAWN, Types.TIMESTAMP, null, true);
    assertDecimalColumn(COLUMN_CVSS_SCORE);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_CWES, Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ALIASES, Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UPSTREAMS, Types.CLOB, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_PUBLISHED, Types.TIMESTAMP, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_SUMMARY, Types.CLOB, null, true);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_DETAILS, Types.CLOB, null, true);
    db.assertUniqueIndex(TABLE_NAME, INDEX_ID_UNIQ, COLUMN_VULNERABILITY_ID);
    db.assertIndex(TABLE_NAME, INDEX_MODIFIED_ID, COLUMN_MODIFIED, COLUMN_VULNERABILITY_ID);
    db.assertIndex(TABLE_NAME, INDEX_SYNC_GENERATION_MODIFIED_ID, COLUMN_SYNC_GENERATION, COLUMN_MODIFIED, COLUMN_VULNERABILITY_ID);
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }

  private void assertDecimalColumn(String columnName) throws SQLException {
    String dialect = Objects.requireNonNull(db.database().getDialect()).getId();
    if (MsSql.ID.equals(dialect)) {
      try (Connection connection = db.openConnection()) {
        assertThat(DatabaseUtils.tableColumnExists(connection, TABLE_NAME, columnName)).isTrue();
      }
    } else {
      int jdbcType = Oracle.ID.equals(dialect) || PostgreSql.ID.equals(dialect) ? Types.NUMERIC : Types.DOUBLE;
      db.assertColumnDefinition(TABLE_NAME, columnName, jdbcType, null, true);
    }
  }
}
