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
package org.sonar.server.database;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerComponent;
import org.sonar.api.utils.SonarException;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.DdlUtils;

import javax.sql.DataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class LocalDatabaseFactory implements ServerComponent {
  private static final Logger LOG = LoggerFactory.getLogger(LocalDatabaseFactory.class);

  private static final String H2_DIALECT = "h2";
  private static final String H2_DRIVER = "org.h2.Driver";
  private static final String H2_URL = "jdbc:h2:";
  private static final String H2_USER = "sonar";
  private static final String H2_PASSWORD = "sonar";

  private Database database;

  public LocalDatabaseFactory(Database database) {
    this.database = database;
  }

  public String createDatabaseForLocalMode() throws SQLException {
    String name = "/tmp/" + System.nanoTime();

    DataSource source = database.getDataSource();
    BasicDataSource destination = dataSource(H2_DRIVER, H2_USER, H2_PASSWORD, H2_URL + name);

    create(destination, H2_DIALECT);

    copyTable(source, destination, "PROPERTIES", "SELECT * FROM PROPERTIES WHERE (USER_ID IS NULL) AND (RESOURCE_ID IS NULL)");
    copyTable(source, destination, "RULES_PROFILES", "SELECT * FROM RULES_PROFILES");
    copyTable(source, destination, "RULES", "SELECT * FROM RULES");
    copyTable(source, destination, "RULES_PARAMETERS", "SELECT * FROM RULES_PARAMETERS");
    copyTable(source, destination, "ACTIVE_RULES", "SELECT * FROM ACTIVE_RULES");
    copyTable(source, destination, "ACTIVE_RULE_PARAMETERS", "SELECT * FROM ACTIVE_RULE_PARAMETERS");
    copyTable(source, destination, "METRICS", "SELECT * FROM METRICS");

    destination.close();

    return new File(name + ".h2.db").getAbsolutePath();
  }

  private void copyTable(DataSource source, DataSource dest, String table, String query) throws SQLException {
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
    } finally {
      closeQuietly(destResultSet);
      closeQuietly(destConnection);
      closeQuietly(sourceResultSet);
      closeQuietly(sourceStatement);
      closeQuietly(sourceConnection);
    }
  }

  private int getColumnCount(DataSource dataSource, String table) throws SQLException {
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
    } finally {
      closeQuietly(metaData);
      closeQuietly(connection);
    }
  }

  private void truncate(DataSource dataSource, String table) throws SQLException {
    Connection connection = null;
    Statement statement = null;
    try {
      connection = dataSource.getConnection();
      statement = connection.createStatement();
      statement.executeUpdate("TRUNCATE TABLE " + table);
    } finally {
      closeQuietly(statement);
      closeQuietly(connection);
    }
  }

  private BasicDataSource dataSource(String driver, String user, String password, String url) {
    BasicDataSource dataSource = new BasicDataSource();
    dataSource.setDriverClassName(driver);
    dataSource.setUsername(user);
    dataSource.setPassword(password);
    dataSource.setUrl(url);
    return dataSource;
  }

  public void create(DataSource dataSource, String dialect) throws SQLException {
    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      DdlUtils.createSchema(connection, dialect);
    } catch (SQLException e) {
      throw new SonarException("Fail to create local database schema", e);
    } finally {
      closeQuietly(connection);
    }
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
