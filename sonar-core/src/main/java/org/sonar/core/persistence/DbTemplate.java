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
package org.sonar.core.persistence;

import com.google.common.base.Joiner;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.SonarException;
import org.sonar.core.profiling.Profiling;
import org.sonar.core.profiling.Profiling.Level;
import org.sonar.core.profiling.StopWatch;

import javax.annotation.Nullable;
import javax.sql.DataSource;

import java.sql.*;

public class DbTemplate implements ServerComponent {
  private static final Logger LOG = LoggerFactory.getLogger(DbTemplate.class);

  private Profiling profiling;

  public DbTemplate(Profiling profiling) {
    this.profiling = profiling;
  }

  public DbTemplate copyTable(DataSource source, DataSource dest, String table) {
    return copyTableColumns(source, dest, table, null);
  }

  public DbTemplate copyTableColumns(DataSource source, DataSource dest, String table, @Nullable String[] columnNames) {
    String selectQuery = "SELECT * FROM " + table;
    copyTableColumns(source, dest, table, selectQuery, columnNames);

    return this;
  }

  public DbTemplate copyTable(DataSource source, DataSource dest, String table, String selectQuery) {
    return copyTableColumns(source, dest, table, selectQuery, null);
  }

  public DbTemplate copyTableColumns(DataSource source, DataSource dest, String table, String selectQuery, @Nullable String[] columnNames) {
    LOG.debug("Copy table {}", table);
    StopWatch watch = profiling.start("previewdb", Level.BASIC);

    truncate(dest, table);

    Connection sourceConnection = null;
    Statement sourceStatement = null;
    ResultSet sourceResultSet = null;
    Connection destConnection = null;
    ResultSet destResultSet = null;
    PreparedStatement destStatement = null;
    int count = 0;
    try {
      sourceConnection = source.getConnection();
      sourceStatement = sourceConnection.createStatement();
      sourceResultSet = sourceStatement.executeQuery(selectQuery);

      if (sourceResultSet.next()) {
        if (columnNames == null) {
          // Copy all columns
          columnNames = columnNames(sourceResultSet);
        }
        int[] columnTypes = columnTypes(sourceResultSet);

        destConnection = dest.getConnection();
        destConnection.setAutoCommit(false);

        String insertSql = new StringBuilder().append("INSERT INTO ").append(table).append("(").append(Joiner.on(",").join(columnNames))
          .append(") VALUES(").append(StringUtils.repeat("?", ",", columnNames.length)).append(")").toString();
        destStatement = destConnection.prepareStatement(insertSql);

        do {
          copyColumns(sourceResultSet, destStatement, columnNames, columnTypes);
          count++;
          destStatement.addBatch();
          if (count % BatchSession.MAX_BATCH_SIZE == 0) {
            destStatement.executeBatch();
            destConnection.commit();

          }
        } while (sourceResultSet.next());

        destStatement.executeBatch();
        destConnection.commit();
      }
    } catch (SQLException e) {
      LOG.error("Fail to copy table " + table, e);
      throw new IllegalStateException("Fail to copy table " + table, e);
    } finally {
      watch.stop("  " + count + " rows of " + table + " copied");
      DbUtils.closeQuietly(destStatement);
      DbUtils.closeQuietly(destResultSet);
      DbUtils.closeQuietly(destConnection);
      DbUtils.closeQuietly(sourceResultSet);
      DbUtils.closeQuietly(sourceStatement);
      DbUtils.closeQuietly(sourceConnection);
    }
    return this;
  }

  private void copyColumns(ResultSet sourceResultSet, PreparedStatement destStatement, String[] columnNames, int[] columnTypes) throws SQLException {
    for (int col = 1; col <= columnNames.length; col++) {
      if (columnTypes[col - 1] == Types.TIMESTAMP) {
        Timestamp value = sourceResultSet.getTimestamp(columnNames[col - 1]);
        destStatement.setTimestamp(col, value);
      } else {
        Object value = sourceResultSet.getObject(columnNames[col - 1]);
        destStatement.setObject(col, value);
      }
    }
  }

  private String[] columnNames(ResultSet resultSet) throws SQLException {
    int colCount = resultSet.getMetaData().getColumnCount();
    String[] columnNames = new String[colCount];
    for (int i = 1; i <= colCount; i++) {
      columnNames[i - 1] = resultSet.getMetaData().getColumnName(i);
    }
    return columnNames;
  }

  private int[] columnTypes(ResultSet resultSet) throws SQLException {
    int colCount = resultSet.getMetaData().getColumnCount();
    int[] columnTypes = new int[colCount];
    for (int i = 1; i <= colCount; i++) {
      columnTypes[i - 1] = resultSet.getMetaData().getColumnType(i);
    }
    return columnTypes;
  }

  public int getRowCount(DataSource dataSource, String table) {
    Connection connection = null;
    Statement statement = null;
    ResultSet resultSet = null;
    try {
      connection = dataSource.getConnection();
      statement = connection.createStatement();
      resultSet = statement.executeQuery("SELECT count(*) FROM " + table);

      return resultSet.next() ? resultSet.getInt(1) : 0;
    } catch (SQLException e) {
      LOG.error("Fail to get row count for table " + table, e);
      throw new SonarException("Fail to get row count for table " + table, e);
    } finally {
      DbUtils.closeQuietly(resultSet);
      DbUtils.closeQuietly(statement);
      DbUtils.closeQuietly(connection);
    }
  }

  public DbTemplate truncate(DataSource dataSource, String table) {
    Connection connection = null;
    Statement statement = null;
    try {
      connection = dataSource.getConnection();
      statement = connection.createStatement();
      statement.executeUpdate("TRUNCATE TABLE " + table);
    } catch (SQLException e) {
      LOG.error("Fail to truncate table " + table, e);
      throw new SonarException("Fail to truncate table " + table, e);
    } finally {
      DbUtils.closeQuietly(statement);
      DbUtils.closeQuietly(connection);
    }

    return this;
  }

  public BasicDataSource dataSource(String driver, String user, String password, String url) {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName(driver);
    dataSource.setUsername(user);
    dataSource.setPassword(password);
    dataSource.setUrl(url);
    return dataSource;
  }

  public DbTemplate createSchema(DataSource dataSource, String dialect) {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      DdlUtils.createSchema(connection, dialect);
    } catch (SQLException e) {
      LOG.error("Fail to createSchema local database schema", e);
      throw new SonarException("Fail to createSchema local database schema", e);
    } finally {
      DbUtils.closeQuietly(connection);
    }

    return this;
  }
}
