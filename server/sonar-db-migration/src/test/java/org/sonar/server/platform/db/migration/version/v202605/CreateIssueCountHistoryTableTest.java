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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueCountHistoryTable.DIMENSION_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueCountHistoryTable.ENTITY_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueCountHistoryTable.ENTITY_TYPE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueCountHistoryTable.ISS_CNT_HIST_EPOCH_TIME_IDX;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueCountHistoryTable.ISS_CNT_HIST_EPOCH_UQ_IDX_2;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueCountHistoryTable.ISSUE_COUNT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueCountHistoryTable.RECORDED_AT_EPOCH;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueCountHistoryTable.TABLE_NAME;

class CreateIssueCountHistoryTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateIssueCountHistoryTable.class);

  private final CreateIssueCountHistoryTable underTest = new CreateIssueCountHistoryTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertColumnDefinition(TABLE_NAME, ENTITY_ID, Types.VARCHAR, 40, false);
    db.assertColumnDefinition(TABLE_NAME, DIMENSION_ID, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, RECORDED_AT_EPOCH, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, ISSUE_COUNT, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, ENTITY_TYPE, Types.VARCHAR, 40, false);
    assertEpochUniqueIndex();
    db.assertIndex(TABLE_NAME, ISS_CNT_HIST_EPOCH_TIME_IDX, ENTITY_ID, ENTITY_TYPE, RECORDED_AT_EPOCH);
  }

  private void assertEpochUniqueIndex() {
    if (Oracle.ID.equals(Objects.requireNonNull(db.database().getDialect()).getId())) {
      assertUniqueIndexExistsForOracle();
    } else {
      db.assertUniqueIndex(TABLE_NAME, ISS_CNT_HIST_EPOCH_UQ_IDX_2, ENTITY_ID, ENTITY_TYPE, DIMENSION_ID, RECORDED_AT_EPOCH);
    }
  }

  private void assertUniqueIndexExistsForOracle() {
    try (Connection connection = db.openConnection()) {
      try (ResultSet rs = connection.getMetaData().getIndexInfo(null, null, TABLE_NAME.toUpperCase(), false, false)) {
        boolean indexFound = false;
        while (rs.next()) {
          if (ISS_CNT_HIST_EPOCH_UQ_IDX_2.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
            indexFound = true;
            assertThat(rs.getBoolean("NON_UNIQUE")).as("Index should be unique").isFalse();
            break;
          }
        }
        assertThat(indexFound).as("Index %s should exist", ISS_CNT_HIST_EPOCH_UQ_IDX_2).isTrue();
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to check index", e);
    }
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
