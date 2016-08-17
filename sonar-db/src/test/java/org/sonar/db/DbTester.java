/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db;

import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.dbunit.Assertion;
import org.dbunit.DatabaseUnitException;
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
import org.junit.rules.ExternalResource;
import org.picocontainer.containers.TransientPicoContainer;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.sql.ResultSetMetaData.columnNoNulls;
import static java.sql.ResultSetMetaData.columnNullable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * This class should be called using @Rule.
 * Data is truncated between each tests. The schema is created between each test.
 */
public class DbTester extends ExternalResource {

  private static final Joiner COMMA_JOINER = Joiner.on(", ");
  private final System2 system2;
  private final TestDb db;
  private DbClient client;
  private DbSession session = null;

  private DbTester(System2 system2, @Nullable String schemaPath) {
    this.system2 = system2;
    this.db = TestDb.create(schemaPath);
  }

  public static DbTester create(System2 system2) {
    return new DbTester(system2, null);
  }

  public static DbTester createForSchema(System2 system2, Class testClass, String filename) {
    String path = StringUtils.replaceChars(testClass.getCanonicalName(), '.', '/');
    String schemaPath = path + "/" + filename;
    return new DbTester(system2, schemaPath);
  }

  @Override
  protected void before() throws Throwable {
    db.start();
    db.truncateTables();
  }

  @Override
  protected void after() {
    if (session != null) {
      MyBatis.closeQuietly(session);
    }
    db.stop();
  }

  public DbSession getSession() {
    if (session == null) {
      session = db.getMyBatis().openSession(false);
    }
    return session;
  }

  public void commit() {
    getSession().commit();
  }

  public DbClient getDbClient() {
    if (client == null) {
      TransientPicoContainer ioc = new TransientPicoContainer();
      ioc.addComponent(db.getMyBatis());
      ioc.addComponent(system2);
      for (Class daoClass : DaoModule.classes()) {
        ioc.addComponent(daoClass);
      }
      List<Dao> daos = ioc.getComponents(Dao.class);
      client = new DbClient(db.getDatabase(), db.getMyBatis(), daos.toArray(new Dao[daos.size()]));
    }
    return client;
  }

  public void executeUpdateSql(String sql, Object... params) {
    try (Connection connection = getConnection()) {
      new QueryRunner().update(connection, sql, params);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute sql: " + sql, e);
    }
  }

  /**
   * Very simple helper method to insert some data into a table.
   * It's the responsibility of the caller to convert column values to string.
   *
   * @param valuesByColumn column name and value pairs, if any value is null, the associated column won't be inserted
   */
  public void executeInsert(String table, String firstColumn, Object... others) {
    executeInsert(table, mapOf(firstColumn, others));
  }

  private static Map<String, Object> mapOf(String firstColumn, Object... values) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    List<Object> args = Lists.asList(firstColumn, values);
    for (int i = 0; i < args.size(); i++) {
      String key = args.get(i).toString();
      Object value = args.get(i + 1);
      if (value != null) {
        builder.put(key, value);
      }
      i++;
    }
    return builder.build();
  }

  /**
   * Very simple helper method to insert some data into a table.
   * It's the responsibility of the caller to convert column values to string.
   */
  public void executeInsert(String table, Map<String, Object> valuesByColumn) {
    if (valuesByColumn.isEmpty()) {
      throw new IllegalArgumentException("Values cannot be empty");
    }

    String sql = "insert into " + table.toLowerCase(Locale.ENGLISH) + " (" +
      COMMA_JOINER.join(valuesByColumn.keySet()) +
      ") values (" +
      COMMA_JOINER.join(Collections.nCopies(valuesByColumn.size(), '?')) +
      ")";
    executeUpdateSql(sql, valuesByColumn.values().toArray(new Object[valuesByColumn.size()]));
  }

  /**
   * Returns the number of rows in the table. Example:
   * <pre>int issues = countRowsOfTable("issues")</pre>
   */
  public int countRowsOfTable(String tableName) {
    return countRowsOfTable(tableName, this::getConnection);
  }

  public int countRowsOfTable(DbSession dbSession, String tableName) {
    return countRowsOfTable(tableName, () -> dbSession.getConnection());
  }

  private int countRowsOfTable(String tableName, SqlExceptionSupplier<Connection> connectionSupplier) {
    checkArgument(StringUtils.containsNone(tableName, " "), "Parameter must be the name of a table. Got " + tableName);
    return countSql("select count(1) from " + tableName.toLowerCase(Locale.ENGLISH), connectionSupplier);
  }

  /**
   * Executes a SQL request starting with "SELECT COUNT(something) FROM", for example:
   * <pre>int OpenIssues = countSql("select count('id') from issues where status is not null")</pre>
   */
  public int countSql(String sql) {
    return countSql(sql, this::getConnection);
  }

  public int countSql(DbSession dbSession, String sql) {
    return countSql(sql, () -> dbSession.getConnection());
  }

  private int countSql(String sql, SqlExceptionSupplier<Connection> connectionSupplier) {
    checkArgument(StringUtils.contains(sql, "count("),
      "Parameter must be a SQL request containing 'count(x)' function. Got " + sql);
    try (
      Connection connection = connectionSupplier.get();
      PreparedStatement stmt = connection.prepareStatement(sql);
      ResultSet rs = stmt.executeQuery()) {
      if (rs.next()) {
        return rs.getInt(1);
      }
      throw new IllegalStateException("No results for " + sql);

    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute sql: " + sql, e);
    }
  }

  public List<Map<String, Object>> select(String selectSql) {
    return select(selectSql, this::getConnection);
  }

  private List<Map<String, Object>> select(String selectSql, SqlExceptionSupplier<Connection> connectionSupplier) {
    try (
      Connection connection = connectionSupplier.get();
      PreparedStatement stmt = connection.prepareStatement(selectSql);
      ResultSet rs = stmt.executeQuery()) {
      return getHashMap(rs);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute sql: " + selectSql, e);
    }
  }

  public Map<String, Object> selectFirst(String selectSql) {
    return selectFirst(selectSql, this::getConnection);
  }

  public Map<String, Object> selectFirst(DbSession dbSession, String selectSql) {
    return selectFirst(selectSql, () -> dbSession.getConnection());
  }

  private Map<String, Object> selectFirst(String selectSql, SqlExceptionSupplier<Connection> connectionSupplier) {
    List<Map<String, Object>> rows = select(selectSql, connectionSupplier);
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
          BigDecimal bgValue = ((BigDecimal) value);
          if (bgValue.scale() == 0) {
            value = bgValue.longValue();
          } else {
            value = bgValue.doubleValue();
          }
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
      for (int i = 0; i < testNames.length; i++) {
        String path = "/" + testClass.getName().replace('.', '/') + "/" + testNames[i];
        streams[i] = testClass.getResourceAsStream(path);
        if (streams[i] == null) {
          throw new IllegalStateException("DbUnit file not found: " + path);
        }
      }

      prepareDbUnit(streams);
      db.getCommands().resetPrimaryKeys(db.getDatabase().getDataSource());
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
      db.getDbUnitTester().setDataSet(new CompositeDataSet(dataSets));
      connection = dbUnitConnection();
      new InsertIdentityOperation(DatabaseOperation.INSERT).execute(connection, db.getDbUnitTester().getDataSet());
    } catch (Exception e) {
      throw translateException("Could not setup DBUnit data", e);
    } finally {
      closeQuietly(connection);
    }
  }

  public void assertDbUnitTable(Class testClass, String filename, String table, String... columns) {
    IDatabaseConnection connection = dbUnitConnection();
    try {
      IDataSet dataSet = connection.createDataSet();
      String path = "/" + testClass.getName().replace('.', '/') + "/" + filename;
      IDataSet expectedDataSet = dbUnitDataSet(testClass.getResourceAsStream(path));
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
      e.printStackTrace();
      fail(e.getMessage());
    } catch (Exception e) {
      throw translateException("Error while checking results", e);
    } finally {
      closeQuietly(connection);
    }
  }

  public void assertColumnDefinition(String table, String column, int expectedType, @Nullable Integer expectedSize) {
    assertColumnDefinition(table, column, expectedType, expectedSize, null);
  }

  public void assertColumnDefinition(String table, String column, int expectedType, @Nullable Integer expectedSize, @Nullable Boolean isNullable) {
    try (Connection connection = getConnection();
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
      if (isNullable != null) {
        assertThat(res.getMetaData().isNullable(columnIndex)).isEqualTo(isNullable ? columnNullable : columnNoNulls);
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to check column", e);
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
      IDatabaseConnection connection = db.getDbUnitTester().getConnection();
      connection.getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, db.getDbUnitFactory());
      return connection;
    } catch (Exception e) {
      throw translateException("Error while getting connection", e);
    }
  }

  private void closeQuietly(@Nullable IDatabaseConnection connection) {
    try {
      if (connection != null) {
        connection.close();
      }
    } catch (SQLException e) {
      // ignore
    }
  }

  public static RuntimeException translateException(String msg, Exception cause) {
    RuntimeException runtimeException = new RuntimeException(String.format("%s: [%s] %s", msg, cause.getClass().getName(), cause.getMessage()));
    runtimeException.setStackTrace(cause.getStackTrace());
    return runtimeException;
  }

  private static void doClobFree(Clob clob) throws SQLException {
    try {
      clob.free();
    } catch (AbstractMethodError e) {
      // JTS driver do not implement free() as it's using JDBC 3.0
    }
  }

  @Deprecated
  public MyBatis myBatis() {
    return db.getMyBatis();
  }

  @Deprecated
  public Connection openConnection() throws Exception {
    return getConnection();
  }

  private Connection getConnection() throws SQLException {
    return db.getDatabase().getDataSource().getConnection();
  }

  @Deprecated
  public Database database() {
    return db.getDatabase();
  }

  public DatabaseCommands getCommands() {
    return db.getCommands();
  }

  /**
   * A {@link Supplier} that declares the checked exception {@link SQLException}.
   */
  @FunctionalInterface
  private interface SqlExceptionSupplier<T> {
    T get() throws SQLException;
  }

}
