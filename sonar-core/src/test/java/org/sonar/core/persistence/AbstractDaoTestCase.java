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

import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import org.apache.commons.io.IOUtils;
import org.dbunit.Assertion;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.DatabaseUnitException;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.*;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.Assert;
import org.junit.Before;
import org.sonar.api.config.Settings;
import org.sonar.core.config.Logback;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.fail;

public abstract class AbstractDaoTestCase {
  private static Database database;
  private static DatabaseCommands databaseCommands;
  private static IDatabaseTester databaseTester;
  private static MyBatis myBatis;

  @Before
  public void startDatabase() throws SQLException {
    if (database == null) {
      Settings settings = new Settings().setProperties(Maps.fromProperties(System.getProperties()));

      boolean hasDialect = settings.hasKey("sonar.jdbc.dialect");
      if (hasDialect) {
        database = new DefaultDatabase(settings);
      } else {
        database = new H2Database("sonarMyBatis");
      }
      database.start();

      databaseCommands = DatabaseCommands.forDialect(database.getDialect());
      databaseTester = new DataSourceDatabaseTester(database.getDataSource());

      myBatis = new MyBatis(database, settings, new Logback());
      myBatis.start();
    }

    databaseCommands.truncateDatabase(database.getDataSource().getConnection());
  }

  protected MyBatis getMyBatis() {
    return myBatis;
  }

  protected void setupData(String... testNames) {
    InputStream[] streams = new InputStream[testNames.length];
    try {
      for (int i = 0; i < testNames.length; i++) {
        String className = getClass().getName();
        className = String.format("/%s/%s.xml", className.replace(".", "/"), testNames[i]);
        streams[i] = getClass().getResourceAsStream(className);
        if (streams[i] == null) {
          throw new RuntimeException("Test not found :" + className);
        }
      }

      setupData(streams);

    } finally {
      for (InputStream stream : streams) {
        IOUtils.closeQuietly(stream);
      }
    }
  }

  private void setupData(InputStream... dataSetStream) {
    IDatabaseConnection connection = null;
    try {
      IDataSet[] dataSets = new IDataSet[dataSetStream.length];
      for (int i = 0; i < dataSetStream.length; i++) {
        dataSets[i] = getData(dataSetStream[i]);
      }
      databaseTester.setDataSet(new CompositeDataSet(dataSets));

      connection = createConnection();

      databaseCommands.getDbunitDatabaseOperation().execute(connection, databaseTester.getDataSet());
    } catch (Exception e) {
      throw translateException("Could not setup DBUnit data", e);
    } finally {
      closeQuietly(connection);
    }
  }

  private void closeQuietly(IDatabaseConnection connection) {
    try {
      if (connection != null) {
        connection.close();
      }
    } catch (SQLException e) {
      // ignore
    }
  }

  protected void checkTables(String testName, String... tables) {
    checkTables(testName, new String[0], tables);
  }

  protected void checkTables(String testName, String[] excludedColumnNames, String... tables) {
    IDatabaseConnection connection = null;
    try {
      connection = createConnection();

      IDataSet dataSet = connection.createDataSet();
      IDataSet expectedDataSet = getExpectedData(testName);
      for (String table : tables) {
        ITable filteredTable = DefaultColumnFilter.excludedColumnsTable(dataSet.getTable(table), excludedColumnNames);
        ITable filteredExpectedTable = DefaultColumnFilter.excludedColumnsTable(expectedDataSet.getTable(table), excludedColumnNames);
        Assertion.assertEquals(filteredExpectedTable, filteredTable);
      }
    } catch (DatabaseUnitException e) {
      fail(e.getMessage());
    } catch (SQLException e) {
      throw translateException("Error while checking results", e);
    } finally {
      closeQuietly(connection);
    }
  }

  protected void checkTable(String testName, String table, String... columns) {
    IDatabaseConnection connection = null;
    try {
      connection = createConnection();

      IDataSet dataSet = connection.createDataSet();
      IDataSet expectedDataSet = getExpectedData(testName);
      ITable filteredTable = DefaultColumnFilter.includedColumnsTable(dataSet.getTable(table), columns);
      ITable filteredExpectedTable = DefaultColumnFilter.includedColumnsTable(expectedDataSet.getTable(table), columns);
      Assertion.assertEquals(filteredExpectedTable, filteredTable);
    } catch (DatabaseUnitException e) {
      fail(e.getMessage());
    } catch (SQLException e) {
      throw translateException("Error while checking results", e);
    } finally {
      closeQuietly(connection);
    }
  }

  protected void assertEmptyTables(String... emptyTables) {
    IDatabaseConnection connection = null;
    try {
      connection = createConnection();

      IDataSet dataSet = connection.createDataSet();
      for (String table : emptyTables) {
        try {
          Assert.assertEquals("Table " + table + " not empty.", 0, dataSet.getTable(table).getRowCount());
        } catch (DataSetException e) {
          throw translateException("Error while checking results", e);
        }
      }
    } catch (SQLException e) {
      throw translateException("Error while checking results", e);
    } finally {
      closeQuietly(connection);
    }
  }

  private IDatabaseConnection createConnection() {
    try {
      IDatabaseConnection connection = databaseTester.getConnection();
      connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, databaseCommands.getDbUnitFactory());
      return connection;
    } catch (Exception e) {
      throw translateException("Error while getting connection", e);
    }
  }

  private IDataSet getExpectedData(String testName) {
    String className = getClass().getName();
    className = String.format("/%s/%s-result.xml", className.replace('.', '/'), testName);

    InputStream in = getClass().getResourceAsStream(className);
    try {
      return getData(in);
    } finally {
      Closeables.closeQuietly(in);
    }
  }

  private IDataSet getData(InputStream stream) {
    try {
      ReplacementDataSet dataSet = new ReplacementDataSet(new FlatXmlDataSet(stream));
      dataSet.addReplacementObject("[null]", null);
      dataSet.addReplacementObject("[false]", databaseCommands.getFalse());
      dataSet.addReplacementObject("[true]", databaseCommands.getTrue());
      return dataSet;
    } catch (Exception e) {
      throw translateException("Could not read the dataset stream", e);
    }
  }

  private static RuntimeException translateException(String msg, Exception cause) {
    RuntimeException runtimeException = new RuntimeException(String.format("%s: [%s] %s", msg, cause.getClass().getName(), cause.getMessage()));
    runtimeException.setStackTrace(cause.getStackTrace());
    return runtimeException;
  }

  protected Connection getConnection() throws SQLException {
    return database.getDataSource().getConnection();
  }
}
