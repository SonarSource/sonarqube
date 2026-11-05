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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.version.v202606.CreatePrResolutionHistoryTable.COLUMN_DIMENSION_ID;
import static org.sonar.server.platform.db.migration.version.v202606.CreatePrResolutionHistoryTable.COLUMN_ENTITY_ID;
import static org.sonar.server.platform.db.migration.version.v202606.CreatePrResolutionHistoryTable.COLUMN_ENTITY_TYPE;
import static org.sonar.server.platform.db.migration.version.v202606.CreatePrResolutionHistoryTable.COLUMN_ISSUES_RESOLVED;
import static org.sonar.server.platform.db.migration.version.v202606.CreatePrResolutionHistoryTable.COLUMN_RECORDED_AT_EPOCH;
import static org.sonar.server.platform.db.migration.version.v202606.CreatePrResolutionHistoryTable.ENTITY_ID_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.CreatePrResolutionHistoryTable.ENTITY_TYPE_SIZE;
import static org.sonar.server.platform.db.migration.version.v202606.CreatePrResolutionHistoryTable.INDEX_ENTITY_TYPE_RECORDED_AT;
import static org.sonar.server.platform.db.migration.version.v202606.CreatePrResolutionHistoryTable.INDEX_TYPE_EPOCH;
import static org.sonar.server.platform.db.migration.version.v202606.CreatePrResolutionHistoryTable.INDEX_UNIQUE;
import static org.sonar.server.platform.db.migration.version.v202606.CreatePrResolutionHistoryTable.TABLE_NAME;

class CreatePrResolutionHistoryTableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(CreatePrResolutionHistoryTable.class);

  private final CreatePrResolutionHistoryTable underTest = new CreatePrResolutionHistoryTable(db.database());

  @Test
  void migration_should_create_table() throws SQLException {
    db.assertTableDoesNotExist(TABLE_NAME);

    underTest.execute();

    db.assertTableExists(TABLE_NAME);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ENTITY_ID, Types.VARCHAR, ENTITY_ID_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ENTITY_TYPE, Types.VARCHAR, ENTITY_TYPE_SIZE, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_DIMENSION_ID, Types.INTEGER, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_RECORDED_AT_EPOCH, Types.BIGINT, null, false);
    db.assertColumnDefinition(TABLE_NAME, COLUMN_ISSUES_RESOLVED, Types.INTEGER, null, false);
    assertColumns();
    assertUniqueIndex();
    db.assertIndex(TABLE_NAME, INDEX_ENTITY_TYPE_RECORDED_AT, COLUMN_ENTITY_ID, COLUMN_ENTITY_TYPE, COLUMN_RECORDED_AT_EPOCH);
    assertTypeEpochIndex();
  }

  private void assertTypeEpochIndex() {
    String dialect = db.database().getDialect().getId();
    if (MsSql.ID.equals(dialect)) {
      db.assertIndex(TABLE_NAME, INDEX_TYPE_EPOCH, COLUMN_ENTITY_TYPE, COLUMN_RECORDED_AT_EPOCH, COLUMN_ENTITY_ID);
      var includedColumns = db.select(
        "select c.name as column_name from sys.index_columns ic " +
          "join sys.indexes i on i.object_id = ic.object_id and i.index_id = ic.index_id " +
          "join sys.columns c on c.object_id = ic.object_id and c.column_id = ic.column_id " +
          "where i.object_id = object_id('" + TABLE_NAME + "') and i.name = '" + INDEX_TYPE_EPOCH + "' " +
          "and ic.is_included_column = 1 order by ic.index_column_id");
      assertThat(includedColumns).extracting(row -> row.get("column_name"))
        .containsExactly(COLUMN_DIMENSION_ID, COLUMN_ISSUES_RESOLVED);
    } else {
      db.assertIndex(TABLE_NAME, INDEX_TYPE_EPOCH, COLUMN_ENTITY_TYPE, COLUMN_RECORDED_AT_EPOCH, COLUMN_ENTITY_ID,
        COLUMN_DIMENSION_ID, COLUMN_ISSUES_RESOLVED);
      if (PostgreSql.ID.equals(dialect)) {
        var indexes = db.select("select indexdef from pg_indexes where indexname = '" + INDEX_TYPE_EPOCH + "'");
        assertThat(indexes).singleElement().satisfies(row -> assertThat((String) row.get("indexdef"))
          .contains("(entity_type, recorded_at_epoch, entity_id) INCLUDE (dimension_id, issues_resolved)"));
      }
    }
  }

  private void assertColumns() throws SQLException {
    try (Connection connection = db.openConnection();
         var statement = connection.prepareStatement("select * from pr_resolution_history where 1 = 0");
         ResultSet resultSet = statement.executeQuery()) {
      var metadata = resultSet.getMetaData();
      List<String> columnNames = new ArrayList<>();
      for (int column = 1; column <= metadata.getColumnCount(); column++) {
        columnNames.add(metadata.getColumnName(column).toLowerCase(Locale.ROOT));
      }
      assertThat(columnNames).containsExactlyInAnyOrder(
        COLUMN_ENTITY_ID, COLUMN_ENTITY_TYPE, COLUMN_DIMENSION_ID, COLUMN_RECORDED_AT_EPOCH, COLUMN_ISSUES_RESOLVED);
    }
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
         ResultSet resultSet = connection.getMetaData().getIndexInfo(null, null, TABLE_NAME.toUpperCase(Locale.ROOT), false, false)) {
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
    assertTypeEpochIndex();
  }
}
