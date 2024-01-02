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

import com.google.common.base.Function;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.dialect.Oracle;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.DatabaseUtils.ORACLE_DRIVER_NAME;
import static org.sonar.db.DatabaseUtils.toUniqueAndSortedList;

public class DatabaseUtilsTest {
  private static final String DEFAULT_SCHEMA = "public";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(DatabaseUtilsTest.class, "sql.sql", false);
  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void find_index_with_lower_case() throws SQLException {
    String tableName = "SCHEMA_MIGRATIONS";
    String indexName = "lower_case_name";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(DatabaseUtils.findExistingIndex(connection, tableName, indexName)).contains(indexName);
      assertThat(DatabaseUtils.findExistingIndex(connection, tableName.toLowerCase(Locale.US), indexName)).contains(indexName);
    }
  }

  @Test
  public void find_index_with_upper_case() throws SQLException {
    String tableName = "SCHEMA_MIGRATIONS";
    String indexName = "UPPER_CASE_NAME";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(DatabaseUtils.findExistingIndex(connection, tableName, indexName)).contains(indexName);
      assertThat(DatabaseUtils.findExistingIndex(connection, tableName, indexName.toLowerCase(Locale.US))).contains(indexName);
      assertThat(DatabaseUtils.findExistingIndex(connection, tableName.toLowerCase(Locale.US), indexName.toLowerCase(Locale.US))).contains(indexName);
    }
  }

  @Test
  public void find_index_with_special_name() throws SQLException {
    String tableName = "SCHEMA_MIGRATIONS";
    String indexName = "INDEX_NAME";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(DatabaseUtils.findExistingIndex(connection, tableName, indexName)).contains("idx_1234_index_name");
      assertThat(DatabaseUtils.findExistingIndex(connection, tableName.toLowerCase(Locale.US), indexName)).contains("idx_1234_index_name");
      assertThat(DatabaseUtils.findExistingIndex(connection, tableName.toLowerCase(Locale.US), indexName.toLowerCase(Locale.US))).contains("idx_1234_index_name");
      assertThat(DatabaseUtils.findExistingIndex(connection, tableName, "index")).isEmpty();
      assertThat(DatabaseUtils.findExistingIndex(connection, tableName, "index_name_2")).isEmpty();
      assertThat(DatabaseUtils.findExistingIndex(connection, tableName, "index_name_")).isEmpty();
    }
  }

  @Test
  public void find_column_with_lower_case_table_name_and_upper_case_column_name() throws SQLException {
    String tableName = "tablea";
    String columnName = "COLUMNA";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(DatabaseUtils.tableColumnExists(connection, tableName, columnName)).isTrue();
    }
  }

  @Test
  public void find_column_with_upper_case_table_name_and_upper_case_column_name() throws SQLException {
    String tableName = "TABLEA";
    String columnName = "COLUMNA";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(DatabaseUtils.tableColumnExists(connection, tableName, columnName)).isTrue();
    }
  }

  @Test
  public void find_column_with_lower_case_table_name_and_lower_case_column_name() throws SQLException {
    String tableName = "tablea";
    String columnName = "columna";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(DatabaseUtils.tableColumnExists(connection, tableName, columnName)).isTrue();
    }
  }

  @Test
  public void find_column_with_upper_case_table_name_and_lower_case_column_name() throws SQLException {
    String tableName = "TABLEA";
    String columnName = "columna";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(DatabaseUtils.tableColumnExists(connection, tableName, columnName)).isTrue();
    }
  }

  @Test
  public void should_close_connection() throws Exception {
    try (Connection connection = dbTester.openConnection()) {
      assertThat(isClosed(connection)).isFalse();

      DatabaseUtils.closeQuietly(connection);
      assertThat(isClosed(connection)).isTrue();
    }
  }

  @Test
  public void should_support_null_connection() {
    DatabaseUtils.closeQuietly((Connection) null);
    // no failure
  }

  @Test
  public void should_close_statement_and_resultset() throws Exception {
    try (Connection connection = dbTester.openConnection(); PreparedStatement statement = connection.prepareStatement(selectDual())) {
      ResultSet rs = statement.executeQuery();

      DatabaseUtils.closeQuietly(rs);
      DatabaseUtils.closeQuietly(statement);

      assertThat(isClosed(statement)).isTrue();
      assertThat(isClosed(rs)).isTrue();
    }
  }

  @Test
  public void should_not_fail_on_connection_errors() throws SQLException {
    Connection connection = mock(Connection.class);
    doThrow(new SQLException()).when(connection).close();

    DatabaseUtils.closeQuietly(connection);

    // no failure
    verify(connection).close(); // just to be sure
  }

  @Test
  public void should_not_fail_on_statement_errors() throws SQLException {
    Statement statement = mock(Statement.class);
    doThrow(new SQLException()).when(statement).close();

    DatabaseUtils.closeQuietly(statement);

    // no failure
    verify(statement).close(); // just to be sure
  }

  @Test
  public void should_not_fail_on_resulset_errors() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    doThrow(new SQLException()).when(rs).close();

    DatabaseUtils.closeQuietly(rs);

    // no failure
    verify(rs).close(); // just to be sure
  }

  @Test
  public void toUniqueAndSortedList_throws_NPE_if_arg_is_null() {
    assertThatThrownBy(() -> toUniqueAndSortedList(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void toUniqueAndSortedList_throws_NPE_if_arg_contains_a_null() {
    assertThatThrownBy(() -> toUniqueAndSortedList(List.of("A", null, "C")))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void toUniqueAndSortedList_throws_NPE_if_arg_is_a_set_containing_a_null() {
    assertThatThrownBy(() -> toUniqueAndSortedList(Set.of("A", null, "C")))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void toUniqueAndSortedList_enforces_natural_order() {
    assertThat(toUniqueAndSortedList(List.of("A", "B", "C"))).containsExactly("A", "B", "C");
    assertThat(toUniqueAndSortedList(List.of("B", "A", "C"))).containsExactly("A", "B", "C");
    assertThat(toUniqueAndSortedList(List.of("B", "C", "A"))).containsExactly("A", "B", "C");
  }

  @Test
  public void toUniqueAndSortedList_removes_duplicates() {
    assertThat(toUniqueAndSortedList(List.of("A", "A", "A"))).containsExactly("A");
    assertThat(toUniqueAndSortedList(List.of("A", "C", "A"))).containsExactly("A", "C");
    assertThat(toUniqueAndSortedList(List.of("C", "C", "B", "B", "A", "N", "C", "A"))).containsExactly("A", "B", "C", "N");
  }

  @Test
  public void toUniqueAndSortedList_removes_duplicates_and_apply_natural_order_of_any_Comparable() {
    assertThat(
      toUniqueAndSortedList(List.of(myComparable(2), myComparable(5), myComparable(2), myComparable(4), myComparable(-1), myComparable(10))))
      .containsExactly(
        myComparable(-1), myComparable(2), myComparable(4), myComparable(5), myComparable(10));
  }

  @Test
  public void can_not_determine_database_driver() throws SQLException {
    Connection connection = mock(Connection.class);
    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDriverName()).thenThrow(new SQLException());
    DatabaseUtils.getDriver(connection);
    assertThat(logTester.logs(LoggerLevel.WARN)).contains("Fail to determine database driver.");
  }

  @Test
  public void result_set_throw_exception() throws SQLException {
    String indexName = "idx";
    String schema = "TEST-SONAR";
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenThrow(new SQLException());
    assertThatThrownBy(() -> findExistingIndex(indexName, schema, resultSet, true))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Can not check that table test_table exists");
  }

  @Test
  public void find_existing_index_on_oracle_double_quoted_schema() throws SQLException {
    String indexName = "idx";
    String schema = "TEST-SONAR";
    ResultSet resultSet = newResultSet(true, indexName, schema);
    Optional<String> foundIndex = findExistingIndex(indexName, schema, resultSet, true);
    assertThat(foundIndex).hasValue(indexName);
  }

  @Test
  public void find_existing_index_on_oracle_standard_schema() throws SQLException {
    String indexName = "idx";
    String schema = DEFAULT_SCHEMA;
    ResultSet resultSet = newResultSet(true, indexName, schema);
    Optional<String> foundIndex = findExistingIndex(indexName, schema, resultSet, true);
    assertThat(foundIndex).hasValue(indexName);
  }

  @Test
  public void no_existing_index_on_oracle_double_quoted_schema() throws SQLException {
    String indexName = "idx";
    String schema = "TEST-SONAR";
    ResultSet resultSet = newResultSet(false, null, null);
    Optional<String> foundIndex = findExistingIndex(indexName, schema, resultSet, true);
    assertThat(foundIndex).isEmpty();
  }

  @Test
  public void no_matching_index_on_oracle_double_quoted_schema() throws SQLException {
    String indexName = "idx";
    String schema = "TEST-SONAR";
    ResultSet resultSet = newResultSet(true, "different", "different");
    Optional<String> foundIndex = findExistingIndex(indexName, schema, resultSet, true);
    assertThat(foundIndex).isEmpty();
  }

  @Test
  public void find_existing_index_on_default_schema() throws SQLException {
    String indexName = "idx";
    String schema = DEFAULT_SCHEMA;
    ResultSet resultSet = newResultSet(true, indexName, schema);
    Optional<String> foundIndex = findExistingIndex(indexName, schema, resultSet, false);
    assertThat(foundIndex).hasValue(indexName);
  }


  private Optional<String> findExistingIndex(String indexName, String schema, ResultSet resultSet, boolean isOracle) throws SQLException {
    Connection connection = mock(Connection.class);
    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    if (isOracle) {
      when(metaData.getDriverName()).thenReturn(ORACLE_DRIVER_NAME);
    }
    when(metaData.getIndexInfo(anyString(), eq(DEFAULT_SCHEMA.equals(schema) ? schema : null), anyString(), anyBoolean(), anyBoolean())).thenReturn(resultSet);
    when(connection.getMetaData()).thenReturn(metaData);
    when(connection.getSchema()).thenReturn(schema);
    when(connection.getCatalog()).thenReturn("catalog");

    return DatabaseUtils.findExistingIndex(connection, "test_table", indexName);
  }

  private ResultSet newResultSet(boolean hasNext, String indexName, String schema) throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(hasNext).thenReturn(false);
    when(resultSet.getString("INDEX_NAME")).thenReturn(indexName);
    when(resultSet.getString("TABLE_SCHEM")).thenReturn(schema);
    return resultSet;
  }


  private static DatabaseUtilsTest.MyComparable myComparable(int ordinal) {
    return new DatabaseUtilsTest.MyComparable(ordinal);
  }

  private static final class MyComparable implements Comparable<MyComparable> {
    private final int ordinal;

    private MyComparable(int ordinal) {
      this.ordinal = ordinal;
    }

    @Override
    public int compareTo(MyComparable o) {
      return ordinal - o.ordinal;
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      MyComparable that = (MyComparable) o;
      return ordinal == that.ordinal;
    }

    @Override
    public int hashCode() {
      return Objects.hash(ordinal);
    }
  }

  /**
   * Connection.isClosed() has been introduced in java 1.6
   */
  private boolean isClosed(Connection c) {
    try {
      c.createStatement().execute(selectDual());
      return false;
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * Statement.isClosed() has been introduced in java 1.6
   */
  private boolean isClosed(Statement s) {
    try {
      s.execute("SELECT 1");
      return false;
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * ResultSet.isClosed() has been introduced in java 1.6
   */
  private boolean isClosed(ResultSet rs) {
    try {
      rs.next();
      return false;
    } catch (Exception e) {
      return true;
    }
  }

  private String selectDual() {
    String sql = "SELECT 1";
    if (Oracle.ID.equals(dbTester.database().getDialect().getId())) {
      sql = "SELECT 1 FROM DUAL";
    }
    return sql;
  }

  @Test
  public void executeLargeInputs() {
    List<Integer> inputs = newArrayList();
    List<String> expectedOutputs = newArrayList();
    for (int i = 0; i < 2010; i++) {
      inputs.add(i);
      expectedOutputs.add(Integer.toString(i));
    }

    List<String> outputs = DatabaseUtils.executeLargeInputs(inputs, input -> {
      // Check that each partition is only done on 1000 elements max
      assertThat(input).hasSizeLessThanOrEqualTo(1000);
      return input.stream().map(String::valueOf).collect(MoreCollectors.toList());
    });

    assertThat(outputs).isEqualTo(expectedOutputs);
  }

  @Test
  public void executeLargeInputs_on_empty_list() {
    List<String> outputs = DatabaseUtils.executeLargeInputs(Collections.emptyList(), new Function<List<Integer>, List<String>>() {
      @Override
      public List<String> apply(List<Integer> input) {
        fail("No partition should be made on empty list");
        return Collections.emptyList();
      }
    });

    assertThat(outputs).isEmpty();
  }

  @Test
  public void executeLargeInputs_uses_specified_partition_size_manipulations() {
    List<List<Integer>> partitions = new ArrayList<>();
    List<Integer> outputs = DatabaseUtils.executeLargeInputs(
      List.of(1, 2, 3),
      partition -> {
        partitions.add(partition);
        return partition;
      },
      i -> i / 500);

    assertThat(outputs).containsExactly(1, 2, 3);
    assertThat(partitions).containsExactly(List.of(1, 2), List.of(3));
  }

  @Test
  public void executeLargeUpdates() {
    List<Integer> inputs = newArrayList();
    for (int i = 0; i < 2010; i++) {
      inputs.add(i);
    }

    List<Integer> processed = newArrayList();
    DatabaseUtils.executeLargeUpdates(inputs, input -> {
      assertThat(input).hasSizeLessThanOrEqualTo(1000);
      processed.addAll(input);
    });
    assertThat(processed).containsExactlyElementsOf(inputs);
  }

  @Test
  public void executeLargeUpdates_on_empty_list() {
    DatabaseUtils.executeLargeUpdates(Collections.<Integer>emptyList(), input -> fail("No partition should be made on empty list"));
  }

  @Test
  public void log_all_sql_exceptions() {
    SQLException root = new SQLException("this is root", "123");
    SQLException next = new SQLException("this is next", "456");
    root.setNextException(next);

    DatabaseUtils.log(Loggers.get(getClass()), root);

    assertThat(logTester.logs(LoggerLevel.ERROR)).contains("SQL error: 456. Message: this is next");
  }

  @Test
  public void tableExists_returns_true_if_table_is_referenced_in_db_metadata() throws Exception {
    try (Connection connection = dbTester.openConnection()) {
      assertThat(DatabaseUtils.tableExists("SCHEMA_MIGRATIONS", connection)).isTrue();
      assertThat(DatabaseUtils.tableExists("schema_migrations", connection)).isTrue();
      assertThat(DatabaseUtils.tableExists("schema_MIGRATIONS", connection)).isTrue();
      assertThat(DatabaseUtils.tableExists("foo", connection)).isFalse();
    }
  }

  @Test
  public void tableExists_is_resilient_on_getSchema() throws Exception {
    try (Connection connection = spy(dbTester.openConnection())) {
      doThrow(AbstractMethodError.class).when(connection).getSchema();
      assertThat(DatabaseUtils.tableExists("SCHEMA_MIGRATIONS", connection)).isTrue();
      assertThat(DatabaseUtils.tableExists("schema_migrations", connection)).isTrue();
      assertThat(DatabaseUtils.tableExists("schema_MIGRATIONS", connection)).isTrue();
      assertThat(DatabaseUtils.tableExists("foo", connection)).isFalse();
    }
  }

  @Test
  public void tableExists_is_using_getSchema_when_not_using_h2() throws Exception {
    try (Connection connection = spy(dbTester.openConnection())) {
      // DatabaseMetaData mock
      DatabaseMetaData metaData = mock(DatabaseMetaData.class);
      doReturn("xxx").when(metaData).getDriverName();

      // ResultSet mock
      ResultSet resultSet = mock(ResultSet.class);
      doReturn(true, false).when(resultSet).next();
      doReturn("SCHEMA_MIGRATIONS").when(resultSet).getString(eq("TABLE_NAME"));
      doReturn(resultSet).when(metaData).getTables(any(), eq("yyyy"), any(), any());

      // Connection mock
      doReturn("yyyy").when(connection).getSchema();
      doReturn(metaData).when(connection).getMetaData();

      assertThat(DatabaseUtils.tableExists("SCHEMA_MIGRATIONS", connection)).isTrue();
    }
  }

  @Test
  public void checkThatNotTooManyConditions_does_not_fail_if_less_than_1000_conditions() {
    DatabaseUtils.checkThatNotTooManyConditions(null, "unused");
    DatabaseUtils.checkThatNotTooManyConditions(Collections.emptySet(), "unused");
    DatabaseUtils.checkThatNotTooManyConditions(Collections.nCopies(10, "foo"), "unused");
    DatabaseUtils.checkThatNotTooManyConditions(Collections.nCopies(1_000, "foo"), "unused");
  }

  @Test
  public void checkThatNotTooManyConditions_throws_IAE_if_strictly_more_than_1000_conditions() {
    List<String> list = Collections.nCopies(1_001, "foo");
    assertThatThrownBy(() -> DatabaseUtils.checkThatNotTooManyConditions(list, "the message"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("the message");
  }
}
