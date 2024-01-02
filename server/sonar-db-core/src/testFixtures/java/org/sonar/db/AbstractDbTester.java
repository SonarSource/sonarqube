/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import com.google.common.collect.Ordering;
import java.math.BigDecimal;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.rules.ExternalResource;
import org.slf4j.LoggerFactory;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.asList;
import static java.sql.Types.BIGINT;
import static java.sql.Types.BIT;
import static java.sql.Types.BOOLEAN;
import static java.sql.Types.INTEGER;
import static java.sql.Types.NUMERIC;
import static java.sql.Types.NVARCHAR;
import static java.sql.Types.VARCHAR;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;

public class AbstractDbTester<T extends TestDb> extends ExternalResource {
  private static final Joiner COMMA_JOINER = Joiner.on(", ");

  private static final Pattern INDEX_NAME_PATTERN = Pattern.compile("COALESCE\\(([\\w_]*),");

  private static final Map<Integer, Integer> POSTGRES_TYPE_SUBSTITUTION = Map.of(BOOLEAN, BIT);
  private static final Map<Integer, Integer> MSSQL_TYPE_SUBSTITUTION = Map.of(
    BOOLEAN, BIT,
    VARCHAR, NVARCHAR);

  private static final Map<Integer, Integer> ORACLE_TYPE_SUBSTITUTION = Map.of(
    BOOLEAN, NUMERIC,
    BIGINT, NUMERIC,
    INTEGER, NUMERIC);

  protected final T db;

  public AbstractDbTester(T db) {
    this.db = db;
  }

  public T getDb() {
    return db;
  }

  public void executeUpdateSql(String sql, Object... params) {
    try (Connection connection = getConnection()) {
      new QueryRunner().update(connection, sql, params);
      if (!connection.getAutoCommit()) {
        connection.commit();
      }
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
    List<Object> args = asList(firstColumn, values);
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
                 COMMA_JOINER.join(valuesByColumn.keySet().stream().map(t -> t.toLowerCase(Locale.ENGLISH)).toArray(String[]::new)) +
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

  protected int countRowsOfTable(String tableName, ConnectionSupplier connectionSupplier) {
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

  protected int countSql(String sql, ConnectionSupplier connectionSupplier) {
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

  protected List<Map<String, Object>> select(String selectSql, ConnectionSupplier connectionSupplier) {
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

  protected Map<String, Object> selectFirst(String selectSql, ConnectionSupplier connectionSupplier) {
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
    List<Map<String, Object>> rows = new ArrayList<>();
    while (resultSet.next()) {
      Map<String, Object> columns = new CaseInsensitiveMap<>();
      for (int i = 1; i <= colCount; i++) {
        Object value = resultSet.getObject(i);
        if (value instanceof Clob) {
          Clob clob = (Clob) value;
          value = IOUtils.toString((clob.getAsciiStream()));
          doClobFree(clob);
        } else if (value instanceof BigDecimal) {
          // In Oracle, INTEGER types are mapped as BigDecimal
          BigDecimal bgValue = ((BigDecimal) value);
          if (metaData.getPrecision(i) == 1) {
            // In Oracle, Boolean are considered BigDecimal with a precision of 1.
            value = bgValue.compareTo(BigDecimal.ONE) == 0;
          } else if (bgValue.scale() == 0) {
            value = bgValue.longValue();
          } else {
            value = bgValue.doubleValue();
          }
        } else if (value instanceof Integer) {
          // To be consistent, all INTEGER types are mapped as Long
          value = ((Integer) value).longValue();
        } else if (value instanceof Byte) {
          Byte byteValue = (Byte) value;
          value = byteValue.intValue();
        } else if (value instanceof Timestamp) {
          value = new Date(((Timestamp) value).getTime());
        } else if (value instanceof oracle.sql.TIMESTAMP) {
          value = ((oracle.sql.TIMESTAMP) value).dateValue();
        }
        columns.put(metaData.getColumnLabel(i), value);
      }
      rows.add(columns);
    }
    return rows;
  }

  public void renameIndex(String tableName, String indexName, String newIndexName) {
    if (db.getDatabase().getDialect().getId().equals(MsSql.ID)) {
      executeUpdateSql(String.format("EXEC sp_rename N'%s.%s', N'%s', N'INDEX'", tableName, indexName, newIndexName));
    } else {
      executeUpdateSql(String.format("ALTER INDEX %s RENAME TO %s", indexName, newIndexName));
    }
  }

  public void assertColumnDefinition(String table, String column, int expectedType, @Nullable Integer expectedSize,
    @Nullable Boolean isNullable) {
    try (Connection connection = getConnection();
         ResultSet rs = connection.getMetaData().getColumns(null, null, toVendorCase(table), toVendorCase(column))) {
      boolean exists = false;

      while (rs.next()) {
        if (column.equalsIgnoreCase(rs.getString("COLUMN_NAME"))) {
          exists = true;
          assertThat(rs.getInt("DATA_TYPE")).isEqualTo(getDBType(expectedType));
          if (expectedSize != null) {
            assertThat(rs.getInt("COLUMN_SIZE")).isEqualTo(expectedSize);
          }
          if (isNullable != null) {
            assertThat(rs.getString("IS_NULLABLE")).isEqualTo(isNullable ? "YES" : "NO");
          }
        }
      }

      if (!exists) {
        fail("The column '" + column + "' does not exist");
      }
    } catch (Exception e) {
      throw new IllegalStateException("Fail to check column", e);
    }
  }

  private int getDBType(int expectedType) {
    return switch (db.getDatabase().getDialect().getId()) {
      case PostgreSql.ID -> POSTGRES_TYPE_SUBSTITUTION.getOrDefault(expectedType, expectedType);
      case MsSql.ID -> MSSQL_TYPE_SUBSTITUTION.getOrDefault(expectedType, expectedType);
      case Oracle.ID -> ORACLE_TYPE_SUBSTITUTION.getOrDefault(expectedType, expectedType);
      default -> expectedType;
    };
  }

  public void assertColumnDoesNotExist(String table, String column) throws SQLException {
    try (Connection connection = getConnection();
         PreparedStatement stmt = connection.prepareStatement("select * from " + table);
         ResultSet res = stmt.executeQuery()) {
      assertThat(getColumnNames(res)).doesNotContain(column);
    }
  }

  public void assertTableDoesNotExist(String table) {
    assertTableExists(table, false);
  }

  public void assertTableExists(String table) {
    assertTableExists(table, true);
  }

  private void assertTableExists(String table, boolean expected) {
    try (Connection connection = getConnection()) {
      boolean tableExists = DatabaseUtils.tableExists(table, connection);
      assertThat(tableExists).isEqualTo(expected);
    } catch (Exception e) {
      throw new IllegalStateException("Fail to check if table exists", e);
    }
  }

  /**
   * Verify that non-unique index exists on columns
   */
  public void assertIndex(String tableName, String indexName, String expectedColumn, String... expectedSecondaryColumns) {
    assertIndexImpl(tableName, indexName, false, expectedColumn, expectedSecondaryColumns);
  }

  /**
   * Verify that unique index exists on columns
   */
  public void assertUniqueIndex(String tableName, String indexName, String expectedColumn, String... expectedSecondaryColumns) {
    assertIndexImpl(tableName, indexName, true, expectedColumn, expectedSecondaryColumns);
  }

  private void assertIndexImpl(String tableName, String indexName, boolean expectedUnique, String expectedColumn, String... expectedSecondaryColumns) {
    try (Connection connection = getConnection();
         ResultSet rs = connection.getMetaData().getIndexInfo(null, null, toVendorCase(tableName), false, false)) {
      List<String> onColumns = new ArrayList<>();

      while (rs.next()) {
        if (indexName.equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
          assertThat(rs.getBoolean("NON_UNIQUE")).isEqualTo(!expectedUnique);
          int position = rs.getInt("ORDINAL_POSITION");
          String columnDefinition = rs.getString("COLUMN_NAME");
          String columnName = getColumnNameFromDefinition(columnDefinition);
          onColumns.add(position - 1, columnName.toLowerCase(Locale.ENGLISH));
        }
      }
      assertThat(onColumns).containsExactlyInAnyOrderElementsOf(asList(expectedColumn, expectedSecondaryColumns));
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to check index", e);
    }
  }

  /**
   * Column definition is not necessarly the column name, as we also create index with special expression
   * see {@link org.sonar.server.platform.db.migration.sql.CreateIndexBuilder#createSqlStatement()}
   */
  private static String getColumnNameFromDefinition(String columnDefinition) {
    Matcher matcher = INDEX_NAME_PATTERN.matcher(columnDefinition);
    String columnName;
    if (matcher.find()) {
      columnName = matcher.group(1);
    } else {
      columnName = columnDefinition;
    }
    return columnName;
  }

  /**
   * Verify that index with name {@code indexName} does not exist on the table {@code tableName}
   */
  public void assertIndexDoesNotExist(String tableName, String indexName) {
    try (Connection connection = getConnection();
         ResultSet rs = connection.getMetaData().getIndexInfo(null, null, tableName.toUpperCase(Locale.ENGLISH), false, false)) {
      List<String> indices = new ArrayList<>();
      while (rs.next()) {
        if (rs.getString("INDEX_NAME") != null) {
          indices.add(rs.getString("INDEX_NAME").toLowerCase(Locale.ENGLISH));
        }
      }
      assertThat(indices).doesNotContain(indexName);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to check existence of index", e);
    }
  }

  public void assertPrimaryKey(String tableName, @Nullable String expectedPkName, String columnName, String... otherColumnNames) {
    try (Connection connection = getConnection()) {
      PK pk = pkOf(connection, toVendorCase(tableName));
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

  public void assertNoPrimaryKey(String tableName) {
    try (Connection connection = getConnection()) {
      PK pk = pkOf(connection, tableName.toUpperCase(Locale.ENGLISH));
      if (pk == null) {
        pkOf(connection, tableName.toLowerCase(Locale.ENGLISH));
      }
      assertThat(pk).as("Primary key is still defined on table %s", tableName).isNull();
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to check primary key", e);
    }
  }

  @CheckForNull
  private PK pkOf(Connection connection, String tableName) throws SQLException {
    try (ResultSet resultSet = connection.getMetaData().getPrimaryKeys(null, null, tableName)) {
      String pkName = null;
      List<PkColumn> columnNames = null;
      while (resultSet.next()) {
        if (columnNames == null) {
          pkName = resultSet.getString("PK_NAME");
          columnNames = new ArrayList<>(1);
        } else {
          assertThat(pkName).as("Multiple primary keys found").isEqualTo(resultSet.getString("PK_NAME"));
        }
        columnNames.add(new PkColumn(resultSet.getInt("KEY_SEQ") - 1, resultSet.getString("COLUMN_NAME")));
      }
      if (columnNames == null) {
        return null;
      }
      return new PK(
        pkName,
        columnNames.stream()
          .sorted(PkColumn.ORDERING_BY_INDEX)
          .map(PkColumn::getName)
          .toList());
    }
  }

  public String toVendorCase(String input) {
    if (List.of(H2.ID, Oracle.ID).contains(db.getDatabase().getDialect().getId())) {
      return input.toUpperCase(Locale.ENGLISH);
    }
    return input.toLowerCase(Locale.ENGLISH);
  }

  private static final class PkColumn {
    private static final Ordering<PkColumn> ORDERING_BY_INDEX = Ordering.natural().onResultOf(PkColumn::getIndex);

    /**
     * 0-based
     */
    private final int index;
    private final String name;

    private PkColumn(int index, String name) {
      this.index = index;
      this.name = name;
    }

    public int getIndex() {
      return index;
    }

    public String getName() {
      return name;
    }
  }

  @CheckForNull
  private Integer getColumnIndex(ResultSet res, String column) {
    try {
      ResultSetMetaData meta = res.getMetaData();
      int numCol = meta.getColumnCount();
      for (int i = 1; i < numCol + 1; i++) {
        if (meta.getColumnLabel(i).equalsIgnoreCase(column)) {
          return i;
        }
      }
      return null;

    } catch (Exception e) {
      throw new IllegalStateException("Fail to get column index");
    }
  }

  private Set<String> getColumnNames(ResultSet res) {
    try {
      Set<String> columnNames = new HashSet<>();
      ResultSetMetaData meta = res.getMetaData();
      int numCol = meta.getColumnCount();
      for (int i = 1; i < numCol + 1; i++) {
        columnNames.add(meta.getColumnLabel(i).toLowerCase());
      }
      return columnNames;
    } catch (Exception e) {
      throw new IllegalStateException("Fail to get column names");
    }
  }

  private static void doClobFree(Clob clob) throws SQLException {
    try {
      clob.free();
    } catch (AbstractMethodError e) {
      // JTS driver do not implement free() as it's using JDBC 3.0
    }
  }

  public Connection openConnection() throws SQLException {
    return getConnection();
  }

  private Connection getConnection() throws SQLException {
    return db.getDatabase().getDataSource().getConnection();
  }

  public Database database() {
    return db.getDatabase();
  }

  /**
   * An {@link AutoCloseable} supplier of {@link Connection}.
   */
  protected interface ConnectionSupplier extends AutoCloseable {
    Connection get() throws SQLException;

    @Override
    void close();
  }

  private static class PK {
    @CheckForNull
    private final String name;
    private final List<String> columns;

    private PK(@Nullable String name, List<String> columns) {
      this.name = name;
      this.columns = List.copyOf(columns);
    }

    @CheckForNull
    public String getName() {
      return name;
    }

    public List<String> getColumns() {
      return columns;
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
          LoggerFactory.getLogger(CoreDbTester.class).warn("Fail to close connection", e);
          // do not re-throw the exception
        }
      }
    }
  }
}
