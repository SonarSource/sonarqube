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

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

public class DatabaseUtils {
  private static final String TABLE_NOT_EXIST_MESSAGE = "Can not check that table %s exists";
  public static final int PARTITION_SIZE_FOR_ORACLE = 1000;
  public static final String ORACLE_DRIVER_NAME = "Oracle JDBC driver";
  public static final Pattern ORACLE_OBJECT_NAME_RULE = Pattern.compile("\"[^\"\\u0000]+\"|\\p{L}[\\p{L}\\p{N}_$#@]*");
  public static final String INDEX_NAME_VARIATION = "^idx_\\d+_%s$";
  /**
   * @see DatabaseMetaData#getTableTypes()
   */
  private static final String[] TABLE_TYPE = {"TABLE"};

  protected DatabaseUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static void closeQuietly(@Nullable Connection connection) {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        LoggerFactory.getLogger(DatabaseUtils.class).warn("Fail to close connection", e);
        // ignore
      }
    }
  }

  public static void closeQuietly(@Nullable Statement stmt) {
    if (stmt != null) {
      try {
        stmt.close();
      } catch (SQLException e) {
        LoggerFactory.getLogger(DatabaseUtils.class).warn("Fail to close statement", e);
        // ignore
      }
    }
  }

  public static void closeQuietly(@Nullable ResultSet rs) {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException e) {
        LoggerFactory.getLogger(DatabaseUtils.class).warn("Fail to close result set", e);
        // ignore
      }
    }
  }

  /**
   * Partition by 1000 elements a list of input and execute a function on each part.
   * <p>
   * The goal is to prevent issue with ORACLE when there's more than 1000 elements in a 'in ('X', 'Y', ...)'
   * and with MsSQL when there's more than 2000 parameters in a query
   */
  public static <OUTPUT, INPUT extends Comparable<INPUT>> List<OUTPUT> executeLargeInputs(Collection<INPUT> input, Function<List<INPUT>, List<OUTPUT>> function) {
    return executeLargeInputs(input, function, i -> i);
  }

  /**
   * Partition by 1000 elements a list of input and execute a function on each part.
   * <p>
   * The goal is to prevent issue with ORACLE when there's more than 1000 elements in a 'in ('X', 'Y', ...)'
   * and with MsSQL when there's more than 2000 parameters in a query
   */
  public static <OUTPUT, INPUT extends Comparable<INPUT>> List<OUTPUT> executeLargeInputs(Collection<INPUT> input, Function<List<INPUT>, List<OUTPUT>> function,
    IntFunction<Integer> partitionSizeManipulations) {
    return executeLargeInputs(input, function, size -> size == 0 ? Collections.emptyList() : new ArrayList<>(size), partitionSizeManipulations);
  }

  public static <OUTPUT, INPUT extends Comparable<INPUT>> Set<OUTPUT> executeLargeInputsIntoSet(Collection<INPUT> input, Function<List<INPUT>, Set<OUTPUT>> function,
    IntFunction<Integer> partitionSizeManipulations) {
    return executeLargeInputs(input, function, size -> size == 0 ? Collections.emptySet() : new HashSet<>(size), partitionSizeManipulations);
  }

  private static <OUTPUT, INPUT extends Comparable<INPUT>, RESULT extends Collection<OUTPUT>> RESULT executeLargeInputs(Collection<INPUT> input,
    Function<List<INPUT>, RESULT> function, java.util.function.IntFunction<RESULT> outputInitializer, IntFunction<Integer> partitionSizeManipulations) {
    if (input.isEmpty()) {
      return outputInitializer.apply(0);
    }
    RESULT results = outputInitializer.apply(input.size());
    for (List<INPUT> partition : toUniqueAndSortedPartitions(input, partitionSizeManipulations)) {
      RESULT subResults = function.apply(partition);
      if (subResults != null) {
        results.addAll(subResults);
      }
    }
    return results;
  }

  /**
   * Partition by 1000 elements a list of input and execute a consumer on each part.
   * <p>
   * The goal is to prevent issue with ORACLE when there's more than 1000 elements in a 'in ('X', 'Y', ...)'
   * and with MsSQL when there's more than 2000 parameters in a query
   */
  public static <INPUT extends Comparable<INPUT>> void executeLargeUpdates(Collection<INPUT> inputs, Consumer<List<INPUT>> consumer) {
    executeLargeUpdates(inputs, consumer, i -> i);
  }

  /**
   * Partition by 1000 elements a list of input and execute a consumer on each part.
   * <p>
   * The goal is to prevent issue with ORACLE when there's more than 1000 elements in a 'in ('X', 'Y', ...)'
   * and with MsSQL when there's more than 2000 parameters in a query
   *
   * @param inputs                     the whole list of elements to be partitioned
   * @param consumer                   the mapper method to be executed, for example {@code mapper(dbSession)::selectByUuids}
   * @param partitionSizeManipulations the function that computes the number of usages of a partition, for example
   *                                   {@code partitionSize -> partitionSize / 2} when the partition of elements
   *                                   in used twice in the SQL request.
   */
  public static <INPUT extends Comparable<INPUT>> void executeLargeUpdates(Collection<INPUT> inputs, Consumer<List<INPUT>> consumer,
    IntFunction<Integer> partitionSizeManipulations) {
    Iterable<List<INPUT>> partitions = toUniqueAndSortedPartitions(inputs, partitionSizeManipulations);
    for (List<INPUT> partition : partitions) {
      consumer.accept(partition);
    }
  }

  /**
   * Ensure values {@code inputs} are unique (which avoids useless arguments) and sorted before creating the partition.
   */
  public static <INPUT extends Comparable<INPUT>> Iterable<List<INPUT>> toUniqueAndSortedPartitions(Collection<INPUT> inputs) {
    return toUniqueAndSortedPartitions(inputs, i -> i);
  }

  /**
   * Ensure values {@code inputs} are unique (which avoids useless arguments) and sorted before creating the partition.
   */
  public static <INPUT extends Comparable<INPUT>> Iterable<List<INPUT>> toUniqueAndSortedPartitions(Collection<INPUT> inputs, IntFunction<Integer> partitionSizeManipulations) {
    int partitionSize = partitionSizeManipulations.apply(PARTITION_SIZE_FOR_ORACLE);
    return Iterables.partition(toUniqueAndSortedList(inputs), partitionSize);
  }

  /**
   * Ensure values {@code inputs} are unique (which avoids useless arguments) and sorted so that there is little
   * variations of SQL requests over time as possible with a IN clause and/or a group of OR clauses. Such requests can
   * then be more easily optimized by the SGDB engine.
   */
  public static <INPUT extends Comparable<INPUT>> List<INPUT> toUniqueAndSortedList(Iterable<INPUT> inputs) {
    if (inputs instanceof Set) {
      // inputs are unique but order is not enforced
      return Ordering.natural().immutableSortedCopy(inputs);
    }
    // inputs are not unique and order is not guaranteed
    return Ordering.natural().immutableSortedCopy(Sets.newHashSet(inputs));
  }

  /**
   * Partition by 1000 elements a list of input and execute a consumer on each part.
   * <p>
   * The goal is to prevent issue with ORACLE when there's more than 1000 elements in a 'in ('X', 'Y', ...)'
   * and with MsSQL when there's more than 2000 parameters in a query
   */
  public static <T> void executeLargeInputsWithoutOutput(Collection<T> input, Consumer<List<T>> consumer) {
    if (input.isEmpty()) {
      return;
    }

    List<List<T>> partitions = Lists.partition(new ArrayList<>(input), PARTITION_SIZE_FOR_ORACLE);
    for (List<T> partition : partitions) {
      consumer.accept(partition);
    }
  }

  /**
   * Logback does not log exceptions associated to {@link java.sql.SQLException#getNextException()}.
   * See http://jira.qos.ch/browse/LOGBACK-775
   */
  public static void log(Logger logger, SQLException e) {
    SQLException next = e.getNextException();
    while (next != null) {
      logger.error("SQL error: {}. Message: {}", next.getSQLState(), next.getMessage());
      next = next.getNextException();
    }
  }

  @CheckForNull
  public static Long getLong(ResultSet rs, String columnName) throws SQLException {
    long l = rs.getLong(columnName);
    return rs.wasNull() ? null : l;
  }

  @CheckForNull
  public static Double getDouble(ResultSet rs, String columnName) throws SQLException {
    double d = rs.getDouble(columnName);
    return rs.wasNull() ? null : d;
  }

  @CheckForNull
  public static Integer getInt(ResultSet rs, String columnName) throws SQLException {
    int i = rs.getInt(columnName);
    return rs.wasNull() ? null : i;
  }

  @CheckForNull
  public static String getString(ResultSet rs, String columnName) throws SQLException {
    String s = rs.getString(columnName);
    return rs.wasNull() ? null : s;
  }

  @CheckForNull
  public static Long getLong(ResultSet rs, int columnIndex) throws SQLException {
    long l = rs.getLong(columnIndex);
    return rs.wasNull() ? null : l;
  }

  @CheckForNull
  public static Double getDouble(ResultSet rs, int columnIndex) throws SQLException {
    double d = rs.getDouble(columnIndex);
    return rs.wasNull() ? null : d;
  }

  @CheckForNull
  public static Integer getInt(ResultSet rs, int columnIndex) throws SQLException {
    int i = rs.getInt(columnIndex);
    return rs.wasNull() ? null : i;
  }

  @CheckForNull
  public static String getString(ResultSet rs, int columnIndex) throws SQLException {
    String s = rs.getString(columnIndex);
    return rs.wasNull() ? null : s;
  }

  @CheckForNull
  public static Date getDate(ResultSet rs, int columnIndex) throws SQLException {
    Timestamp t = rs.getTimestamp(columnIndex);
    return rs.wasNull() ? null : new Date(t.getTime());
  }

  /**
   * @param table case-insensitive name of table
   * @return true if a table exists with this name, otherwise false
   * @throws SQLException
   */
  public static boolean tableExists(String table, Connection connection) {
    return doTableExists(table, connection) ||
      doTableExists(table.toLowerCase(Locale.ENGLISH), connection) ||
      doTableExists(table.toUpperCase(Locale.ENGLISH), connection);

  }

  private static boolean doTableExists(String table, Connection connection) {
    String schema = getSchema(connection);

    // table type is used to speed-up Oracle by removing introspection of system tables and aliases.
    try (ResultSet rs = connection.getMetaData().getTables(connection.getCatalog(), schema, table, TABLE_TYPE)) {
      while (rs.next()) {
        String name = rs.getString("TABLE_NAME");
        if (table.equalsIgnoreCase(name)) {
          return true;
        }
      }
      return false;
    } catch (SQLException e) {
      throw wrapSqlException(e, TABLE_NOT_EXIST_MESSAGE, table);
    }
  }

  public static boolean indexExistsIgnoreCase(String table, String index, Connection connection) {
    return doIndexExistsIgnoreIndexCase(table, index, connection) ||
      doIndexExistsIgnoreIndexCase(table.toLowerCase(Locale.ENGLISH), index, connection) ||
      doIndexExistsIgnoreIndexCase(table.toUpperCase(Locale.ENGLISH), index, connection);
  }

  private static boolean doIndexExistsIgnoreIndexCase(String table, String index, Connection connection) {
    return findIndex(connection, table, index).isPresent();
  }

  /**
   * Finds an index by searching by its lower case or upper case name. If an index is found, it's name is returned with the matching case.
   * This is useful when we need to drop an index that could exist with either lower case or upper case name.
   * See SONAR-13594
   * Related to ticket SONAR-17737, some index name can be changed to pattern idx_{number}_index_name. We also want to be able to identify and return them
   */
  public static Optional<String> findExistingIndex(Connection connection, String tableName, String indexName) {
    Predicate<String> indexSelector = idx -> indexName.equalsIgnoreCase(idx) || indexMatchesPattern(idx, format(INDEX_NAME_VARIATION, indexName));

    return findIndex(connection, tableName.toLowerCase(Locale.US), indexSelector)
      .or(() -> findIndex(connection, tableName.toUpperCase(Locale.US), indexSelector));
  }

  private static boolean indexMatchesPattern(@Nullable String idx, String pattern) {
    return idx != null && Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(idx).matches();
  }

  private static Optional<String> findIndex(Connection connection, String tableName, String indexName) {
    return findIndex(connection, tableName, indexName::equalsIgnoreCase);
  }

  private static Optional<String> findIndex(Connection connection, String tableName, Predicate<String> indexMatcher) {
    String schema = getSchema(connection);

    if (StringUtils.isNotEmpty(schema)) {
      String driverName = getDriver(connection);
//      Fix for double quoted schema name in Oracle
      if (ORACLE_DRIVER_NAME.equals(driverName) && !ORACLE_OBJECT_NAME_RULE.matcher(schema).matches()) {
        return getOracleIndex(connection, tableName, indexMatcher, schema);
      }
    }

    return getIndex(connection, tableName, indexMatcher, schema);
  }

  private static Optional<String> getIndex(Connection connection, String tableName, Predicate<String> indexMatcher, @Nullable String schema) {
    try (ResultSet rs = connection.getMetaData().getIndexInfo(connection.getCatalog(), schema, tableName, false, true)) {
      while (rs.next()) {
        String idx = rs.getString("INDEX_NAME");
        if (indexMatcher.test(idx)) {
          return Optional.of(idx);
        }
      }
      return Optional.empty();
    } catch (SQLException e) {
      throw wrapSqlException(e, TABLE_NOT_EXIST_MESSAGE, tableName);
    }
  }

  private static Optional<String> getOracleIndex(Connection connection, String tableName, Predicate<String> indexMatcher, @Nonnull String schema) {
    try (ResultSet rs = connection.getMetaData().getIndexInfo(connection.getCatalog(), null, tableName, false, true)) {
      while (rs.next()) {
        String idx = rs.getString("INDEX_NAME");
        String tableSchema = rs.getString("TABLE_SCHEM");
        if (schema.equalsIgnoreCase(tableSchema) && indexMatcher.test(idx)) {
          return Optional.of(idx);
        }
      }
      return Optional.empty();
    } catch (SQLException e) {
      throw wrapSqlException(e, TABLE_NOT_EXIST_MESSAGE, tableName);
    }
  }

  public static boolean tableColumnExists(Connection connection, String tableName, String columnName) {
    try {
      return columnExists(connection, tableName.toLowerCase(Locale.US), columnName)
        || columnExists(connection, tableName.toUpperCase(Locale.US), columnName);
    } catch (SQLException e) {
      throw wrapSqlException(e, "Can not check that column %s exists", columnName);
    }
  }

  private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
    String schema = getSchema(connection);
    try (ResultSet rs = connection.getMetaData().getColumns(connection.getCatalog(), schema, tableName, null)) {
      while (rs.next()) {
        // this is wrong and could lead to bugs, there is no point of going through each column - only one column contains column name
        // see the contract (javadoc) of java.sql.DatabaseMetaData.getColumns
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
          String name = rs.getString(i);
          if (columnName.equalsIgnoreCase(name)) {
            return true;
          }
        }
      }
      return false;
    }
  }
  @CheckForNull
  public static ColumnMetadata getColumnMetadata(Connection connection, String tableName, String columnName) throws SQLException {
    ColumnMetadata columnMetadataLowerCase = getColumnMetadataWithCaseSensitiveTableName(connection, tableName.toLowerCase(Locale.US), columnName);
    if (columnMetadataLowerCase != null) {
      return columnMetadataLowerCase;
    }
    return getColumnMetadataWithCaseSensitiveTableName(connection, tableName.toUpperCase(Locale.US), columnName);
  }

  @CheckForNull
  public static ColumnMetadata getColumnMetadataWithCaseSensitiveTableName(Connection connection, String tableName, String columnName) throws SQLException {
    String schema = getSchema(connection);
    try (ResultSet rs = connection.getMetaData().getColumns(connection.getCatalog(), schema, tableName, null)) {
      while (rs.next()) {
        String name = rs.getString(4);
        int type = rs.getInt(5);
        int limit = rs.getInt(7);
        boolean nullable = rs.getBoolean(11);
        if (columnName.equalsIgnoreCase(name)) {
          return new ColumnMetadata(name, nullable, type, limit);
        }
      }
      return null;
    }
  }

  @CheckForNull
  static String getDriver(Connection connection) {
    try {
      return connection.getMetaData().getDriverName();
    } catch (SQLException e) {
      LoggerFactory.getLogger(DatabaseUtils.class).warn("Fail to determine database driver.", e);
      return null;
    }
  }

  @CheckForNull
  private static String getSchema(Connection connection) {
    String schema = null;

    try {
      // Using H2 with a JDBC TCP connection is throwing an exception
      // See org.h2.engine.SessionRemote#getCurrentSchemaName()
      if (!"H2 JDBC Driver".equals(connection.getMetaData().getDriverName())) {
        schema = connection.getSchema();
      }
    } catch (SQLException e) {
      LoggerFactory.getLogger(DatabaseUtils.class).warn("Fail to determine schema. Keeping it null for searching tables", e);
    }
    return schema;
  }

  public static IllegalStateException wrapSqlException(SQLException e, String message, Object... messageArgs) {
    return new IllegalStateException(format(message, messageArgs), e);
  }

  /**
   * This method can be used as a method reference, for not to have to handle the checked exception {@link SQLException}
   */
  public static Consumer<String> setStrings(PreparedStatement stmt, IntSupplier index) {
    return value -> {
      try {
        stmt.setString(index.getAsInt(), value);
      } catch (SQLException e) {
        Throwables.propagate(e);
      }
    };
  }

  /**
   * @throws IllegalArgumentException if the collection is not null and has strictly more
   *                                  than {@link #PARTITION_SIZE_FOR_ORACLE} values.
   */
  public static void checkThatNotTooManyConditions(@Nullable Collection<?> values, String message) {
    if (values != null) {
      checkArgument(values.size() <= PARTITION_SIZE_FOR_ORACLE, message);
    }
  }

}
