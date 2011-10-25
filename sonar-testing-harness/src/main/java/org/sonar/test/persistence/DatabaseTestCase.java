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
package org.sonar.test.persistence;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.dbunit.Assertion;
import org.dbunit.DatabaseUnitException;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import java.io.InputStream;
import java.sql.*;
import java.util.List;

import static org.junit.Assert.fail;

public abstract class DatabaseTestCase {

  private static IDatabaseTester databaseTester = null;
  private static final String JDBC_URL = "jdbc:derby:memory:sonar";
  private Connection connection = null;


  @BeforeClass
  public static void startDatabase() throws Exception {
    System.setProperty("derby.stream.error.file", "target/derby.log");

    /*
    Note: we could use a datasource instead of a direct JDBC connection.
    See org.apache.derby.jdbc.ClientDataSource (http://db.apache.org/derby/papers/DerbyClientSpec.html#Connection+URL+Format)
    and org.dbunit.DataSourceDatabaseTester
     */
    EmbeddedDriver driver = new EmbeddedDriver();
    DriverManager.registerDriver(driver);
    databaseTester = new JdbcDatabaseTester(driver.getClass().getName(), JDBC_URL + ";create=true");
    createDatabase();
  }

  private static void createDatabase() throws Exception {
    Connection c = databaseTester.getConnection().getConnection();
    Statement st = c.createStatement();
    for (String ddl : loadDdlStatements()) {
      st.executeUpdate(ddl);
      c.commit();
    }
    st.close();
    c.close();
  }

  private static String[] loadDdlStatements() throws Exception {
    InputStream in = DatabaseTestCase.class.getResourceAsStream("/org/sonar/test/persistence/sonar-test.ddl");
    List<String> lines = IOUtils.readLines(in);
    StringBuilder ddl = new StringBuilder();
    for (String line : lines) {
      if (StringUtils.isNotBlank(line) && !StringUtils.startsWith(StringUtils.trimToEmpty(line), "#")) {
        ddl.append(line).append(" ");
      }
    }

    in.close();
    return StringUtils.split(StringUtils.trim(ddl.toString()), ";");
  }

  @AfterClass
  public static void stopDatabase() throws Exception {
    try {
      DriverManager.getConnection(JDBC_URL + ";drop=true");
      databaseTester.onTearDown();
    } catch (Exception e) {
      // silently fail
    }
  }

  public static IDatabaseTester getDatabaseTester() {
    return databaseTester;
  }

  protected final Connection getConnection() {
    try {
      if (connection == null) {
        connection = getDatabaseTester().getConnection().getConnection();
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return connection;
  }

  @After
  public final void truncateTables() throws SQLException {
    ResultSet rs = getConnection().getMetaData().getTables(null, "APP", null, null);
    Statement st = getConnection().createStatement();
    while (rs.next()) {
      String tableName = rs.getString(3);
      // truncate command is implemented since derby 10.7
      st.executeUpdate("TRUNCATE TABLE " + tableName);
    }
    st.close();
    rs.close();
    getConnection().commit();
  }

  @After
  public final void closeConnection() {
    if (connection != null) {
      try {
        connection.close();
        connection = null;
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
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

  private void setupData(InputStream... dataSetStream) {
    try {
      IDataSet[] dataSets = new IDataSet[dataSetStream.length];
      for (int i = 0; i < dataSetStream.length; i++) {
        ReplacementDataSet dataSet = new ReplacementDataSet(new FlatXmlDataSet(dataSetStream[i]));
        dataSet.addReplacementObject("[null]", null);
        dataSet.addReplacementObject("true", 1);
        dataSet.addReplacementObject("false", 0);
        dataSets[i] = dataSet;
      }
      CompositeDataSet compositeDataSet = new CompositeDataSet(dataSets);
      DatabaseOperation.CLEAN_INSERT.execute(getDatabaseTester().getConnection(), compositeDataSet);

    } catch (Exception e) {
      throw new RuntimeException("Could not setup DBUnit data", e);
    }
  }

  protected final void assertTables(String testName, String... tables) {
    try {
      IDataSet dataSet = getCurrentDataSet();
      IDataSet expectedDataSet = getExpectedData(testName);
      for (String table : tables) {
        Assertion.assertEquals(expectedDataSet.getTable(table), dataSet.getTable(table));
      }
    } catch (DataSetException e) {
      throw translateException("Error while checking results", e);
    } catch (DatabaseUnitException e) {
      fail(e.getMessage());
    }
  }

  protected final void assertTables(String testName, String[] tables, String[] ignoreCols) {
    try {
      IDataSet dataSet = getCurrentDataSet();
      IDataSet expectedDataSet = getExpectedData(testName);
      for (String table : tables) {
        Assertion.assertEqualsIgnoreCols(expectedDataSet.getTable(table), dataSet.getTable(table), ignoreCols);
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

  private IDataSet getExpectedData(String testName) {
    String className = getClass().getName();
    className = String.format("/%s/%s-result.xml", className.replace(".", "/"), testName);

    InputStream in = getClass().getResourceAsStream(className);
    try {
      return getData(in);
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  private IDataSet getData(InputStream stream) {
    try {
      ReplacementDataSet dataSet = new ReplacementDataSet(new FlatXmlDataSet(stream));
      dataSet.addReplacementObject("[null]", null);
      return dataSet;
    } catch (Exception e) {
      throw translateException("Could not read the dataset stream", e);
    }
  }

  private IDataSet getCurrentDataSet() {
    try {
      return databaseTester.getConnection().createDataSet();
    } catch (Exception e) {
      throw translateException("Could not create the current dataset", e);
    }
  }

  private static RuntimeException translateException(String msg, Exception cause) {
    RuntimeException runtimeException = new RuntimeException(String.format("%s: [%s] %s", msg, cause.getClass().getName(), cause.getMessage()));
    runtimeException.setStackTrace(cause.getStackTrace());
    return runtimeException;
  }

}
