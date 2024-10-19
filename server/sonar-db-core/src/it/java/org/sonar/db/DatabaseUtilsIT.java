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
import java.util.function.Function;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.db.dialect.Oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
import static org.sonar.db.DatabaseUtils.checkThatNotTooManyConditions;
import static org.sonar.db.DatabaseUtils.closeQuietly;
import static org.sonar.db.DatabaseUtils.getColumnMetadata;
import static org.sonar.db.DatabaseUtils.getDriver;
import static org.sonar.db.DatabaseUtils.log;
import static org.sonar.db.DatabaseUtils.tableColumnExists;
import static org.sonar.db.DatabaseUtils.tableExists;
import static org.sonar.db.DatabaseUtils.toUniqueAndSortedList;

public class DatabaseUtilsIT {

  private static final String DEFAULT_SCHEMA = "public";
  private static final String SCHEMA_MIGRATIONS_TABLE = "SCHEMA_MIGRATIONS";

  @Rule
  public CoreDbTester dbTester = CoreDbTester.createForSchema(DatabaseUtilsIT.class, "sql.sql", false);
  @Rule
  public LogTester logTester = new LogTester();

  @Test
  public void findExistingIndex_whenTableBothInLowerAndUpperCase_shouldFindIndex() throws SQLException {
    String indexName = "lower_case_name";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(DatabaseUtils.findExistingIndex(connection, SCHEMA_MIGRATIONS_TABLE, indexName)).contains(indexName);
      assertThat(DatabaseUtils.findExistingIndex(connection, SCHEMA_MIGRATIONS_TABLE.toLowerCase(Locale.US), indexName)).contains(indexName);
    }
  }

  @Test
  public void findExistingIndex_whenMixedCasesInTableAndIndexName_shouldFindIndex() throws SQLException {
    String indexName = "UPPER_CASE_NAME";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(DatabaseUtils.findExistingIndex(connection, SCHEMA_MIGRATIONS_TABLE, indexName)).contains(indexName);
      assertThat(DatabaseUtils.findExistingIndex(connection, SCHEMA_MIGRATIONS_TABLE, indexName.toLowerCase(Locale.US))).contains(indexName);
      assertThat(DatabaseUtils.findExistingIndex(connection, SCHEMA_MIGRATIONS_TABLE.toLowerCase(Locale.US), indexName.toLowerCase(Locale.US))).contains(indexName);
    }
  }

  @Test
  public void findExistingIndex_whenPassingOnlyPartOfIndexName_shouldFindIndexAndReturnFullName() throws SQLException {
    String indexName = "INDEX_NAME";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(DatabaseUtils.findExistingIndex(connection, SCHEMA_MIGRATIONS_TABLE, indexName)).contains("idx_1234_index_name");
      assertThat(DatabaseUtils.findExistingIndex(connection, SCHEMA_MIGRATIONS_TABLE.toLowerCase(Locale.US), indexName)).contains("idx_1234_index_name");
      assertThat(DatabaseUtils.findExistingIndex(connection, SCHEMA_MIGRATIONS_TABLE.toLowerCase(Locale.US), indexName.toLowerCase(Locale.US))).contains("idx_1234_index_name");
      assertThat(DatabaseUtils.findExistingIndex(connection, SCHEMA_MIGRATIONS_TABLE, "index")).isEmpty();
      assertThat(DatabaseUtils.findExistingIndex(connection, SCHEMA_MIGRATIONS_TABLE, "index_name_2")).isEmpty();
      assertThat(DatabaseUtils.findExistingIndex(connection, SCHEMA_MIGRATIONS_TABLE, "index_name_")).isEmpty();
    }
  }

  @Test
  public void tableColumnExists_whenTableNameLowerCaseColumnUpperCase_shouldFindColumn() throws SQLException {
    String tableName = "tablea";
    String columnName = "COLUMNA";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(tableColumnExists(connection, tableName, columnName)).isTrue();
    }
  }

  @Test
  public void tableColumnExists_whenArgumentInUpperCase_shouldFindColumn() throws SQLException {
    String tableName = "TABLEA";
    String columnName = "COLUMNA";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(tableColumnExists(connection, tableName, columnName)).isTrue();
    }
  }

  @Test
  public void tableColumnExists_whenArgumentsInLowerCase_shouldFindColumn() throws SQLException {
    String tableName = "tablea";
    String columnName = "columna";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(tableColumnExists(connection, tableName, columnName)).isTrue();
    }
  }

  @Test
  public void tableColumnExists_whenTableNameInUpperCaseAndColumnInLowerCase_shouldFindColumn() throws SQLException {
    String tableName = "TABLEA";
    String columnName = "columna";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(tableColumnExists(connection, tableName, columnName)).isTrue();
    }
  }

  @Test
  public void getColumnMetadata_whenTableNameLowerCaseColumnUpperCase_shouldFindColumn() throws SQLException {
    String tableName = "tablea";
    String columnName = "COLUMNA";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(getColumnMetadata(connection, tableName, columnName)).isNotNull();
    }
  }

  @Test
  public void getColumnMetadata_whenArgumentInUpperCase_shouldFindColumn() throws SQLException {
    String tableName = "TABLEA";
    String columnName = "COLUMNA";
    try (Connection connection = dbTester.openConnection()) {
      assertThat(getColumnMetadata(connection, tableName, columnName)).isNotNull();
    }
  }

  @Test
  public void closeQuietly_shouldCloseConnection() throws SQLException {
    try (Connection connection = dbTester.openConnection()) {
      assertThat(isClosed(connection)).isFalse();

      closeQuietly(connection);
      assertThat(isClosed(connection)).isTrue();
    }
  }

  @Test
  public void closeQuietly_shouldNotFailOnNullArgument() {
    assertThatCode(() -> closeQuietly((Connection) null)).doesNotThrowAnyException();
  }

  @Test
  public void closeQuietly_whenStatementAndResultSetOpen_shouldCloseBoth() throws SQLException {
    try (Connection connection = dbTester.openConnection(); PreparedStatement statement = connection.prepareStatement(selectDual())) {
      ResultSet rs = statement.executeQuery();

      closeQuietly(rs);
      closeQuietly(statement);

      assertThat(isClosed(statement)).isTrue();
      assertThat(isClosed(rs)).isTrue();
    }
  }

  @Test
  public void closeQuietly_whenConnectionThrowsException_shouldNotThrowException() throws SQLException {
    Connection connection = mock(Connection.class);
    doThrow(new SQLException()).when(connection).close();

    closeQuietly(connection);

    // no failure
    verify(connection).close(); // just to be sure
  }

  @Test
  public void closeQuietly_whenStatementThrowsException_shouldNotThrowException() throws SQLException {
    Statement statement = mock(Statement.class);
    doThrow(new SQLException()).when(statement).close();

    closeQuietly(statement);

    // no failure
    verify(statement).close(); // just to be sure
  }

  @Test
  public void closeQuietly_whenResultSetThrowsException_shouldNotThrowException() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    doThrow(new SQLException()).when(rs).close();

    closeQuietly(rs);

    // no failure
    verify(rs).close(); // just to be sure
  }

  @Test
  public void toUniqueAndSortedList_whenNullPassed_shouldThrowNullPointerException() {
    assertThatThrownBy(() -> toUniqueAndSortedList(null))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void toUniqueAndSortedList_whenNullPassedInsideTheList_shouldThrowNullPointerException() {
    assertThatThrownBy(() -> toUniqueAndSortedList(List.of("A", null, "C")))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void toUniqueAndSortedList_whenNullPassedInsideTheSet_shouldThrowNullPointerException() {
    assertThatThrownBy(() -> toUniqueAndSortedList(Set.of("A", null, "C")))
      .isInstanceOf(NullPointerException.class);
  }

  @Test
  public void toUniqueAndSortedList_shouldEnforceNaturalOrder() {
    assertThat(toUniqueAndSortedList(List.of("A", "B", "C"))).containsExactly("A", "B", "C");
    assertThat(toUniqueAndSortedList(List.of("B", "A", "C"))).containsExactly("A", "B", "C");
    assertThat(toUniqueAndSortedList(List.of("B", "C", "A"))).containsExactly("A", "B", "C");
  }

  @Test
  public void toUniqueAndSortedList_shouldRemoveDuplicates() {
    assertThat(toUniqueAndSortedList(List.of("A", "A", "A"))).containsExactly("A");
    assertThat(toUniqueAndSortedList(List.of("A", "C", "A"))).containsExactly("A", "C");
    assertThat(toUniqueAndSortedList(List.of("C", "C", "B", "B", "A", "N", "C", "A"))).containsExactly("A", "B", "C", "N");
  }

  @Test
  public void toUniqueAndSortedList_shouldRemoveDuplicatesAndEnforceNaturalOrder() {
    assertThat(
      toUniqueAndSortedList(List.of(myComparable(2), myComparable(5), myComparable(2), myComparable(4), myComparable(-1), myComparable(10))))
      .containsExactly(
        myComparable(-1), myComparable(2), myComparable(4), myComparable(5), myComparable(10));
  }

  @Test
  public void getDriver_whenIssuesWithDriver_shouldLeaveAMessageInTheLogs() throws SQLException {
    Connection connection = mock(Connection.class);
    DatabaseMetaData metaData = mock(DatabaseMetaData.class);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDriverName()).thenThrow(new SQLException());

    getDriver(connection);

    assertThat(logTester.logs(Level.WARN)).contains("Fail to determine database driver.");
  }

  @Test
  public void findExistingIndex_whenResultSetThrowsException_shouldThrowExceptionToo() throws SQLException {
    String indexName = "idx";
    String schema = "TEST-SONAR";
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenThrow(new SQLException());

    assertThatThrownBy(() -> findExistingIndex(indexName, schema, resultSet, true))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Can not check that table test_table exists");
  }

  @Test
  public void findExistingIndex_whenExistingIndexOnOracleDoubleQuotedSchema_shouldReturnIndex() throws SQLException {
    String indexName = "idx";
    String schema = "TEST-SONAR";
    ResultSet resultSet = newResultSet(true, indexName, schema);

    Optional<String> foundIndex = findExistingIndex(indexName, schema, resultSet, true);

    assertThat(foundIndex).hasValue(indexName);
  }

  @Test
  public void findExistingIndex_whenExistingIndexOnDefaultSchema_shouldReturnIndex() throws SQLException {
    String indexName = "idx";
    String schema = DEFAULT_SCHEMA;
    ResultSet resultSet = newResultSet(true, indexName, schema);

    Optional<String> foundIndex = findExistingIndex(indexName, schema, resultSet, true);

    assertThat(foundIndex).hasValue(indexName);
  }

  @Test
  public void findExistingIndex_whenNoExistingIndexOnOracleDoubleQuotedSchema_shouldNotReturnIndex() throws SQLException {
    String indexName = "idx";
    String schema = "TEST-SONAR";
    ResultSet resultSet = newResultSet(false, null, null);

    Optional<String> foundIndex = findExistingIndex(indexName, schema, resultSet, true);

    assertThat(foundIndex).isEmpty();
  }

  @Test
  public void findExistingIndex_whenNoMatchingIndexOnOracleDoubleQuotedSchema_shouldNotReturnIndex() throws SQLException {
    String indexName = "idx";
    String schema = "TEST-SONAR";
    ResultSet resultSet = newResultSet(true, "different", "different");

    Optional<String> foundIndex = findExistingIndex(indexName, schema, resultSet, true);

    assertThat(foundIndex).isEmpty();
  }

  @Test
  public void findExistingIndex_whenExistingIndexAndSchemaPassed_shouldFindIndex() throws SQLException {
    String indexName = "idx";
    String schema = DEFAULT_SCHEMA;
    ResultSet resultSet = newResultSet(true, indexName, schema);

    Optional<String> foundIndex = findExistingIndex(indexName, schema, resultSet, false);

    assertThat(foundIndex).hasValue(indexName);
  }

  @Test
  public void executeLargeInputs_whenALotOfElementsPassed_shouldProcessAllItems() {
    List<Integer> inputs = new ArrayList<>();
    List<String> expectedOutputs = new ArrayList<>();
    for (int i = 0; i < 2010; i++) {
      inputs.add(i);
      expectedOutputs.add(Integer.toString(i));
    }

    List<String> outputs = DatabaseUtils.executeLargeInputs(inputs, input -> {
      // Check that each partition is only done on 1000 elements max
      assertThat(input).hasSizeLessThanOrEqualTo(1000);
      return input.stream().map(String::valueOf).toList();
    });

    assertThat(outputs).isEqualTo(expectedOutputs);
  }

  @Test
  public void executeLargeInputsWithFunctionAsInput_whenEmptyList_shouldReturnEmpty() {
    List<String> outputs = DatabaseUtils.executeLargeInputs(Collections.emptyList(), (Function<List<Integer>, List<String>>) input -> {
      fail("No partition should be made on empty list");
      return Collections.emptyList();
    });

    assertThat(outputs).isEmpty();
  }

  @Test
  public void executeLargeUpdates_whenEmptyList_shouldFail() {
    DatabaseUtils.executeLargeUpdates(Collections.<Integer>emptyList(), input -> fail("No partition should be made on empty list"));
  }

  @Test
  public void executeLargeInputs_whenPartitionSizeIsCustom_shouldParitionAccordingly() {
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
  public void executeLargeUpdates_whenALotOfElementsPassed_shouldProcessAllItems() {
    List<Integer> inputs = new ArrayList<>();
    for (int i = 0; i < 2010; i++) {
      inputs.add(i);
    }

    List<Integer> processed = new ArrayList<>();
    DatabaseUtils.executeLargeUpdates(inputs, input -> {
      assertThat(input).hasSizeLessThanOrEqualTo(1000);
      processed.addAll(input);
    });
    assertThat(processed).containsExactlyElementsOf(inputs);
  }

  @Test
  public void logging_whenSomeExceptionThrown_shouldContainThemInTheLog() {
    SQLException root = new SQLException("this is root", "123");
    SQLException next = new SQLException("this is next", "456");
    root.setNextException(next);

    log(LoggerFactory.getLogger(getClass()), root);

    assertThat(logTester.logs(Level.ERROR)).contains("SQL error: 456. Message: this is next");
  }

  @Test
  public void tableExists_whenTableInTheMetadata_shouldReturnTrue() throws Exception {
    try (Connection connection = dbTester.openConnection()) {
      assertThat(tableExists("SCHEMA_MIGRATIONS", connection)).isTrue();
      assertThat(tableExists("schema_migrations", connection)).isTrue();
      assertThat(tableExists("schema_MIGRATIONS", connection)).isTrue();
      assertThat(tableExists("foo", connection)).isFalse();
    }
  }

  @Test
  public void tableExists_whenGetSchemaThrowException_shouldNotFail() throws Exception {
    try (Connection connection = spy(dbTester.openConnection())) {
      doThrow(AbstractMethodError.class).when(connection).getSchema();
      assertThat(tableExists("SCHEMA_MIGRATIONS", connection)).isTrue();
      assertThat(tableExists("schema_migrations", connection)).isTrue();
      assertThat(tableExists("schema_MIGRATIONS", connection)).isTrue();
      assertThat(tableExists("foo", connection)).isFalse();
    }
  }

  @Test//is_using_getSchema_when_not_using_h2
  public void tableExists_whenNotUsingH2_shouldReturnTrue() throws Exception {
    try (Connection connection = spy(dbTester.openConnection())) {
      // DatabaseMetaData mock
      DatabaseMetaData metaData = mock(DatabaseMetaData.class);
      doReturn("xxx").when(metaData).getDriverName();

      // ResultSet mock
      ResultSet resultSet = mock(ResultSet.class);
      doReturn(true, false).when(resultSet).next();
      doReturn(SCHEMA_MIGRATIONS_TABLE).when(resultSet).getString("TABLE_NAME");
      doReturn(resultSet).when(metaData).getTables(any(), eq("yyyy"), any(), any());

      // Connection mock
      doReturn("yyyy").when(connection).getSchema();
      doReturn(metaData).when(connection).getMetaData();

      assertThat(tableExists(SCHEMA_MIGRATIONS_TABLE, connection)).isTrue();
    }
  }

  @Test
  public void checkThatNotTooManyConditions_whenLessThan1000Items_shouldNotThrowException() {
    checkThatNotTooManyConditions(null, "unused");
    checkThatNotTooManyConditions(Collections.emptySet(), "unused");
    checkThatNotTooManyConditions(Collections.nCopies(10, "foo"), "unused");
    checkThatNotTooManyConditions(Collections.nCopies(1_000, "foo"), "unused");
  }

  @Test
  public void checkThatNotTooManyConditions_whenMoreThan1000ItemsInTheList_shouldNotThrowException() {
    List<String> list = Collections.nCopies(1_001, "foo");
    assertThatThrownBy(() -> checkThatNotTooManyConditions(list, "the message"))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("the message");
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


  private static DatabaseUtilsIT.MyComparable myComparable(int ordinal) {
    return new DatabaseUtilsIT.MyComparable(ordinal);
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

}
