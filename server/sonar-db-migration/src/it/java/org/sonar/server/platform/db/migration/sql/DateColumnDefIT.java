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
package org.sonar.server.platform.db.migration.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Dialect;
import org.sonar.server.platform.db.migration.def.ColumnDef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.server.platform.db.migration.def.DateColumnDef.newDateColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

class DateColumnDefIT {

  private static final String CREATED_TABLE = "datecoldef_created";
  private static final String ALTERED_TABLE = "datecoldef_altered";

  @RegisterExtension
  public final MigrationDbTester dbTester = MigrationDbTester.createEmpty();

  private final List<String> tablesToDrop = new ArrayList<>();
  private Dialect dialect;

  @BeforeEach
  void print_execution_context() {
    dialect = dbTester.database().getDialect();
    System.out.println("DateColumnDefIT: zone=" + ZoneId.systemDefault() + ", database=" + dialect.getId());
  }

  @AfterEach
  void drop_created_tables() {
    RuntimeException cleanupFailure = null;
    for (int index = tablesToDrop.size() - 1; index >= 0; index--) {
      try {
        dbTester.executeDdl("DROP TABLE " + tablesToDrop.get(index));
      } catch (RuntimeException e) {
        if (cleanupFailure == null) {
          cleanupFailure = e;
        } else {
          cleanupFailure.addSuppressed(e);
        }
      }
    }
    tablesToDrop.clear();
    if (cleanupFailure != null) {
      throw cleanupFailure;
    }
  }

  @Test
  void nullability_on_date_columns_is_correctly_applied_in_migrations() throws SQLException {
    // Arrange
    createTable(CREATED_TABLE,
      dateColumn("nullable_date", true),
      dateColumn("required_date", false));

    // Act
    insertDateColumns(CREATED_TABLE, "row-1", LocalDate.of(2024, 2, 29), LocalDate.of(1969, 12, 31));
    insertDateColumns(CREATED_TABLE, "row-2", null, LocalDate.of(2025, 12, 31));
    List<DateColumns> rows = readDateColumns(CREATED_TABLE);

    // Assert
    assertThat(rows).containsExactly(
      new DateColumns("row-1", LocalDate.of(2024, 2, 29), LocalDate.of(1969, 12, 31)),
      new DateColumns("row-2", null, LocalDate.of(2025, 12, 31)));
  }

  @Test
  void date_columns_added_to_existing_table_round_trip_correctly() throws SQLException {
    // Arrange
    createTable(ALTERED_TABLE);
    String alterSql = new AddColumnsBuilder(dialect, ALTERED_TABLE)
      .addColumn(dateColumn("nullable_date", true))
      .addColumn(dateColumn("required_date", false))
      .build();
    dbTester.executeDdl(alterSql);

    // Act
    insertDateColumns(ALTERED_TABLE, "row-1", LocalDate.of(2024, 2, 29), LocalDate.of(1969, 12, 31));
    insertDateColumns(ALTERED_TABLE, "row-2", null, LocalDate.of(2025, 12, 31));
    List<DateColumns> rows = readDateColumns(ALTERED_TABLE);

    // Assert
    assertThat(rows).containsExactly(
      new DateColumns("row-1", LocalDate.of(2024, 2, 29), LocalDate.of(1969, 12, 31)),
      new DateColumns("row-2", null, LocalDate.of(2025, 12, 31)));
  }

  private void createTable(String tableName, ColumnDef... columns) {
    CreateTableBuilder builder = new CreateTableBuilder(dialect, tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName("id").setLimit(40).setIsNullable(false).build());
    for (ColumnDef column : columns) {
      builder.addColumn(column);
    }
    builder.build().forEach(dbTester::executeDdl);
    tablesToDrop.add(tableName);
  }

  private static ColumnDef dateColumn(String name, boolean isNullable) {
    return newDateColumnDefBuilder().setColumnName(name).setIsNullable(isNullable).build();
  }

  private void insertDateColumns(String tableName, String id, LocalDate nullableDate, LocalDate requiredDate) throws SQLException {
    String sql = "INSERT INTO " + tableName + " (id, nullable_date, required_date) VALUES (?, ?, ?)";
    try (Connection connection = dbTester.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, id);
      setDateParameter(statement, 2, nullableDate);
      setDateParameter(statement, 3, requiredDate);
      statement.executeUpdate();
    }
  }

  private List<DateColumns> readDateColumns(String tableName) throws SQLException {
    List<DateColumns> rows = new ArrayList<>();
    String sql = "SELECT id, nullable_date, required_date FROM " + tableName + " ORDER BY id";
    try (Connection connection = dbTester.openConnection(); PreparedStatement statement = connection.prepareStatement(sql);
         ResultSet resultSet = statement.executeQuery()) {
      while (resultSet.next()) {
        rows.add(new DateColumns(
          resultSet.getString(1),
          resultSet.getObject(2, LocalDate.class),
          resultSet.getObject(3, LocalDate.class)));
      }
    }
    return rows;
  }


  private static void setDateParameter(PreparedStatement statement, int parameterIndex, LocalDate date) throws SQLException {
    if (date == null) {
      statement.setNull(parameterIndex, Types.DATE);
    } else {
      statement.setObject(parameterIndex, date, Types.DATE);
    }
  }

  private record DateColumns(String id, LocalDate nullableDate, LocalDate requiredDate) {
  }
}
