/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
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
package org.sonar.persistence.dao;

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
import org.dbunit.operation.DatabaseOperation;
import org.junit.*;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.persistence.*;

import java.io.InputStream;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.junit.Assert.fail;

public abstract class DaoTestCase {

  private static Database database;
  private static MyBatis myBatis;
  private static DatabaseCommands databaseCommands;

  private IDatabaseTester databaseTester;
  private IDatabaseConnection connection;

  @BeforeClass
  public static void startDatabase() throws Exception {
    Settings settings = new Settings();
    settings.setProperties((Map) System.getProperties());
    if (settings.hasKey("sonar.jdbc.dialect")) {
      database = new DefaultDatabase(settings);
    } else {
      database = new InMemoryDatabase();
    }
    database.start();

    myBatis = new MyBatis(database);
    myBatis.start();

    databaseCommands = DatabaseCommands.forDialect(database.getDialect());
  }

  @Before
  public void setupDbUnit() throws SQLException {
    truncateTables();
    databaseTester = new DataSourceDatabaseTester(database.getDataSource());
  }

  @After
  public void tearDownDbUnit() throws Exception {
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

  private void truncateTables() throws SQLException {
    Connection connection = myBatis.openSession().getConnection();
    Statement statement = connection.createStatement();
    for (String table : DatabaseUtils.TABLE_NAMES) {
      // 1. truncate
      String truncateCommand = databaseCommands.truncate(table);
      LoggerFactory.getLogger(getClass()).info("Execute: " + truncateCommand);
      statement.executeUpdate(truncateCommand);
      connection.commit();

      // 2. reset primary keys
      try {
        statement.executeUpdate(databaseCommands.resetPrimaryKey(table));
        connection.commit();
      } catch (Exception e) {
        // this table has no primary key
        connection.rollback();
      }
    }
    statement.close();
    connection.commit();
    connection.close();
  }

  protected MyBatis getMyBatis() {
    return myBatis;
  }

  protected final void setupData(String... testNames) {
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

  protected final void setupData(InputStream... dataSetStream) {
    try {
      IDataSet[] dataSets = new IDataSet[dataSetStream.length];
      for (int i = 0; i < dataSetStream.length; i++) {
        ReplacementDataSet dataSet = new ReplacementDataSet(new FlatXmlDataSet(dataSetStream[i]));
        dataSet.addReplacementObject("[null]", null);
        dataSets[i] = dataSet;
      }
      CompositeDataSet compositeDataSet = new CompositeDataSet(dataSets);

      databaseTester.setDataSet(compositeDataSet);
      connection = databaseTester.getConnection();

      connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, databaseCommands.dbUnitFactory());
      DatabaseOperation.CLEAN_INSERT.execute(connection, databaseTester.getDataSet());

    } catch (Exception e) {
      throw translateException("Could not setup DBUnit data", e);
    }
  }

  protected final void checkTables(String testName, String... tables) {
    checkTables(testName, new String[]{}, tables);
  }

  protected final void checkTables(String testName, String[] excludedColumnNames, String... tables) {
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

  protected final void assertEmptyTables(String... emptyTables) {
    for (String table : emptyTables) {
      try {
        Assert.assertEquals(0, getCurrentDataSet().getTable(table).getRowCount());
      } catch (DataSetException e) {
        throw translateException("Error while checking results", e);
      }
    }
  }

  protected final IDataSet getExpectedData(String testName) {
    String className = getClass().getName();
    className = String.format("/%s/%s-result.xml", className.replace(".", "/"), testName);

    InputStream in = getClass().getResourceAsStream(className);
    try {
      return getData(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  protected final IDataSet getData(InputStream stream) {
    try {
      ReplacementDataSet dataSet = new ReplacementDataSet(new FlatXmlDataSet(stream));
      dataSet.addReplacementObject("[null]", null);
      return dataSet;
    } catch (Exception e) {
      throw translateException("Could not read the dataset stream", e);
    }
  }

  protected final IDataSet getCurrentDataSet() {
    try {
      return connection.createDataSet();
    } catch (SQLException e) {
      throw translateException("Could not create the current dataset", e);
    }
  }

  protected String getCurrentDataSetAsXML() {
    return getDataSetAsXML(getCurrentDataSet());
  }

  protected String getDataSetAsXML(IDataSet dataset) {
    try {
      StringWriter writer = new StringWriter();
      FlatXmlDataSet.write(dataset, writer);
      return writer.getBuffer().toString();
    } catch (Exception e) {
      throw translateException("Could not build XML from dataset", e);
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

  protected IDatabaseTester getDatabaseTester() {
    return databaseTester;
  }

}
