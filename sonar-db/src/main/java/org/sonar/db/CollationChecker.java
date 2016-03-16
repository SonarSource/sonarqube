/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Loggers;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.apache.commons.lang.StringUtils.endsWithIgnoreCase;

/**
 * SONAR-6171
 * Check that database has UTF8 character set and case-sensitive collation.
 * As obviously tables must be checked after being created, this component
 * must not be executed at the same time as {@link DatabaseChecker}.
 */
public class CollationChecker implements Startable {

  private final Database db;
  private final StatementExecutor statementExecutor;

  public CollationChecker(Database db) {
    this(db, new StatementExecutor());
  }

  @VisibleForTesting
  CollationChecker(Database db, StatementExecutor statementExecutor) {
    this.db = db;
    this.statementExecutor = statementExecutor;
  }

  @Override
  public void start() {
    try {
      Loggers.get(getClass()).info("Verify database collation");
      check();
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void stop() {
    // nothing to do
  }

  private void check() throws SQLException {
    try (Connection connection = db.getDataSource().getConnection()) {
      switch (db.getDialect().getId()) {
        case Oracle.ID:
          checkOracle(connection);
          break;
        case PostgreSql.ID:
          checkPostgreSql(connection);
          break;
        case MySql.ID:
          checkMySql(connection);
          break;
        case MsSql.ID:
          checkMsSql(connection);
          break;
      }
    }
  }

  /**
   * Only database metadata is checked. Oracle does not allow to override character set on tables.
   */
  private void checkOracle(Connection connection) throws SQLException {
    String charset = selectSingleCell(connection, "select value  from nls_database_parameters where parameter='NLS_CHARACTERSET'");
    String sort = selectSingleCell(connection, "select value from nls_database_parameters where parameter='NLS_SORT'");
    if (!containsIgnoreCase(charset, "UTF8") || !"BINARY".equalsIgnoreCase(sort)) {
      throw MessageException.of(format("Oracle must be have UTF8 charset and BINARY sort. NLS_CHARACTERSET is %s and NLS_SORT is %s.", charset, sort));
    }
  }

  /**
   * PostgreSQL does not support case-insensitive collations. Only character set must be verified.
   */
  private void checkPostgreSql(Connection connection) throws SQLException {
    // character set is defined globally and can be overridden on each column
    // This request returns all VARCHAR columns. Collation may be empty.
    // Examples:
    // issues | key | ''
    // projects | name | utf8
    List<String[]> rows = select(connection, "select table_name, column_name, collation_name " +
      "from information_schema.columns " +
      "where table_schema='public' " +
      "and udt_name='varchar' " +
      "order by table_name, column_name", 3);
    boolean mustCheckGlobalCollation = false;
    List<String> errors = new ArrayList<>();
    for (String[] row : rows) {
      if (StringUtils.isBlank(row[2])) {
        mustCheckGlobalCollation = true;
      } else if (!containsIgnoreCase(row[2], "utf8")) {
        errors.add(row[0] + "." + row[1]);
      }
    }

    if (mustCheckGlobalCollation) {
      String charset = selectSingleCell(connection, "SELECT pg_encoding_to_char(encoding) FROM pg_database WHERE datname = current_database()");
      if (!containsIgnoreCase(charset, "UTF8")) {
        throw MessageException.of("");
      }
    }
    if (!errors.isEmpty()) {
      throw MessageException.of("UTF8 charset is required for database columns [" + Joiner.on(", ").join(errors) + "]");
    }
  }

  /**
   * Check VARCHAR columns
   */
  private void checkMySql(Connection connection) throws SQLException {
    // All VARCHAR columns are returned. No need to check database general collation.
    // Example of row:
    // issues | kee | utf8 | utf8_bin
    List<String[]> rows = select(connection,
      "SELECT table_name, column_name, character_set_name, collation_name " +
        "FROM INFORMATION_SCHEMA.columns " +
        "WHERE table_schema=database() and character_set_name is not null and collation_name is not null", 4 /* columns */);
    List<String> errors = new ArrayList<>();
    for (String[] row : rows) {
      if (!containsIgnoreCase(row[2], "utf8") || endsWithIgnoreCase(row[3], "_ci")) {
        errors.add(row[0] + "." + row[1]);
      }
    }
    if (!errors.isEmpty()) {
      throw MessageException.of("UTF8 charset and case-sensitive collation are required for database columns [" + Joiner.on(", ").join(errors) + "]");
    }
  }

  private void checkMsSql(Connection connection) throws SQLException {
    // All VARCHAR columns are returned. No need to check database general collation.
    // Example of row:
    // issues | kee | Latin1_General_CS_AS
    List<String[]> rows = select(connection,
      "SELECT table_name, column_name, collation_name " +
        "FROM [INFORMATION_SCHEMA].[COLUMNS] " +
        "WHERE collation_name is not null " +
        "ORDER BY table_name,column_name", 3 /* columns */);
    List<String> errors = new ArrayList<>();
    for (String[] row : rows) {
      if (!endsWithIgnoreCase(row[2], "_CS_AS")) {
        errors.add(row[0] + "." + row[1]);
      }
    }
    if (!errors.isEmpty()) {
      throw MessageException.of("Case-sensitive and accent-sensitive charset (CS_AS) is required for database columns [" + Joiner.on(", ").join(errors) + "]");
    }
  }

  @CheckForNull
  private String selectSingleCell(Connection connection, String sql) throws SQLException {
    String[] cols = selectSingleRow(connection, sql, 1);
    return cols == null ? null : cols[0];
  }

  @CheckForNull
  private String[] selectSingleRow(Connection connection, String sql, int columns) throws SQLException {
    List<String[]> rows = select(connection, sql, columns);
    if (rows.isEmpty()) {
      return null;
    }
    if (rows.size() == 1) {
      return rows.get(0);
    }
    throw new IllegalStateException("Expecting only one result for [" + sql + "]");
  }

  private List<String[]> select(Connection connection, String sql, int columns) throws SQLException {
    return statementExecutor.executeQuery(connection, sql, columns);
  }

  static class StatementExecutor {
    List<String[]> executeQuery(Connection connection, String sql, int columns) throws SQLException {
      Statement stmt = null;
      ResultSet rs = null;
      try {
        stmt = connection.createStatement();
        rs = stmt.executeQuery(sql);
        List<String[]> result = new ArrayList<>();
        while (rs.next()) {
          String[] row = new String[columns];
          for (int i = 0; i < columns; i++) {
            row[i] = rs.getString(i + 1);
          }
          result.add(row);
        }
        return result;

      } finally {
        DatabaseUtils.closeQuietly(rs);
        DatabaseUtils.closeQuietly(stmt);
      }
    }
  }

}
