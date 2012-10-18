/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.persistence;

import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DatabaseUtilsTest extends AbstractDaoTestCase {

  @Test
  public void should_close_connection() throws SQLException {
    Connection connection = getConnection();
    assertThat(connection.isClosed()).isFalse();

    DatabaseUtils.closeQuietly(connection);
    assertThat(connection.isClosed()).isTrue();
  }

  @Test
  public void should_support_null_connection() {
    DatabaseUtils.closeQuietly((Connection) null);
    // no failure
  }

  @Test
  public void should_close_statement_and_resultset() throws SQLException {
    Connection connection = getConnection();
    try {
      PreparedStatement statement = connection.prepareStatement("SELECT 1");
      ResultSet rs = statement.executeQuery();

      DatabaseUtils.closeQuietly(rs);
      DatabaseUtils.closeQuietly(statement);

      assertThat(statement.isClosed()).isTrue();
      // can not execute: assertThat(rs.isClosed()).isTrue(); -> isClosed() has been introduced in Java 6
    } finally {
      DatabaseUtils.closeQuietly(connection);
    }
  }

  @Test
  public void should_not_fail_on_connection_errors() throws SQLException {
    Connection connection = mock(Connection.class);
    doThrow(new SQLException()).when(connection).close();

    DatabaseUtils.closeQuietly(connection);

    // no failure
    verify(connection).close(); // just to be sure
  }

  @Test
  public void should_not_fail_on_statement_errors() throws SQLException {
    Statement statement = mock(Statement.class);
    doThrow(new SQLException()).when(statement).close();

    DatabaseUtils.closeQuietly(statement);

    // no failure
    verify(statement).close(); // just to be sure
  }

  @Test
  public void should_not_fail_on_resulset_errors() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    doThrow(new SQLException()).when(rs).close();

    DatabaseUtils.closeQuietly(rs);

    // no failure
    verify(rs).close(); // just to be sure
  }
}
