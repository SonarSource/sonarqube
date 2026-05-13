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
package org.sonar.server.platform.db.migration.version.v202603;

import jakarta.annotation.Nullable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RenameIndexOnIssuesImpactsToPkTest {

  private static final String TABLE_NAME = "issues_impacts";
  private static final String OLD_INDEX_NAME = "uniq_iss_key_sof_qual";
  private static final String NEW_INDEX_NAME = "pk_issues_impacts";

  @Test
  void execute_whenOracleAndIndexExists_shouldRenameIndex() throws SQLException {
    DdlChange.Context context = runExecuteForOracle(OLD_INDEX_NAME);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(context).execute(sqlCaptor.capture());
    assertThat(sqlCaptor.getValue()).isEqualTo("ALTER INDEX " + OLD_INDEX_NAME + " RENAME TO " + NEW_INDEX_NAME);
  }

  @Test
  void execute_whenOracleAndActualIndexNameDiffers_shouldRenameActualIndex() throws SQLException {
    String actualIndexName = "sys_c00012345";
    DdlChange.Context context = runExecuteForOracle(actualIndexName);

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(context).execute(sqlCaptor.capture());
    assertThat(sqlCaptor.getValue()).isEqualTo("ALTER INDEX " + actualIndexName + " RENAME TO " + NEW_INDEX_NAME);
  }

  @Test
  void execute_whenOracleAndIndexDoesNotExist_shouldNotExecuteAnySql() throws SQLException {
    DdlChange.Context context = runExecuteForOracle(null);

    verify(context, never()).execute(any(String.class));
  }

  private DdlChange.Context runExecuteForOracle(@Nullable String foundIndexName) throws SQLException {
    Dialect dialect = mock(Dialect.class);
    when(dialect.getId()).thenReturn(Oracle.ID);

    Database db = mock(Database.class, Mockito.withSettings().defaultAnswer(RETURNS_MOCKS));
    when(db.getDialect()).thenReturn(dialect);

    DdlChange.Context context = mock(DdlChange.Context.class);

    try (MockedStatic<DatabaseUtils> dbUtils = Mockito.mockStatic(DatabaseUtils.class)) {
      dbUtils.when(() -> DatabaseUtils.findExistingIndex(any(Connection.class), eq(TABLE_NAME), eq(OLD_INDEX_NAME)))
        .thenReturn(Optional.ofNullable(foundIndexName));

      new RenameIndexOnIssuesImpactsToPk(db).execute(context);
    }
    return context;
  }

  @ParameterizedTest
  @ValueSource(strings = {H2.ID, PostgreSql.ID, MsSql.ID})
  void execute_whenNotOracle_shouldBeNoOp(String dialectId) throws SQLException {
    verifyNoOpForDialect(dialectId);
  }

  private void verifyNoOpForDialect(String dialectId) throws SQLException {
    Dialect dialect = mock(Dialect.class);
    when(dialect.getId()).thenReturn(dialectId);

    Database db = mock(Database.class, Mockito.withSettings().defaultAnswer(RETURNS_MOCKS));
    when(db.getDialect()).thenReturn(dialect);

    DdlChange.Context context = mock(DdlChange.Context.class);

    try (MockedStatic<DatabaseUtils> dbUtils = Mockito.mockStatic(DatabaseUtils.class)) {
      new RenameIndexOnIssuesImpactsToPk(db).execute(context);

      dbUtils.verifyNoInteractions();
    }

    verify(context, never()).execute(any(String.class));
  }

}
