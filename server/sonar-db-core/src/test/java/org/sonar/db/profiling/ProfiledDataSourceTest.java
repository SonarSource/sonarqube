/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.profiling;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProfiledDataSourceTest {

  @Rule
  public LogTester logTester = new LogTester();

  BasicDataSource originDataSource = mock(BasicDataSource.class);

  @Test
  public void execute_and_log_statement() throws Exception {
    logTester.setLevel(LoggerLevel.TRACE);

    Connection connection = mock(Connection.class);
    when(originDataSource.getConnection()).thenReturn(connection);

    String sql = "select from dual";
    Statement stmt = mock(Statement.class);
    when(connection.createStatement()).thenReturn(stmt);
    when(stmt.execute(sql)).thenReturn(true);

    ProfiledDataSource underTest = new ProfiledDataSource(originDataSource, ProfiledConnectionInterceptor.INSTANCE);

    assertThat(underTest.getUrl()).isNull();
    assertThat(underTest.getConnection().getClientInfo()).isNull();
    final Statement statementProxy = underTest.getConnection().createStatement();
    assertThat(statementProxy.getConnection()).isNull();
    assertThat(statementProxy.execute(sql)).isTrue();

    assertThat(logTester.logs(LoggerLevel.TRACE)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.TRACE).get(0))
      .contains("sql=select from dual");
  }

  @Test
  public void execute_and_log_prepared_statement_with_parameters() throws Exception {
    logTester.setLevel(LoggerLevel.TRACE);

    Connection connection = mock(Connection.class);
    when(originDataSource.getConnection()).thenReturn(connection);

    String sqlWithParams = "insert into polop (col1, col2, col3, col4) values (?, ?, ?, ?, ?)";
    int param1 = 42;
    String param2 = "plouf";
    Date param3 = new Date(System.currentTimeMillis());
    Timestamp param4 = new Timestamp(System.currentTimeMillis());
    byte[] param5 = "blob".getBytes(UTF_8);

    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    when(connection.prepareStatement(sqlWithParams)).thenReturn(preparedStatement);
    when(preparedStatement.execute()).thenReturn(true);

    ProfiledDataSource ds = new ProfiledDataSource(originDataSource, ProfiledConnectionInterceptor.INSTANCE);

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

    assertThat(logTester.logs(LoggerLevel.TRACE)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.TRACE).get(0))
      .contains("sql=insert into polop (col1, col2, col3, col4) values (?, ?, ?, ?, ?)")
      .contains("params=42, plouf");
  }

  @Test
  public void execute_and_log_prepared_statement_without_parameters() throws Exception {
    logTester.setLevel(LoggerLevel.TRACE);

    Connection connection = mock(Connection.class);
    when(originDataSource.getConnection()).thenReturn(connection);

    String sqlWithParams = "select from dual";
    PreparedStatement preparedStatement = mock(PreparedStatement.class);
    when(connection.prepareStatement(sqlWithParams)).thenReturn(preparedStatement);
    when(preparedStatement.execute()).thenReturn(true);

    ProfiledDataSource ds = new ProfiledDataSource(originDataSource, ProfiledConnectionInterceptor.INSTANCE);

    assertThat(ds.getUrl()).isNull();
    assertThat(ds.getConnection().getClientInfo()).isNull();
    PreparedStatement preparedStatementProxy = ds.getConnection().prepareStatement(sqlWithParams);
    assertThat(preparedStatementProxy.getConnection()).isNull();
    assertThat(preparedStatementProxy.execute()).isTrue();

    assertThat(logTester.logs(LoggerLevel.TRACE)).hasSize(1);
    assertThat(logTester.logs(LoggerLevel.TRACE).get(0))
      .contains("sql=select from dual")
      .doesNotContain("params=");
  }

  @Test
  public void delegate_to_underlying_data_source() throws Exception {
    ProfiledDataSource proxy = new ProfiledDataSource(originDataSource, ProfiledConnectionInterceptor.INSTANCE);

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
