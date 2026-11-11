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
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.sonar.db.Database;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnComponentsKeeLowerHash.COMPUTED_COLUMN_SQL;
import static org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnComponentsKeeLowerHash.INDEX_NAME;
import static org.sonar.server.platform.db.migration.version.v202606.CreateIndexOnComponentsKeeLowerHash.TABLE_NAME;

class CreateIndexOnComponentsKeeLowerHashTest {

  @Test
  void execute_shouldDoNothingWhenDialectIsNotMsSql() throws SQLException {
    DdlChange.Context context = mock(DdlChange.Context.class);

    new CreateIndexOnComponentsKeeLowerHash(database(H2.ID, null)).execute(context);

    verify(context, never()).execute(anyList());
    verify(context, never()).execute(COMPUTED_COLUMN_SQL);
  }

  @Test
  void execute_shouldAddColumnAndIndexWhenMissing() throws SQLException {
    DdlChange.Context context = mock(DdlChange.Context.class);

    try (Connection ignored = h2("missing")) {
      new CreateIndexOnComponentsKeeLowerHash(database(MsSql.ID, "missing")).execute(context);
    }

    verify(context).execute(COMPUTED_COLUMN_SQL);
    verify(context).execute(List.of("CREATE INDEX " + INDEX_NAME + " ON " + TABLE_NAME + " (kee_lower_hash)"));
  }

  @Test
  void execute_shouldSkipColumnAndIndexWhenTheyAlreadyExist() throws SQLException {
    DdlChange.Context context = mock(DdlChange.Context.class);

    try (Connection connection = h2("present")) {
      connection.createStatement().execute("CREATE TABLE components (kee VARCHAR(10), kee_lower_hash BINARY(32))");
      connection.createStatement().execute("CREATE INDEX components_lower_kee ON components (kee_lower_hash)");
      new CreateIndexOnComponentsKeeLowerHash(database(MsSql.ID, "present")).execute(context);
    }

    verify(context, never()).execute(anyList());
    verify(context, never()).execute(COMPUTED_COLUMN_SQL);
  }

  private static Database database(String dialectId, String databaseName) throws SQLException {
    Database database = mock(Database.class);
    if (MsSql.ID.equals(dialectId)) {
      when(database.getDialect()).thenReturn(new MsSql());
      DataSource dataSource = mock(DataSource.class);
      when(dataSource.getConnection()).thenAnswer(invocation -> h2(databaseName));
      when(database.getDataSource()).thenReturn(dataSource);
    } else {
      when(database.getDialect()).thenReturn(new H2());
    }
    return database;
  }

  private static Connection h2(String name) throws SQLException {
    return DriverManager.getConnection("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1", "sa", "");
  }

  @Test
  void execute_shouldStillCreateTheIndexWhenOnlyTheColumnExists() throws SQLException {
    DdlChange.Context context = mock(DdlChange.Context.class);

    try (Connection connection = h2("columnOnly")) {
      connection.createStatement().execute("CREATE TABLE components (kee VARCHAR(10), kee_lower_hash BINARY(32))");
      new CreateIndexOnComponentsKeeLowerHash(database(MsSql.ID, "columnOnly")).execute(context);
    }

    verify(context, never()).execute(COMPUTED_COLUMN_SQL);
    verify(context).execute(List.of("CREATE INDEX " + INDEX_NAME + " ON " + TABLE_NAME + " (kee_lower_hash)"));
  }
}
