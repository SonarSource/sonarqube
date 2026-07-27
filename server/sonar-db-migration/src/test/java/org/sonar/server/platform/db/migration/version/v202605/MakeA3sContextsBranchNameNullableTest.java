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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.MockedStatic;
import org.sonar.db.ColumnMetadata;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.MigrationDbTester;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.BRANCH_NAME_SIZE;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.COLUMN_BRANCH_NAME;
import static org.sonar.server.platform.db.migration.version.v202605.CreateA3SContextsTable.TABLE_NAME;

class MakeA3sContextsBranchNameNullableTest {

  @RegisterExtension
  public final MigrationDbTester db = MigrationDbTester.createForMigrationStep(MakeA3sContextsBranchNameNullable.class);

  private final MakeA3sContextsBranchNameNullable underTest = new MakeA3sContextsBranchNameNullable(db.database());

  @Test
  void execute_shouldMakeBranchNameNullable() throws SQLException {
    db.assertColumnDefinition(TABLE_NAME, COLUMN_BRANCH_NAME, Types.VARCHAR, BRANCH_NAME_SIZE, false);

    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_BRANCH_NAME, Types.VARCHAR, BRANCH_NAME_SIZE, true);
  }

  @Test
  void execute_shouldBeReentrant() throws SQLException {
    underTest.execute();
    underTest.execute();

    db.assertColumnDefinition(TABLE_NAME, COLUMN_BRANCH_NAME, Types.VARCHAR, BRANCH_NAME_SIZE, true);
  }

  @Test
  void execute_shouldDropDefaultSoOmittedInsertsStoreNull() throws SQLException {
    underTest.execute();

    db.executeInsert(TABLE_NAME,
      "uuid", "ctx-uuid",
      "analysis_uuid", "analysis-uuid",
      "branch_uuid", "branch-uuid",
      "project_uuid", "project-uuid",
      "kind", "kind-1",
      "created_at", 1_000L);

    assertThat(db.selectFirst("SELECT branch_name FROM " + TABLE_NAME).get("BRANCH_NAME")).isNull();
  }

  @Test
  void execute_shouldDoNothingWhenColumnDoesNotExist() throws SQLException {
    db.executeDdl("DROP TABLE " + TABLE_NAME);

    underTest.execute();

    db.assertTableDoesNotExist(TABLE_NAME);
  }

  @Test
  void execute_whenOracle_shouldDropDefaultWithModify() throws SQLException {
    try (Mocks mocks = mockMigration(Oracle.ID, mock(Connection.class))) {
      mocks.migration.execute(mocks.context);

      verify(mocks.context).execute("ALTER TABLE " + TABLE_NAME + " MODIFY (" + COLUMN_BRANCH_NAME + " DEFAULT NULL)");
    }
  }

  @Test
  void execute_whenMsSql_shouldDropDefaultConstraint() throws SQLException {
    // On SQL Server the default is a named constraint that ALTER COLUMN leaves behind, so it must be looked up and
    // dropped by name.
    String constraintName = "DF_a3s_contexts_branch_name";
    try (Mocks mocks = mockMigration(MsSql.ID, connectionReturningDefaultConstraint(constraintName))) {
      mocks.migration.execute(mocks.context);

      verify(mocks.context).execute(List.of("ALTER TABLE " + TABLE_NAME + " DROP CONSTRAINT " + constraintName));
    }
  }

  private static Mocks mockMigration(String dialectId, Connection connection) throws SQLException {
    Dialect dialect = mock(Dialect.class);
    when(dialect.getId()).thenReturn(dialectId);
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenReturn(connection);
    Database database = mock(Database.class);
    when(database.getDialect()).thenReturn(dialect);
    when(database.getDataSource()).thenReturn(dataSource);

    // Column still NOT NULL with the leftover DEFAULT '', i.e. the state produced by CreateA3SContextsTable.
    ColumnMetadata columnMetadata = mock(ColumnMetadata.class);
    when(columnMetadata.nullable()).thenReturn(false);
    MockedStatic<DatabaseUtils> databaseUtils = mockStatic(DatabaseUtils.class);
    databaseUtils.when(() -> DatabaseUtils.getColumnMetadata(any(Connection.class), eq(TABLE_NAME), eq(COLUMN_BRANCH_NAME)))
      .thenReturn(columnMetadata);

    return new Mocks(new MakeA3sContextsBranchNameNullable(database), mock(DdlChange.Context.class), databaseUtils);
  }

  /** A connection whose SQL Server default-constraint lookup returns a single constraint of the given name. */
  private static Connection connectionReturningDefaultConstraint(String constraintName) throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(true, false);
    when(resultSet.getString(1)).thenReturn(constraintName);
    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.executeQuery()).thenReturn(resultSet);
    Connection connection = mock(Connection.class);
    when(connection.getSchema()).thenReturn("dbo");
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    return connection;
  }

  private record Mocks(MakeA3sContextsBranchNameNullable migration, DdlChange.Context context,
                       MockedStatic<DatabaseUtils> databaseUtils) implements AutoCloseable {
    @Override
    public void close() {
      databaseUtils.close();
    }
  }
}
