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
import org.dbunit.Assertion;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.DatabaseUnitException;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.sonar.api.config.Settings;

import java.io.InputStream;
import java.sql.SQLException;

import static org.junit.Assert.fail;

public abstract class AbstractDaoTestCase {
  private static Database database;
  private static MyBatis myBatis;
  private static DatabaseCommands databaseCommands;

  private IDatabaseTester databaseTester;
  private IDatabaseConnection connection;

  @BeforeClass
  public static void startDatabase() throws Exception {
    Settings settings = new Settings().setProperties(Maps.fromProperties(System.getProperties()));

    boolean hasDialect = settings.hasKey("sonar.jdbc.dialect");
    if (hasDialect) {
      database = new DefaultDatabase(settings);
    } else {
      database = new H2Database();
    }
    database.start();

    myBatis = new MyBatis(database);
    myBatis.start();

    databaseCommands = DatabaseCommands.forDialect(database.getDialect());
  }

  @Before
  public void setupDbUnit() throws SQLException {
    databaseCommands.truncateDatabase(myBatis.openSession().getConnection());
    databaseTester = new DataSourceDatabaseTester(database.getDataSource());
  }

  @After
  public void stopConnection() throws Exception {
    if (databaseTester != null) {
      databaseTester.onTearDown();
    }
    if (connection != null) {
      connection.close();
    }
  }

  @AfterClass
  public static void stopDatabase() {
    if (database != null) {
      database.stop();
    }
  }

  protected MyBatis getMyBatis() {
    return myBatis;
  }

  protected void setupData(String testName) {
    InputStream stream = null;
    try {
      String className = getClass().getName();
      className = String.format("/%s/%s.xml", className.replace(".", "/"), testName);
      stream = getClass().getResourceAsStream(className);
      if (stream == null) {
        throw new RuntimeException("Test not found :" + className);
      }

      setupData(stream);
    } finally {
      Closeables.closeQuietly(stream);
    }
  }

  private void setupData(InputStream dataStream) {
    try {
      ReplacementDataSet dataSet = new ReplacementDataSet(new FlatXmlDataSet(dataStream));
      dataSet.addReplacementObject("[null]", null);
      dataSet.addReplacementObject("[false]", databaseCommands.getFalse());
      dataSet.addReplacementObject("[true]", databaseCommands.getTrue());

      databaseTester.setDataSet(dataSet);
      connection = databaseTester.getConnection();

      connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, databaseCommands.getDbUnitFactory());
      databaseCommands.getDbunitDatabaseOperation().execute(connection, databaseTester.getDataSet());

    } catch (Exception e) {
      throw translateException("Could not setup DBUnit data", e);
    }
  }

  protected void checkTables(String testName, String... tables) {
    checkTables(testName, new String[] {}, tables);
  }

  protected void checkTables(String testName, String[] excludedColumnNames, String... tables) {
    try {
      IDataSet dataSet = getCurrentDataSet();
      IDataSet expectedDataSet = getExpectedData(testName);
      for (String table : tables) {
        ITable filteredTable = DefaultColumnFilter.excludedColumnsTable(dataSet.getTable(table), excludedColumnNames);
        Assertion.assertEquals(expectedDataSet.getTable(table), filteredTable);
      }
    } catch (DataSetException e) {
      throw translateException("Error while checking results", e);
    } catch (DatabaseUnitException e) {
      fail(e.getMessage());
    }
  }

  protected void assertEmptyTables(String... emptyTables) {
    for (String table : emptyTables) {
      try {
        Assert.assertEquals("Table " + table + " not empty.", 0, getCurrentDataSet().getTable(table).getRowCount());
      } catch (DataSetException e) {
        throw translateException("Error while checking results", e);
      }
    }
  }

  private IDataSet getExpectedData(String testName) {
    String className = getClass().getName();
    className = String.format("/%s/%s-result.xml", className.replace(".", "/"), testName);

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

  private IDataSet getCurrentDataSet() {
    try {
      return connection.createDataSet();
    } catch (SQLException e) {
      throw translateException("Could not create the current dataset", e);
    }
  }

  private static RuntimeException translateException(String msg, Exception cause) {
    RuntimeException runtimeException = new RuntimeException(String.format("%s: [%s] %s", msg, cause.getClass().getName(), cause.getMessage()));
    runtimeException.setStackTrace(cause.getStackTrace());
    return runtimeException;
  }

  protected IDatabaseConnection getConnection() {
    return connection;
  }
}
