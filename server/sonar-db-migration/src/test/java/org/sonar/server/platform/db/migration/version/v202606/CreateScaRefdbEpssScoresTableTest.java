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
import org.sonar.db.MigrationDbTester;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbEpssScoresTable.COLUMN_CREATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbEpssScoresTable.COLUMN_PERCENTILE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbEpssScoresTable.COLUMN_SCORE;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbEpssScoresTable.COLUMN_UPDATED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbEpssScoresTable.COLUMN_UUID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbEpssScoresTable.COLUMN_VULNERABILITY_ID;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbEpssScoresTable.INDEX_VULN_ID_UNIQ;
import static org.sonar.server.platform.db.migration.version.v202606.CreateScaRefdbEpssScoresTable.TABLE_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.ScaRefdbMigrationConstants.VULNERABILITY_ID_MAX_LENGTH;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

class CreateScaRefdbEpssScoresTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateScaRefdbEpssScoresTable.class);

  private final CreateScaRefdbEpssScoresTable underTest = new CreateScaRefdbEpssScoresTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertPrimaryKey(TABLE_NAME, "pk_sca_ref_epss_scores", COLUMN_UUID);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_UUID, Types.VARCHAR, UUID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_VULNERABILITY_ID, Types.VARCHAR, VULNERABILITY_ID_MAX_LENGTH, false);
    assertDecimalColumn(COLUMN_PERCENTILE);
    assertDecimalColumn(COLUMN_SCORE);
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

  private void assertDecimalColumn(String columnName) throws SQLException {
    String dialect = Objects.requireNonNull(db.database().getDialect()).getId();
    if (MsSql.ID.equals(dialect)) {
      try (Connection connection = db.openConnection()) {
        assertThat(DatabaseUtils.tableColumnExists(connection, TABLE_NAME, columnName)).isTrue();
      }
    } else {
      int jdbcType = Oracle.ID.equals(dialect) || PostgreSql.ID.equals(dialect) ? Types.NUMERIC : Types.DOUBLE;
      db.assertColumnDefinition(TABLE_NAME, columnName, jdbcType, null, false);
    }
  }
}
