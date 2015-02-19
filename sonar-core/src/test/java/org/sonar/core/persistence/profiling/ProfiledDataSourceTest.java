/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.persistence.profiling;

import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ProfiledDataSourceTest {

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void log_sql_requests() throws Exception {
    BasicDataSource originDataSource = mock(BasicDataSource.class);

    Connection connection = mock(Connection.class);
    when(originDataSource.getConnection()).thenReturn(connection);

    String sql = "select 'polop' from dual;";
    String sqlWithParams = "insert into polop (col1, col2, col3, col4) values (?, ?, ?, ?, ?);";
    int param1 = 42;
    String param2 = "plouf";
    Date param3 = new Date(System.currentTimeMillis());
    Timestamp param4 = new Timestamp(System.currentTimeMillis());
    byte[] param5 = "blob".getBytes("UTF-8");

    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    when(connection.prepareStatement(sqlWithParams)).thenReturn(preparedStatement);
    when(preparedStatement.execute()).thenReturn(true);

    Statement statement = mock(Statement.class);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.execute(sql)).thenReturn(true);

    ProfiledDataSource ds = new ProfiledDataSource(originDataSource);

    assertThat(ds.getUrl()).isNull();
    assertThat(ds.getConnection().getClientInfo()).isNull();
    PreparedStatement preparedStatementProxy = ds.getConnection().prepareStatement(sqlWithParams);
    preparedStatementProxy.setInt(1, param1);
    preparedStatementProxy.setString(2, param2);
    preparedStatementProxy.setDate(3, param3);
    preparedStatementProxy.setTimestamp(4, param4);
    preparedStatementProxy.setBlob(5, new ByteArrayInputStream(param5));
    assertThat(preparedStatementProxy.getConnection()).isNull();
    assertThat(preparedStatementProxy.execute()).isTrue();
    final Statement statementProxy = ds.getConnection().createStatement();
    assertThat(statementProxy.getConnection()).isNull();
    assertThat(statementProxy.execute(sql)).isTrue();

    assertThat(logTester.logs()).hasSize(2);
    assertThat(logTester.logs().get(1)).contains(sql);
  }

  @Test
  public void delegate_to_underlying_datasource() throws Exception {
    BasicDataSource delegate = mock(BasicDataSource.class);
    ProfiledDataSource proxy = new ProfiledDataSource(delegate);

    // painful to call all methods
    // so using reflection to check that calls does not fail
    // Limitation: methods with parameters are not tested and calls to
    // underlying datasource are not verified
    for (Method method : ProfiledDataSource.class.getDeclaredMethods()) {
      if (method.getParameterTypes().length == 0 && Modifier.isPublic(method.getModifiers())) {
        method.invoke(proxy);
      }
    }
  }
}
