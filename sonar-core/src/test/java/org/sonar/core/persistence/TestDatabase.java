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

import com.google.common.collect.Maps;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.dbunit.Assertion;
import org.dbunit.DataSourceDatabaseTester;
import org.dbunit.DatabaseUnitException;
import org.dbunit.IDatabaseTester;
import org.dbunit.assertion.DiffCollectingFailureHandler;
import org.dbunit.assertion.Difference;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.ext.mssql.InsertIdentityOperation;
import org.dbunit.operation.DatabaseOperation;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.core.config.Logback;
import org.sonar.core.persistence.dialect.Dialect;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.fail;

public class TestDatabase extends ExternalResource {

  private static final Logger LOG = LoggerFactory.getLogger(TestDatabase.class);

  private static Database db;
  private static DatabaseCommands commands;
  private static IDatabaseTester tester;
  private static MyBatis myBatis;
  private String schemaPath = null;


  public TestDatabase schema(Class baseClass, String filename) {
    String path = StringUtils.replaceChars(baseClass.getCanonicalName(), '.', '/');
    schemaPath = path + "/" + filename;
    return this;
  }

  @Override
  protected void before() throws Throwable {
    if (db == null) {
      Settings settings = new Settings().setProperties(Maps.fromProperties(System.getProperties()));
      if (settings.hasKey("orchestrator.configUrl")) {
        loadOrchestratorSettings(settings);
      }
      for (String key : settings.getKeysStartingWith("sonar.jdbc")) {
        LOG.info(key + ": " + settings.getString(key));
      }
      boolean hasDialect = settings.hasKey("sonar.jdbc.dialect");
      if (hasDialect) {
        db = new DefaultDatabase(settings);
      } else {
        db = new H2Database("h2Tests" + DigestUtils.md5Hex(StringUtils.defaultString(schemaPath)), schemaPath == null);
      }
      db.start();
      if (schemaPath != null) {
        // will fail if not H2
        ((H2Database) db).executeScript(schemaPath);
      }
      LOG.info("Test Database: " + db);

      commands = DatabaseCommands.forDialect(db.getDialect());
      tester = new DataSourceDatabaseTester(db.getDataSource());

      myBatis = new MyBatis(db, settings, new Logback());
      myBatis.start();
    }
    commands.truncateDatabase(db.getDataSource());
  }

  public Database database() {
    return db;
  }

  public Dialect dialect() {
    return db.getDialect();
  }

  public DatabaseCommands commands() {
    return commands;
  }

  public MyBatis myBatis() {
    return myBatis;
  }

  public Connection openConnection() throws SQLException {
    return db.getDataSource().getConnection();
  }

  public void executeUpdateSql(String sql) {
    Connection connection = null;
    try {
      connection = openConnection();
      new QueryRunner().update(connection, sql);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute sql: " + sql);
    } finally {
      DbUtils.commitAndCloseQuietly(connection);
    }
  }

  public void prepareDbUnit(Class testClass, String... testNames) {
    InputStream[] streams = new InputStream[testNames.length];
    try {
      for (int i = 0; i < testNames.length; i++) {
        String path = "/" + testClass.getName().replace('.', '/') + "/" + testNames[i];
        streams[i] = testClass.getResourceAsStream(path);
        if (streams[i] == null) {
          throw new IllegalStateException("DbUnit file not found: " + path);
        }
      }

      prepareDbUnit(streams);
      commands.resetPrimaryKeys(db.getDataSource());
    } catch (SQLException e) {
      throw translateException("Could not setup DBUnit data", e);
    } finally {
      for (InputStream stream : streams) {
        IOUtils.closeQuietly(stream);
      }
    }
  }

  private void prepareDbUnit(InputStream... dataSetStream) {
    IDatabaseConnection connection = null;
    try {
      IDataSet[] dataSets = new IDataSet[dataSetStream.length];
      for (int i = 0; i < dataSetStream.length; i++) {
        dataSets[i] = dbUnitDataSet(dataSetStream[i]);
      }
      tester.setDataSet(new CompositeDataSet(dataSets));
      connection = dbUnitConnection();
      new InsertIdentityOperation(DatabaseOperation.INSERT).execute(connection, tester.getDataSet());
    } catch (Exception e) {
      throw translateException("Could not setup DBUnit data", e);
    } finally {
      closeQuietly(connection);
    }
  }

  public void assertDbUnit(Class testClass, String filename, String... tables) {
    IDatabaseConnection connection = null;
    try {
      connection = dbUnitConnection();
      IDataSet dataSet = connection.createDataSet();
      String path = "/" + testClass.getName().replace('.', '/') + "/" + filename;
      IDataSet expectedDataSet = dbUnitDataSet(testClass.getResourceAsStream(path));
      for (String table : tables) {
        DiffCollectingFailureHandler diffHandler = new DiffCollectingFailureHandler();

        Assertion.assertEquals(expectedDataSet.getTable(table), dataSet.getTable(table), diffHandler);
        // Evaluate the differences and ignore some column values
        List diffList = diffHandler.getDiffList();
        for (Object o : diffList) {
          Difference diff = (Difference) o;
          if (!"[ignore]".equals(diff.getExpectedValue())) {
            throw new DatabaseUnitException(diff.toString());
          }
        }
      }
    } catch (DatabaseUnitException e) {
      fail(e.getMessage());
    } catch (Exception e) {
      throw translateException("Error while checking results", e);
    } finally {
      closeQuietly(connection);
    }
  }

  private IDataSet dbUnitDataSet(InputStream stream) {
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

  private IDatabaseConnection dbUnitConnection() {
    try {
      IDatabaseConnection connection = tester.getConnection();
      connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, commands.getDbUnitFactory());
      return connection;
    } catch (Exception e) {
      throw translateException("Error while getting connection", e);
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

  private static RuntimeException translateException(String msg, Exception cause) {
    RuntimeException runtimeException = new RuntimeException(String.format("%s: [%s] %s", msg, cause.getClass().getName(), cause.getMessage()));
    runtimeException.setStackTrace(cause.getStackTrace());
    return runtimeException;
  }

  private void loadOrchestratorSettings(Settings settings) throws URISyntaxException, IOException {
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

}
