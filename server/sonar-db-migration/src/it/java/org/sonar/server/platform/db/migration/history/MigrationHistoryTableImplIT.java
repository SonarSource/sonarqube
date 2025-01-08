/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.history;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.MsSql;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationHistoryTableImplIT {
  private static final String TABLE_SCHEMA_MIGRATIONS = "schema_migrations";

  @RegisterExtension
  public final MigrationDbTester dbTester = MigrationDbTester.createEmpty();

  private final MigrationHistoryTableImpl underTest = new MigrationHistoryTableImpl(dbTester.database());

  @Test
  void start_creates_table_on_empty_schema() {
    underTest.start();

    verifyTable();
  }

  @Test
  void start_does_not_fail_if_table_exists() throws SQLException {
    executeDdl(String.format("create table %s (version %s(255) not null)", TABLE_SCHEMA_MIGRATIONS, getFieldType()));
    verifyTable();

    underTest.start();

    verifyTable();
  }

  private String getFieldType() {
    if (dbTester.database().getDialect().getId().equals(MsSql.ID)) {
      return "nvarchar";
    }
    return "varchar";
  }

  private void executeDdl(String sql) throws SQLException {
    try (Connection connection = dbTester.database().getDataSource().getConnection()) {
      connection.setAutoCommit(false);
      try (Statement statement = connection.createStatement()) {
        statement.execute(sql);
        connection.commit();
      }
    }
  }

  private void verifyTable() {
    assertThat(dbTester.countRowsOfTable(TABLE_SCHEMA_MIGRATIONS)).isZero();
    dbTester.assertColumnDefinition(TABLE_SCHEMA_MIGRATIONS, "version", Types.VARCHAR, 255, false);
  }
}
