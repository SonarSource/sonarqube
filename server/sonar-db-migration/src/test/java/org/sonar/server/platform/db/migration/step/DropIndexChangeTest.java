/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.step;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.sonar.db.Database;
import org.sonar.db.DatabaseUtils;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.def.Validations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DropIndexChangeTest {
  private static final String TABLE_NAME = "components";
  private static final String INDEX_NAME = "projects_module_uuid";

  @Test
  public void execute_whenCalledWithH2_shouldGenerateProperSql() throws SQLException {
    Assertions.assertThat(runExecute(H2.ID, TABLE_NAME, INDEX_NAME, INDEX_NAME))
      .contains(INDEX_NAME);
  }

  @Test
  public void execute_whenCalledWithOracle_shouldGenerateProperSql() throws SQLException {
    Assertions.assertThat(runExecute(Oracle.ID, TABLE_NAME, INDEX_NAME, INDEX_NAME))
      .contains(INDEX_NAME);
  }

  @Test
  public void execute_whenCalledWithPg_shouldGenerateProperSql() throws SQLException {
    Assertions.assertThat(runExecute(PostgreSql.ID, TABLE_NAME, INDEX_NAME, INDEX_NAME))
      .contains(INDEX_NAME);
  }

  @Test
  public void execute_whenCalledWithMsSql_shouldGenerateProperSql() throws SQLException {
    Assertions.assertThat(runExecute(MsSql.ID, TABLE_NAME, INDEX_NAME, INDEX_NAME))
      .contains(INDEX_NAME);
  }

  @Test
  public void execute_whenCalledWithWrongDbId_shouldFail() throws SQLException {
    final String invalidDialectId = "invalid_dialect_id";
    Assertions.assertThatThrownBy(() -> runExecute(invalidDialectId, TABLE_NAME, INDEX_NAME, INDEX_NAME))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageStartingWith("Unsupported dialect for drop of index:");
  }

  @Test
  public void execute_whenNoIndexFound_shouldSkipExecution() throws SQLException {
    Assertions.assertThat(runExecute(H2.ID, TABLE_NAME, INDEX_NAME, INDEX_NAME))
      .contains(INDEX_NAME);
  }

  @Test
  public void execute_whenActualIndexIsLongerThanMax_shouldGenerateProperSql() throws SQLException {
    final String actualIndexName = "idx_123456789123456789123456789_" + INDEX_NAME;
    Assertions.assertThat(runExecute(H2.ID, TABLE_NAME, INDEX_NAME, actualIndexName))
      .contains(actualIndexName);
  }

  @Test
  public void execute_whenDifferentIndexName_shouldFindFromDb() throws SQLException {
    final String actualIndexName = "idx_123_" + INDEX_NAME;
    Assertions.assertThat(runExecute(H2.ID, TABLE_NAME, INDEX_NAME, actualIndexName))
      .contains(actualIndexName);
  }

  @Test
  public void execute_whenNoIndexFound_shouldSkip() throws SQLException {
    Assertions.assertThat(runExecute(H2.ID, TABLE_NAME, INDEX_NAME, null, false))
      .isEmpty();
  }

  private String runExecute(String dialectId, String tableName, String knownIndexName, String actualIndexName) throws SQLException {
    return runExecute(dialectId, tableName, knownIndexName, actualIndexName, true).get();
  }

  private Optional<String> runExecute(String dialectId, String tableName, String knownIndexName, String actualIndexName, boolean expectResult) throws SQLException {
    Dialect dialect = mock(Dialect.class);
    when(dialect.getId()).thenReturn(dialectId);

    Database db = Mockito.mock(Database.class, Mockito.withSettings().defaultAnswer(RETURNS_MOCKS));
    when(db.getDialect()).thenReturn(dialect);

    DdlChange.Context con = mock(DdlChange.Context.class);

    try (MockedStatic<DatabaseUtils> dbUtils = Mockito.mockStatic(DatabaseUtils.class); MockedStatic<Validations> validationsUtil = Mockito.mockStatic(Validations.class)) {
      validationsUtil.when(() -> Validations.validateTableName(any(String.class))).thenCallRealMethod();
      validationsUtil.when(() -> Validations.validateIndexName(any(String.class))).thenCallRealMethod();
      dbUtils.when(() -> DatabaseUtils.findExistingIndex(any(Connection.class), eq(tableName), eq(knownIndexName))).thenReturn(Optional.ofNullable(actualIndexName));

      DropIndexChange underTest = new DropIndexChange(db, knownIndexName, tableName) {
      };
      underTest.execute(con);

      // validate that the validations are called
      validationsUtil.verify(() -> Validations.validateTableName(eq(tableName)));
      validationsUtil.verify(() -> Validations.validateIndexName(eq(knownIndexName)));
    }

    if (expectResult) {
      ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
      Mockito.verify(con).execute(sqlCaptor.capture());
      return Optional.of(sqlCaptor.getValue());
    } else {
      Mockito.verify(con, Mockito.never()).execute(any(String.class));
      return Optional.empty();
    }
  }
}
