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

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.apache.commons.codec.digest.DigestUtils;
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
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.filter.DefaultColumnFilter;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.ext.mssql.InsertIdentityOperation;
import org.dbunit.operation.DatabaseOperation;
import org.junit.AssumptionViolatedException;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.core.cluster.NullQueue;
import org.sonar.core.config.Logback;
import org.sonar.core.persistence.dialect.Dialect;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * This class should be call using @ClassRule in order to create the schema once (ft @Rule is used
 * the schema will be recreated before each test).
 * Data will be truncated each time you call prepareDbUnit().
 * <p/>
 * File using {@link org.sonar.core.persistence.DbTester} must be annotated with {@link org.sonar.test.DbTests} so
 * that they can be executed on all supported DBs (Oracle, MySQL, ...).
 */
public class DbTester extends ExternalResource {

  private static final Logger LOG = LoggerFactory.getLogger(DbTester.class);

  private Database db;
  private DatabaseCommands commands;
  private IDatabaseTester tester;
  private MyBatis myBatis;
  private String schemaPath = null;

  public DbTester schema(Class baseClass, String filename) {
    String path = StringUtils.replaceChars(baseClass.getCanonicalName(), '.', '/');
    schemaPath = path + "/" + filename;
    return this;
  }

  @Override
  protected void before() throws Throwable {
    Settings settings = new Settings().setProperties(Maps.fromProperties(System.getProperties()));
    if (settings.hasKey("orchestrator.configUrl")) {
      loadOrchestratorSettings(settings);
    }
    String login = settings.getString("sonar.jdbc.username");
    for (String key : settings.getKeysStartingWith("sonar.jdbc")) {
      LOG.info(key + ": " + settings.getString(key));
    }
    String dialect = settings.getString("sonar.jdbc.dialect");
    if (dialect != null && !"h2".equals(dialect)) {
      db = new DefaultDatabase(settings);
    } else {
      db = new H2Database("h2Tests" + DigestUtils.md5Hex(StringUtils.defaultString(schemaPath)), schemaPath == null);
    }
    db.start();
    if (schemaPath != null) {
      // will fail if not H2
      if (db.getDialect().getId().equals("h2")) {
        ((H2Database) db).executeScript(schemaPath);
      } else {
        db.stop();
        throw new AssumptionViolatedException("Test disabled because it supports only H2");
      }
    }
    LOG.info("Test Database: " + db);

    commands = DatabaseCommands.forDialect(db.getDialect());
    tester = new DataSourceDatabaseTester(db.getDataSource(), commands.useLoginAsSchema() ? login : null);

    myBatis = new MyBatis(db, new Logback(), new NullQueue());
    myBatis.start();

    truncateTables();
  }

  public void truncateTables() {
    try {
      commands.truncateDatabase(db.getDataSource());
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to truncate db tables", e);
    }
  }

  @Override
  protected void after() {
    db.stop();
    db = null;
    myBatis = null;
  }

  public Database database() {
    return db;
  }

  public Dialect dialect() {
    return db.getDialect();
  }

  public MyBatis myBatis() {
    return myBatis;
  }

  public Connection openConnection() throws SQLException {
    return db.getDataSource().getConnection();
  }

  public void executeUpdateSql(String sql) {
    try (Connection connection = openConnection()) {
      new QueryRunner().update(connection, sql);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute sql: " + sql);
    }
  }

  /**
   * Returns the number of rows in the table. Example:
   * <pre>int issues = countTable("issues")</pre>
   */
  public int countRowsOfTable(String tableName) {
    Preconditions.checkArgument(StringUtils.containsNone(tableName, " "), "Parameter must be the name of a table. Got " + tableName);
    return countSql("select count(*) from " + tableName);
  }

  /**
   * Executes a SQL request starting with "SELECT COUNT(something) FROM", for example:
   * <pre>int OpenIssues = countSql("select count('id') from issues where status is not null")</pre>
   */
  public int countSql(String sql) {
    Preconditions.checkArgument(StringUtils.contains(sql, "count("),
      "Parameter must be a SQL request containing 'count(x)' function. Got " + sql);
    try (
      Connection connection = openConnection();
      PreparedStatement stmt = connection.prepareStatement(sql);
      ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
      throw new IllegalStateException("No results for " + sql);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute sql: " + sql);
    }
  }

  public List<Map<String, Object>> select(String selectSql) {
    try (
      Connection connection = openConnection();
      PreparedStatement stmt = connection.prepareStatement(selectSql);
      ResultSet rs = stmt.executeQuery()) {
      return getHashMap(rs);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute sql: " + selectSql);
    }
  }

  public Map<String, Object> selectFirst(String selectSql) {
    List<Map<String, Object>> rows = select(selectSql);
    if (rows.isEmpty()) {
      throw new IllegalStateException("No results for " + selectSql);
    } else if (rows.size() > 1) {
      throw new IllegalStateException("Too many results for " + selectSql);
    }
    return rows.get(0);
  }

  private static List<Map<String, Object>> getHashMap(ResultSet resultSet) throws Exception {
    ResultSetMetaData metaData = resultSet.getMetaData();
    int colCount = metaData.getColumnCount();
    List<Map<String, Object>> rows = newArrayList();
    while (resultSet.next()) {
      Map<String, Object> columns = newHashMap();
      for (int i = 1; i <= colCount; i++) {
        Object value = resultSet.getObject(i);
        if (value instanceof Clob) {
          Clob clob = (Clob) value;
          value = IOUtils.toString((clob.getAsciiStream()));
          doClobFree(clob);
        } else if (value instanceof BigDecimal) {
          // In Oracle, INTEGER types are mapped as BigDecimal
          value = ((BigDecimal) value).longValue();
        } else if (value instanceof Integer) {
          // To be consistent, all INTEGER types are mapped as Long
          value = ((Integer) value).longValue();
        }
        columns.put(metaData.getColumnLabel(i), value);
      }
      rows.add(columns);
    }
    return rows;
  }

  public void prepareDbUnit(Class testClass, String... testNames) {
    InputStream[] streams = new InputStream[testNames.length];
    try {
      // Purge previous data
      commands.truncateDatabase(db.getDataSource());

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
    assertDbUnit(testClass, filename, new String[0], tables);
  }

  public void assertDbUnit(Class testClass, String filename, String[] excludedColumnNames, String... tables) {
    IDatabaseConnection connection = null;
    try {
      connection = dbUnitConnection();

      IDataSet dataSet = connection.createDataSet();
      String path = "/" + testClass.getName().replace('.', '/') + "/" + filename;
      InputStream inputStream = testClass.getResourceAsStream(path);
      if (inputStream == null) {
        throw new IllegalStateException(String.format("File '%s' does not exist", path));
      }
      IDataSet expectedDataSet = dbUnitDataSet(inputStream);
      for (String table : tables) {
        DiffCollectingFailureHandler diffHandler = new DiffCollectingFailureHandler();

        ITable filteredTable = DefaultColumnFilter.excludedColumnsTable(dataSet.getTable(table), excludedColumnNames);
        ITable filteredExpectedTable = DefaultColumnFilter.excludedColumnsTable(expectedDataSet.getTable(table), excludedColumnNames);
        Assertion.assertEquals(filteredExpectedTable, filteredTable, diffHandler);
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

  public void assertColumnDefinition(String table, String column, int expectedType, @Nullable Integer expectedSize) {
    try (Connection connection = openConnection();
      PreparedStatement stmt = connection.prepareStatement("select * from " + table);
      ResultSet res = stmt.executeQuery()) {
      Integer columnIndex = getColumnIndex(res, column);
      if (columnIndex == null) {
        fail("The column '" + column + "' does not exist");
      }

      assertThat(res.getMetaData().getColumnType(columnIndex)).isEqualTo(expectedType);
      if (expectedSize != null) {
        assertThat(res.getMetaData().getColumnDisplaySize(columnIndex)).isEqualTo(expectedSize);
      }

    } catch (Exception e) {
      throw new IllegalStateException("Fail to check column");
    }
  }

  @CheckForNull
  private Integer getColumnIndex(ResultSet res, String column) {
    try {
      ResultSetMetaData meta = res.getMetaData();
      int numCol = meta.getColumnCount();
      for (int i = 1; i < numCol + 1; i++) {
        if (meta.getColumnLabel(i).toLowerCase().equals(column.toLowerCase())) {
          return i;
        }
      }
      return null;

    } catch (Exception e) {
      throw new IllegalStateException("Fail to get column idnex");
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

  private static void doClobFree(Clob clob) throws SQLException {
    try {
      clob.free();
    } catch (AbstractMethodError e){
      // JTS driver do not implement free() as it's using JDBC 3.0
    }
  }
}
