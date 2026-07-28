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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.COLUMN_DIMENSION_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.COLUMN_ENTITY_ID;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.COLUMN_ENTITY_TYPE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.COLUMN_ISSUES_RESOLVED;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.COLUMN_ISSUES_RESOLVED_RECENT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.COLUMN_RECORDED_AT_EPOCH;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.COLUMN_TOTAL_MINUTES_TO_RESOLUTION;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.COLUMN_TOTAL_MINUTES_TO_RESOLUTION_RECENT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.ENTITY_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.ENTITY_TYPE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.INDEX_ENTITY_TYPE_RECORDED_AT;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.INDEX_UNIQUE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateIssueTtrHistoryTable.TABLE_NAME;

class CreateIssueTtrHistoryTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreateIssueTtrHistoryTable.class);

  private final CreateIssueTtrHistoryTable underTest = new CreateIssueTtrHistoryTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ENTITY_ID, Types.VARCHAR, ENTITY_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ENTITY_TYPE, Types.VARCHAR, ENTITY_TYPE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_DIMENSION_ID, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_RECORDED_AT_EPOCH, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_TOTAL_MINUTES_TO_RESOLUTION, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ISSUES_RESOLVED, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_TOTAL_MINUTES_TO_RESOLUTION_RECENT, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ISSUES_RESOLVED_RECENT, Types.INTEGER, null, false);
    assertUniqueIndex();
    db.assertIndex(TABLE_NAME, INDEX_ENTITY_TYPE_RECORDED_AT, COLUMN_ENTITY_ID, COLUMN_ENTITY_TYPE, COLUMN_RECORDED_AT_EPOCH);
  }

  private void assertUniqueIndex() throws SQLException {
    if (!Oracle.ID.equals(db.database().getDialect().getId())) {
      db.assertUniqueIndex(TABLE_NAME, INDEX_UNIQUE, COLUMN_ENTITY_ID, COLUMN_ENTITY_TYPE, COLUMN_DIMENSION_ID, COLUMN_RECORDED_AT_EPOCH);
      return;
    }

    List<String> indexColumns = new ArrayList<>();
    boolean indexFound = false;
    boolean indexNonUnique = true;
    try (Connection connection = db.openConnection();
         ResultSet resultSet = connection.getMetaData().getIndexInfo(null, null, TABLE_NAME.toUpperCase(), false, false)) {
      while (resultSet.next()) {
        if (INDEX_UNIQUE.equalsIgnoreCase(resultSet.getString("INDEX_NAME"))) {
          indexFound = true;
          indexNonUnique = resultSet.getBoolean("NON_UNIQUE");
          indexColumns.add(resultSet.getString("COLUMN_NAME"));
        }
      }
    }

    assertThat(indexFound).as("Index %s should exist", INDEX_UNIQUE).isTrue();
    assertThat(indexNonUnique).as("Index %s should be unique", INDEX_UNIQUE).isFalse();
    // Oracle exposes descending index columns using generated virtual-column names, so only the naming of the last
    // column is ignored here.
    assertThat(indexColumns).hasSize(4);
    assertThat(indexColumns.subList(0, 3)).containsExactly(
      COLUMN_ENTITY_ID.toUpperCase(Locale.ROOT),
      COLUMN_ENTITY_TYPE.toUpperCase(Locale.ROOT),
      COLUMN_DIMENSION_ID.toUpperCase(Locale.ROOT));
  }

  @Test
  void migration_should_be_reentrant() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();
    underTest.execute();

    db.assertTableExists(TABLE_NAME);
  }
}
