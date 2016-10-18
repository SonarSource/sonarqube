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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
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
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.organization.OrganizationDbTester;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.organization.OrganizationTesting;
import org.sonar.db.permission.template.PermissionTemplateDbTester;
import org.sonar.db.user.RootFlagAssertions;
import org.sonar.db.user.UserDbTester;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
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
  private boolean disableDefaultOrganization = false;
  private boolean started = false;
  private OrganizationDto defaultOrganization;

  private final UserDbTester userTester;
  private final ComponentDbTester componentTester;
  private final OrganizationDbTester organizationTester;
  private final PermissionTemplateDbTester permissionTemplateTester;
  private final RootFlagAssertions rootFlagAssertions;

  private DbTester(System2 system2, @Nullable String schemaPath) {
    this.system2 = system2;
    this.db = TestDb.create(schemaPath);
    initDbClient();
    this.userTester = new UserDbTester(this);
    this.componentTester = new ComponentDbTester(this);
    this.organizationTester = new OrganizationDbTester(this);
    this.permissionTemplateTester = new PermissionTemplateDbTester(this);
    this.rootFlagAssertions = new RootFlagAssertions(this);
  }

  public static DbTester create(System2 system2) {
    return new DbTester(system2, null);
  }

  public static DbTester createForSchema(System2 system2, Class testClass, String filename) {
    String path = StringUtils.replaceChars(testClass.getCanonicalName(), '.', '/');
    String schemaPath = path + "/" + filename;
    return new DbTester(system2, schemaPath).setDisableDefaultOrganization(true);
  }

  private void initDbClient() {
    TransientPicoContainer ioc = new TransientPicoContainer();
    ioc.addComponent(db.getMyBatis());
    ioc.addComponent(system2);
    ioc.addComponent(new SequenceUuidFactory());
    for (Class daoClass : DaoModule.classes()) {
      ioc.addComponent(daoClass);
    }
    List<Dao> daos = ioc.getComponents(Dao.class);
    client = new DbClient(db.getDatabase(), db.getMyBatis(), daos.toArray(new Dao[daos.size()]));
  }

  public DbTester setDisableDefaultOrganization(boolean b) {
    checkState(!started, "DbTester is already started");
    this.disableDefaultOrganization = b;
    return this;
  }

  @Override
  protected void before() throws Throwable {
    db.start();
    db.truncateTables();
    initDbClient();
    if (!disableDefaultOrganization) {
      insertDefaultOrganization();
    }
    started = true;
  }

  private void insertDefaultOrganization() {
    defaultOrganization = OrganizationTesting.newOrganizationDto();
    try (DbSession dbSession = db.getMyBatis().openSession(false)) {
      client.organizationDao().insert(dbSession, defaultOrganization);
      client.internalPropertiesDao().save(dbSession, "organization.default", defaultOrganization.getUuid());
      dbSession.commit();
    }
  }

  public OrganizationDto getDefaultOrganization() {
    checkState(defaultOrganization != null, "Default organization has not been created");
    return defaultOrganization;
  }

  public UserDbTester users() {
    return userTester;
  }

  public ComponentDbTester components() {
    return componentTester;
  }

  public OrganizationDbTester organizations() {
    return organizationTester;
  }

  public PermissionTemplateDbTester permissionTemplates() {
    return permissionTemplateTester;
  }

  public RootFlagAssertions rootFlag() {
    return rootFlagAssertions;
  }

  @Override
  protected void after() {
    if (session != null) {
      MyBatis.closeQuietly(session);
    }
    db.stop();
    started = false;
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
    return client;
  }

  public void executeUpdateSql(String sql, Object... params) {
    try (Connection connection = getConnection()) {
      new QueryRunner().update(connection, sql, params);
    } catch (SQLException e) {
      SQLException nextException = e.getNextException();
      if (nextException != null) {
        throw new IllegalStateException("Fail to execute sql: " + sql,
          new SQLException(e.getMessage(), nextException.getSQLState(), nextException.getErrorCode(), nextException));
      }
      throw new IllegalStateException("Fail to execute sql: " + sql, e);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute sql: " + sql, e);
    }
  }

  public void executeDdl(String ddl) {
    try (Connection connection = getConnection();
      Statement stmt = connection.createStatement()) {
      stmt.execute(ddl);
    } catch (SQLException e) {
      throw new IllegalStateException("Failed to execute DDL: " + ddl, e);
    }
  }

  /**
   * Very simple helper method to insert some data into a table.
   * It's the responsibility of the caller to convert column values to string.
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
    return countRowsOfTable(tableName, new NewConnectionSupplier());
  }

  public int countRowsOfTable(DbSession dbSession, String tableName) {
    return countRowsOfTable(tableName, new DbSessionConnectionSupplier(dbSession));
  }

  private int countRowsOfTable(String tableName, ConnectionSupplier connectionSupplier) {
    checkArgument(StringUtils.containsNone(tableName, " "), "Parameter must be the name of a table. Got " + tableName);
    return countSql("select count(1) from " + tableName.toLowerCase(Locale.ENGLISH), connectionSupplier);
  }

  /**
   * Executes a SQL request starting with "SELECT COUNT(something) FROM", for example:
   * <pre>int OpenIssues = countSql("select count('id') from issues where status is not null")</pre>
   */
  public int countSql(String sql) {
    return countSql(sql, new NewConnectionSupplier());
  }

  public int countSql(DbSession dbSession, String sql) {
    return countSql(sql, new DbSessionConnectionSupplier(dbSession));
  }

  private int countSql(String sql, ConnectionSupplier connectionSupplier) {
    checkArgument(StringUtils.contains(sql, "count("),
      "Parameter must be a SQL request containing 'count(x)' function. Got " + sql);
    try (
      ConnectionSupplier supplier = connectionSupplier;
      PreparedStatement stmt = supplier.get().prepareStatement(sql);
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
    return select(selectSql, new NewConnectionSupplier());
  }

  public List<Map<String, Object>> select(DbSession dbSession, String selectSql) {
    return select(selectSql, new DbSessionConnectionSupplier(dbSession));
  }

  private List<Map<String, Object>> select(String selectSql, ConnectionSupplier connectionSupplier) {
    try (
      ConnectionSupplier supplier = connectionSupplier;
      PreparedStatement stmt = supplier.get().prepareStatement(selectSql);
      ResultSet rs = stmt.executeQuery()) {
      return getHashMap(rs);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to execute sql: " + selectSql, e);
    }
  }

  public Map<String, Object> selectFirst(String selectSql) {
    return selectFirst(selectSql, new NewConnectionSupplier());
  }

  public Map<String, Object> selectFirst(DbSession dbSession, String selectSql) {
    return selectFirst(selectSql, new DbSessionConnectionSupplier(dbSession));
  }

  private Map<String, Object> selectFirst(String selectSql, ConnectionSupplier connectionSupplier) {
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

  public void assertPrimaryKey(String tableName, @Nullable String expectedPkName, String columnName, String... otherColumnNames) {
    try (Connection connection = getConnection()) {
      PK pk = pkOf(connection, tableName.toUpperCase(Locale.ENGLISH));
      if (pk == null) {
        pkOf(connection, tableName.toLowerCase(Locale.ENGLISH));
      }
      assertThat(pk).as("No primary key is defined on table %s", tableName).isNotNull();
      if (expectedPkName != null) {
        assertThat(pk.getName()).isEqualToIgnoringCase(expectedPkName);
      }
      List<String> expectedColumns = ImmutableList.copyOf(Iterables.concat(Collections.singletonList(columnName), Arrays.asList(otherColumnNames)));
      assertThat(pk.getColumns()).as("Primary key does not have the '%s' expected columns", expectedColumns.size()).hasSize(expectedColumns.size());

      Iterator<String> expectedColumnsIt = expectedColumns.iterator();
      Iterator<String> actualColumnsIt = pk.getColumns().iterator();
      while (expectedColumnsIt.hasNext() && actualColumnsIt.hasNext()) {
        assertThat(actualColumnsIt.next()).isEqualToIgnoringCase(expectedColumnsIt.next());
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to check primary key", e);
    }
  }

  @CheckForNull
  private PK pkOf(Connection connection, String tableName) throws SQLException {
    try (ResultSet resultSet = connection.getMetaData().getPrimaryKeys(null, null, tableName)) {
      String pkName = null;
      ArrayList<String> columnNames = null;
      while (resultSet.next()) {
        if (columnNames == null) {
          pkName = resultSet.getString("PK_NAME");
          columnNames = new ArrayList<>(1);
        } else {
          assertThat(pkName).as("Multiple primary keys found").isEqualTo(resultSet.getString("PK_NAME"));
        }
        columnNames.add(resultSet.getInt("KEY_SEQ") - 1, resultSet.getString("COLUMN_NAME"));
      }
      if (columnNames == null) {
        return null;
      }
      return new PK(pkName, columnNames);
    }
  }

  private static class PK {
    @CheckForNull
    private final String name;
    private final List<String> columns;

    private PK(@Nullable String name, List<String> columns) {
      this.name = name;
      this.columns = ImmutableList.copyOf(columns);
    }

    @CheckForNull
    public String getName() {
      return name;
    }

    public List<String> getColumns() {
      return columns;
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
      throw new IllegalStateException("Fail to get column index");
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
   * An {@link AutoCloseable} supplier of {@link Connection}.
   */
  private interface ConnectionSupplier extends AutoCloseable {
    Connection get() throws SQLException;

    @Override
    void close();
  }

  private static class DbSessionConnectionSupplier implements ConnectionSupplier {
    private final DbSession dbSession;

    public DbSessionConnectionSupplier(DbSession dbSession) {
      this.dbSession = dbSession;
    }

    @Override
    public Connection get() throws SQLException {
      return dbSession.getConnection();
    }

    @Override
    public void close() {
      // closing dbSession is not our responsability
    }
  }

  private class NewConnectionSupplier implements ConnectionSupplier {
    private Connection connection;

    @Override
    public Connection get() throws SQLException {
      if (this.connection == null) {
        this.connection = getConnection();
      }
      return this.connection;
    }

    @Override
    public void close() {
      if (this.connection != null) {
        try {
          this.connection.close();
        } catch (SQLException e) {
          Loggers.get(DbTester.class).warn("Fail to close connection", e);
          // do not re-throw the exception
        }
      }
    }
  }
}
