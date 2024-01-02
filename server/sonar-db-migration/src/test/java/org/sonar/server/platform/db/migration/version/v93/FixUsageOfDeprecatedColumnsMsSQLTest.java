/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v93;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.Test;
import org.sonar.db.Database;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.step.DdlChange.Context;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FixUsageOfDeprecatedColumnsMsSQLTest {

  @Test
  public void skip_if_not_mssql() {
    var databaseMock = mock(Database.class);
    when(databaseMock.getDialect()).thenReturn(new PostgreSql());
    var underTest = new FixUsageOfDeprecatedColumnsMsSQL(databaseMock);
    var contextMock = mock(Context.class);

    assertThatCode(() -> underTest.execute(contextMock))
      .doesNotThrowAnyException();
    verify(databaseMock, times(0)).getDataSource();
  }

  @Test
  public void execute_alter_table_if_columns_with_deprecated_type_exist() throws SQLException {
    var databaseMock = mock(Database.class);
    var dataSourceMock = mock(DataSource.class);
    var connectionMock = mock(Connection.class);
    var prepareStatementMock = mock(PreparedStatement.class);
    var resultSetMock = mock(ResultSet.class);

    when(databaseMock.getDialect()).thenReturn(new MsSql());
    when(databaseMock.getDataSource()).thenReturn(dataSourceMock);
    when(dataSourceMock.getConnection()).thenReturn(connectionMock);
    when(connectionMock.prepareStatement(anyString())).thenReturn(prepareStatementMock);
    when(prepareStatementMock.executeQuery()).thenReturn(resultSetMock);

    when(resultSetMock.next()).thenReturn(true, true, true, true, false);
    when(resultSetMock.getString(1)).thenReturn("file_sources", "issues", "notifications", "project_measures");
    when(resultSetMock.getString(2)).thenReturn("binary_data", "locations", "data", "measure_data");

    var underTest = new FixUsageOfDeprecatedColumnsMsSQL(databaseMock);
    var contextMock = mock(Context.class);

    assertThatCode(() -> underTest.execute(contextMock))
      .doesNotThrowAnyException();
    verify(contextMock, times(1)).execute(of("ALTER TABLE file_sources ALTER COLUMN binary_data VARBINARY(MAX) NULL"));
    verify(contextMock, times(1)).execute(of("ALTER TABLE issues ALTER COLUMN locations VARBINARY(MAX) NULL"));
    verify(contextMock, times(1)).execute(of("ALTER TABLE notifications ALTER COLUMN data VARBINARY(MAX) NULL"));
    verify(contextMock, times(1)).execute(of("ALTER TABLE project_measures ALTER COLUMN measure_data VARBINARY(MAX) NULL"));
  }
}
