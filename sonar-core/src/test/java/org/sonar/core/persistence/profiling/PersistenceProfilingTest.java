/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.read.ListAppender;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.core.profiling.Profiling;

import java.io.ByteArrayInputStream;
import java.sql.*;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PersistenceProfilingTest {

  @Test
  public void should_be_transparent_when_profiling_less_than_full() {
    BasicDataSource datasource = mock(BasicDataSource.class);
    assertThat(PersistenceProfiling.addProfilingIfNeeded(datasource , new Settings())).isEqualTo(datasource);
  }

  @Test
  public void should_enable_profiling_when_profiling_is_full() throws Exception {
    final Logger sqlLogger = (Logger) LoggerFactory.getLogger("sql");
    ListAppender<ILoggingEvent> appender = new ListAppender<ILoggingEvent>();
    appender.setContext(new ContextBase());
    appender.start();
    sqlLogger.addAppender(appender);

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

    Settings settings = new Settings();
    settings.setProperty(Profiling.CONFIG_PROFILING_LEVEL, Profiling.Level.FULL.toString());

    BasicDataSource resultDataSource = PersistenceProfiling.addProfilingIfNeeded(originDataSource , settings);

    assertThat(resultDataSource).isInstanceOf(ProfilingDataSource.class);
    assertThat(resultDataSource.getUrl()).isNull();
    assertThat(resultDataSource.getConnection().getClientInfo()).isNull();
    PreparedStatement preparedStatementProxy = resultDataSource.getConnection().prepareStatement(sqlWithParams);
    preparedStatementProxy.setInt(1, param1);
    preparedStatementProxy.setString(2, param2);
    preparedStatementProxy.setDate(3, param3);
    preparedStatementProxy.setTimestamp(4, param4);
    preparedStatementProxy.setBlob(5, new ByteArrayInputStream(param5));
    assertThat(preparedStatementProxy.getConnection()).isNull();
    assertThat(preparedStatementProxy.execute()).isTrue();
    final Statement statementProxy = resultDataSource.getConnection().createStatement();
    assertThat(statementProxy.getConnection()).isNull();
    assertThat(statementProxy.execute(sql)).isTrue();

    assertThat(appender.list).hasSize(2);
    assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.INFO);
    assertThat(appender.list.get(0).getFormattedMessage()).contains(sqlWithParams).contains(" - parameters are: ").contains(Integer.toString(param1)).contains(param2);
    assertThat(appender.list.get(1).getLevel()).isEqualTo(Level.INFO);
    assertThat(appender.list.get(1).getFormattedMessage()).contains(sql);
  }
}
