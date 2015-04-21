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
package org.sonar.core.persistence;

import com.google.common.collect.Maps;
import com.google.common.io.Closeables;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.dbunit.Assertion;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.DatabaseUnitException;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.ext.mssql.InsertIdentityOperation;
import org.dbunit.ext.mysql.MySqlMetadataHandler;
import org.dbunit.operation.DatabaseOperation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.core.cluster.NullQueue;
import org.sonar.core.config.Logback;
import org.sonar.core.persistence.dialect.MySql;
import org.sonar.test.DbTests;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.fail;

/**
 * @deprecated use an instance of {@link org.sonar.core.persistence.DbTester instead} instead,
 * and do no forget to annotated the test class with {@link org.sonar.test.DbTests}.
 */
@Category(DbTests.class)
@Deprecated
public abstract class AbstractDaoTestCase {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractDaoTestCase.class);
  private static Database database;
  private static DatabaseCommands databaseCommands;
  private static MyBatis myBatis;
  private static String login;

  private IDatabaseTester databaseTester;

  @BeforeClass
  public static void startDatabase() throws Exception {
    if (database == null) {
      Settings settings = new Settings().setProperties(Maps.fromProperties(System.getProperties()));
      if (settings.hasKey("orchestrator.configUrl")) {
        loadOrchestratorSettings(settings);
      }
      login = settings.getString("sonar.jdbc.username");
      for (String key : settings.getKeysStartingWith("sonar.jdbc")) {
        LOG.info(key + ": " + settings.getString(key));
      }
      boolean hasDialect = settings.hasKey("sonar.jdbc.dialect");
      if (hasDialect) {
        database = new DefaultDatabase(settings);
      } else {
        database = new H2Database("test", true);
      }
      database.start();
      LOG.info("Test Database: " + database);
      databaseCommands = DatabaseCommands.forDialect(database.getDialect());

      myBatis = new MyBatis(database, new Logback(), new NullQueue());
      myBatis.start();
    }
  }

  /**
   * Orchestrator is the name of a SonarSource close-source library for database and integration testing.
   */
  private static void loadOrchestratorSettings(Settings settings) throws URISyntaxException, IOException {
    String url = settings.getString("orchestrator.configUrl");
    URI uri = new URI(url);
    InputStream input = null;
    try {
      if (url.startsWith("file:")) {
        File file = new File(uri);
        input = FileUtils.openInputStream(file);
      } else {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
          throw new IllegalStateException("Fail to request: " + uri + ". Status code=" + responseCode);
        }

        input = connection.getInputStream();

      }
      Properties props = new Properties();
      props.load(input);
      settings.addProperties(props);
      for (Map.Entry<String, String> entry : settings.getProperties().entrySet()) {
        String interpolatedValue = StrSubstitutor.replace(entry.getValue(), System.getenv(), "${", "}");
        settings.setProperty(entry.getKey(), interpolatedValue);
      }
    } finally {
      IOUtils.closeQuietly(input);
    }
  }

  private static RuntimeException translateException(String msg, Exception cause) {
    RuntimeException runtimeException = new RuntimeException(String.format("%s: [%s] %s", msg, cause.getClass().getName(), cause.getMessage()));
    runtimeException.setStackTrace(cause.getStackTrace());
    return runtimeException;
  }

  @Before
  public void startDbUnit() throws Exception {
    databaseCommands.truncateDatabase(database.getDataSource());
    databaseTester = new DataSourceDatabaseTester(database.getDataSource(), databaseCommands.useLoginAsSchema() ? login : null);
  }

  protected MyBatis getMyBatis() {
    return myBatis;
  }

  protected Database getDatabase() {
    return database;
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
    IDatabaseConnection connection = openDbUnitConnection();
    try {
      IDataSet[] dataSets = new IDataSet[dataSetStream.length];
      for (int i = 0; i < dataSetStream.length; i++) {
        dataSets[i] = getData(dataSetStream[i]);
      }
      databaseTester.setDataSet(new CompositeDataSet(dataSets));
      new InsertIdentityOperation(DatabaseOperation.INSERT).execute(connection, databaseTester.getDataSet());
    } catch (Exception e) {
      throw translateException("Could not setup DBUnit data", e);
    } finally {
      closeDbUnitConnection(connection);
    }
  }

  protected void checkTables(String testName, String... tables) {
    checkTables(testName, new String[0], tables);
  }

  protected void checkTables(String testName, String[] excludedColumnNames, String... tables) {
    IDatabaseConnection connection = openDbUnitConnection();
    try {
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
      closeDbUnitConnection(connection);
    }
  }

  protected void checkTable(String testName, String table, String... columns) {
    IDatabaseConnection connection = openDbUnitConnection();
    try {
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
      closeDbUnitConnection(connection);
    }
  }

  protected void assertEmptyTables(String... emptyTables) {
    IDatabaseConnection connection = openDbUnitConnection();
    try {
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
      closeDbUnitConnection(connection);
    }
  }

  private IDatabaseConnection openDbUnitConnection() {
    try {
      IDatabaseConnection connection = databaseTester.getConnection();
      connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, databaseCommands.getDbUnitFactory());
      connection.getConfig().setFeature(DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, false);
      connection.getConfig().setFeature(DatabaseConfig.FEATURE_SKIP_ORACLE_RECYCLEBIN_TABLES, true);
      if (MySql.ID.equals(database.getDialect().getId())) {
        connection.getConfig().setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, false);
        connection.getConfig().setProperty(DatabaseConfig.PROPERTY_METADATA_HANDLER, new MySqlMetadataHandler());
      }
      return connection;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to open dbunit connection", e);
    }
  }

  private void closeDbUnitConnection(IDatabaseConnection c) {
    try {
      c.close();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to close dbunit connection", e);
    }
  }

  private IDataSet getExpectedData(String testName) {
    String className = getClass().getName();
    String fileName = String.format("/%s/%s-result.xml", className.replace('.', '/'), testName);
    InputStream in = getClass().getResourceAsStream(fileName);
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
      dataSet.addReplacementObject("[false]", Boolean.FALSE);
      dataSet.addReplacementObject("[true]", Boolean.TRUE);
      return dataSet;
    } catch (Exception e) {
      throw translateException("Could not read the dataset stream", e);
    }
  }

  protected Connection getConnection() throws SQLException {
    return database.getDataSource().getConnection();
  }
}
