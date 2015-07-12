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
package org.sonar.db;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.dialect.Oracle;
import org.sonar.test.DbTests;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@Category(DbTests.class)
public class DatabaseUtilsTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void should_close_connection() throws Exception {
    Connection connection = dbTester.openConnection();
    assertThat(isClosed(connection)).isFalse();

    DatabaseUtils.closeQuietly(connection);
    assertThat(isClosed(connection)).isTrue();
  }

  @Test
  public void should_support_null_connection() {
    DatabaseUtils.closeQuietly((Connection) null);
    // no failure
  }

  @Test
  public void should_close_statement_and_resultset() throws Exception {
    Connection connection = dbTester.openConnection();
    try {
      PreparedStatement statement = connection.prepareStatement(selectDual());
      ResultSet rs = statement.executeQuery();

      DatabaseUtils.closeQuietly(rs);
      DatabaseUtils.closeQuietly(statement);

      assertThat(isClosed(statement)).isTrue();
      assertThat(isClosed(rs)).isTrue();
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

  /**
   * Connection.isClosed() has been introduced in java 1.6
   */
  private boolean isClosed(Connection c) {
    try {
      c.createStatement().execute(selectDual());
      return false;
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * Statement.isClosed() has been introduced in java 1.6
   */
  private boolean isClosed(Statement s) {
    try {
      s.execute("SELECT 1");
      return false;
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * ResultSet.isClosed() has been introduced in java 1.6
   */
  private boolean isClosed(ResultSet rs) {
    try {
      rs.next();
      return false;
    } catch (Exception e) {
      return true;
    }
  }

  private String selectDual() {
    String sql = "SELECT 1";
    if (Oracle.ID.equals(dbTester.database().getDialect().getId())) {
      sql = "SELECT 1 FROM DUAL";
    }
    return sql;
  }

  @Test
  public void repeatCondition() {
    assertThat(DatabaseUtils.repeatCondition("uuid=?", 1, "or")).isEqualTo("uuid=?");
    assertThat(DatabaseUtils.repeatCondition("uuid=?", 3, "or")).isEqualTo("uuid=? or uuid=? or uuid=?");
  }

  @Test
  public void execute_large_inputs() {
    List<Integer> inputs = newArrayList();
    List<String> expectedOutputs = newArrayList();
    for (int i = 0; i < 2010; i++) {
      inputs.add(i);
      expectedOutputs.add(Integer.toString(i));
    }

    List<String> outputs = DatabaseUtils.executeLargeInputs(inputs, new Function<List<Integer>, List<String>>() {
      @Override
      public List<String> apply(List<Integer> input) {
        // Check that each partition is only done on 1000 elements max
        assertThat(input.size()).isLessThanOrEqualTo(1000);
        return newArrayList(Iterables.transform(input, new Function<Integer, String>() {
          @Override
          public String apply(Integer input) {
            return Integer.toString(input);
          }
        }));
      }
    });

    assertThat(outputs).isEqualTo(expectedOutputs);
  }

  @Test
  public void execute_large_inputs_on_empty_list() {
    List<String> outputs = DatabaseUtils.executeLargeInputs(Collections.<Integer>emptyList(), new Function<List<Integer>, List<String>>() {
      @Override
      public List<String> apply(List<Integer> input) {
        fail("No partition should be made on empty list");
        return Collections.emptyList();
      }
    });

    assertThat(outputs).isEmpty();
  }

  @Test
  public void log_all_sql_exceptions() {
    SQLException root = new SQLException("this is root", "123");
    SQLException next = new SQLException("this is next", "456");
    root.setNextException(next);

    DatabaseUtils.log(Loggers.get(getClass()), root);

    assertThat(logTester.logs(LoggerLevel.ERROR)).contains("SQL error: 456. Message: this is next");
  }
}
