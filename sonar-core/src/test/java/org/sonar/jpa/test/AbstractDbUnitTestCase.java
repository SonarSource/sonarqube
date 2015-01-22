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
package org.sonar.jpa.test;

import org.apache.commons.io.IOUtils;
import org.dbunit.Assertion;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.DatabaseUnitException;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.ext.mssql.InsertIdentityOperation;
import org.dbunit.operation.DatabaseOperation;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.sonar.api.database.DatabaseSession;
import org.sonar.core.cluster.NullQueue;
import org.sonar.core.config.Logback;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.DatabaseCommands;
import org.sonar.core.persistence.H2Database;
import org.sonar.core.persistence.MyBatis;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.jpa.session.JpaDatabaseSession;
import org.sonar.jpa.session.MemoryDatabaseConnector;

import java.io.InputStream;
import java.sql.SQLException;

import static org.junit.Assert.fail;

/**
 * Heavily duplicates AbstractDaoTestCase as long as Hibernate is in use.
 * @deprecated this class does not support non-H2 databases
 */
@Deprecated
public abstract class AbstractDbUnitTestCase {
  private static Database database;
  private static MyBatis myBatis;
  private static DatabaseCommands databaseCommands;
  private static MemoryDatabaseConnector dbConnector;
  private IDatabaseTester databaseTester;
  private JpaDatabaseSession session;

  @BeforeClass
  public static void startDatabase() throws SQLException {
    if (database == null) {
      database = new H2Database("sonarHibernate", true);
      database.start();

      databaseCommands = DatabaseCommands.forDialect(database.getDialect());

      dbConnector = new MemoryDatabaseConnector(database);
      dbConnector.start();

      myBatis = new MyBatis(database, new Logback(), new NullQueue());
      myBatis.start();
    }
  }

  @Before
  public void startDbUnit() throws Exception {
    databaseCommands.truncateDatabase(database.getDataSource());
    databaseTester = new DataSourceDatabaseTester(database.getDataSource());
    session = new JpaDatabaseSession(dbConnector);
    session.start();
  }

  @After
  public void stopDbUnit() throws Exception {
    if (session != null) {
      session.rollback();
    }
  }

  protected DatabaseSession getSession() {
    return session;
  }

  protected MyBatis getMyBatis() {
    return myBatis;
  }

  protected Database getDatabase() {
    return database;
  }

  protected DatabaseSessionFactory getSessionFactory() {
    return new DatabaseSessionFactory() {
      public DatabaseSession getSession() {
        return session;
      }

      public void clear() {
      }
    };
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
      databaseCommands.resetPrimaryKeys(database.getDataSource());
    } catch (SQLException e) {
      throw translateException("Could not setup DBUnit data", e);
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

      new InsertIdentityOperation(DatabaseOperation.INSERT).execute(connection, databaseTester.getDataSet());
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
    } catch (SQLException ignored) {

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
      IOUtils.closeQuietly(in);
    }
  }

  private IDataSet getData(InputStream stream) {
    try {
      ReplacementDataSet dataSet = new ReplacementDataSet(new FlatXmlDataSet(stream));
      dataSet.addReplacementObject("[null]", null);
      dataSet.addReplacementObject("[false]", Boolean.FALSE);
      dataSet.addReplacementObject("[true]", Boolean.TRUE);
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

  protected Long getHQLCount(Class<?> hqlClass) {
    String hqlCount = "SELECT count(o) from " + hqlClass.getSimpleName() + " o";
    return (Long) getSession().createQuery(hqlCount).getSingleResult();
  }
}
