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
package org.sonar.jpa.test;

import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;

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
import org.dbunit.ext.hsqldb.HsqldbDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.junit.*;
import org.sonar.api.database.DatabaseSession;
import org.sonar.jpa.dao.DaoFacade;
import org.sonar.jpa.dao.MeasuresDao;
import org.sonar.jpa.dao.ProfilesDao;
import org.sonar.jpa.dao.RulesDao;
import org.sonar.jpa.session.DatabaseSessionFactory;
import org.sonar.jpa.session.DefaultDatabaseConnector;
import org.sonar.jpa.session.JpaDatabaseSession;
import org.sonar.jpa.session.MemoryDatabaseConnector;
import org.sonar.persistence.*;

public abstract class AbstractDbUnitTestCase {

  private static final boolean USE_HSQL = false;

  private DefaultDatabaseConnector dbConnector;
  private JpaDatabaseSession session;
  private DaoFacade dao;
  private IDatabaseTester databaseTester;
  private IDatabaseConnection connection;
  private Database database;
  private MyBatis myBatis;

  @Before
  public void startDatabase() throws Exception {
    if (USE_HSQL) {
      database = new HsqlDatabase();
    } else {
      database = new InMemoryDatabase();
    }
    database.start();

    myBatis = new MyBatis(database);
    myBatis.start();

    dbConnector = new MemoryDatabaseConnector(database);
    dbConnector.start();

    session = new JpaDatabaseSession(dbConnector);
    session.start();

    databaseTester = new DataSourceDatabaseTester(database.getDataSource());
  }

  @After
  public void stopDatabase() throws Exception {
    databaseTester.onTearDown();
    // Important: close the connection and session, otherwise tests can stuck
    if (connection != null) {
      connection.close();
    }
    session.stop();
    dbConnector.stop();
    database.stop();
  }

  protected MyBatis getMyBatis() {
    return myBatis;
  }

  public DaoFacade getDao() {
    if (dao == null) {
      dao = new DaoFacade(new ProfilesDao(session), new RulesDao(session), new MeasuresDao(session));
    }
    return dao;
  }

  public DatabaseSession getSession() {
    return session;
  }

  public DatabaseSessionFactory getSessionFactory() {
    return new DatabaseSessionFactory() {

      public DatabaseSession getSession() {
        return session;
      }

      public void clear() {
      }
    };
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

      if (USE_HSQL) {
        connection.getConnection().prepareStatement("set referential_integrity FALSE").execute(); // HSQL DB
        DatabaseConfig config = connection.getConfig();
        config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new HsqldbDataTypeFactory());
      }

      DatabaseOperation.CLEAN_INSERT.execute(connection, databaseTester.getDataSet());

      if (USE_HSQL) {
        connection.getConnection().prepareStatement("set referential_integrity TRUE").execute(); // HSQL DB
      } else {
        resetDerbySequence(compositeDataSet);
      }

    } catch (Exception e) {
      throw translateException("Could not setup DBUnit data", e);
    }
  }

  private void resetDerbySequence(CompositeDataSet compositeDataSet) throws DataSetException, SQLException {
    for (ITable table : compositeDataSet.getTables()) {
      ITableMetaData tableMetaData = table.getTableMetaData();
      String tableName = tableMetaData.getTableName();
      for (Column column : tableMetaData.getColumns()) {
        if ("id".equalsIgnoreCase(column.getColumnName())) { // TODO hard-coded value
          String maxSql = "SELECT MAX(id) FROM " + tableName;
          ResultSet res = connection.getConnection().prepareStatement(maxSql).executeQuery();
          res.next();
          int max = res.getInt(1);
          res.close();
          String alterSql = "ALTER TABLE " + tableName + " ALTER COLUMN id RESTART WITH " + (max + 1);
          connection.getConnection().prepareStatement(alterSql).execute();
        }
      }
    }
  }

  protected final void checkTables(String testName, String... tables) {
    checkTables(testName, new String[] {}, tables);
  }

  protected final void checkTables(String testName, String[] excludedColumnNames, String... tables) {
    getSession().commit();
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

  protected Long getHQLCount(final Class<?> hqlClass) {
    String hqlCount = "SELECT count(o) from " + hqlClass.getSimpleName() + " o";
    return (Long) getSession().createQuery(hqlCount).getSingleResult();
  }

  protected IDatabaseConnection getConnection() {
    return connection;
  }

  protected IDatabaseTester getDatabaseTester() {
    return databaseTester;
  }

}
