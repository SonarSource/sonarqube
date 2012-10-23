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

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.SonarException;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DbTemplate implements ServerComponent {
  private static final Logger LOG = LoggerFactory.getLogger(DbTemplate.class);

  public DbTemplate copyTable(DataSource source, DataSource dest, String table, String query) {
    LOG.info("Copy table " + table);

    int colCount = getColumnCount(source, table);

    truncate(dest, table);

    Connection sourceConnection = null;
    Statement sourceStatement = null;
    ResultSet sourceResultSet = null;
    Connection destConnection = null;
    ResultSet destResultSet = null;
    try {
      sourceConnection = source.getConnection();
      sourceStatement = sourceConnection.createStatement();
      sourceResultSet = sourceStatement.executeQuery(query);

      destConnection = dest.getConnection();
      destConnection.setAutoCommit(false);

      PreparedStatement destStatement = destConnection.prepareStatement("INSERT INTO " + table + " VALUES(" + StringUtils.repeat("?", ",", colCount) + ")");
      while (sourceResultSet.next()) {
        for (int col = 1; col <= colCount; col++) {
          Object value = sourceResultSet.getObject(col);
          destStatement.setObject(col, value);
        }
        destStatement.addBatch();
      }

      destStatement.executeBatch();
      destConnection.commit();
      destStatement.close();
    } catch (SQLException e) {
      throw new SonarException("Fail to copy table " + table, e);
    } finally {
      closeQuietly(destResultSet);
      closeQuietly(destConnection);
      closeQuietly(sourceResultSet);
      closeQuietly(sourceStatement);
      closeQuietly(sourceConnection);
    }

    return this;
  }

  public int getColumnCount(DataSource dataSource, String table) {
    Connection connection = null;
    ResultSet metaData = null;
    try {
      connection = dataSource.getConnection();
      metaData = connection.getMetaData().getColumns(null, null, table, null);

      int nbColumns = 0;
      while (metaData.next()) {
        nbColumns++;
      }

      return nbColumns;
    } catch (SQLException e) {
      throw new SonarException("Fail to get column count for table " + table, e);
    } finally {
      closeQuietly(metaData);
      closeQuietly(connection);
    }
  }

  public int getRowCount(BasicDataSource dataSource, String table) {
    Connection connection = null;
    Statement statement = null;
    ResultSet resultSet = null;
    try {
      connection = dataSource.getConnection();
      statement = connection.createStatement();
      resultSet = statement.executeQuery("SELECT count(*) from " + table);

      return resultSet.next() ? resultSet.getInt(1) : 0;
    } catch (SQLException e) {
      throw new SonarException("Fail to get row count for table " + table, e);
    } finally {
      closeQuietly(resultSet);
      closeQuietly(statement);
      closeQuietly(connection);
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
      throw new SonarException("Fail to truncate table " + table, e);
    } finally {
      closeQuietly(statement);
      closeQuietly(connection);
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
      throw new SonarException("Fail to createSchema local database schema", e);
    } finally {
      closeQuietly(connection);
    }

    return this;
  }

  private void closeQuietly(Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        // ignore
      }
    }
  }

  private void closeQuietly(Statement statement) {
    if (statement != null) {
      try {
        statement.close();
      } catch (SQLException e) {
        // ignore
      }
    }
  }

  private void closeQuietly(ResultSet resultSet) {
    if (resultSet != null) {
      try {
        resultSet.close();
      } catch (SQLException e) {
        // ignore
      }
    }
  }
}
