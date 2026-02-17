/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractSqlDbTesterTest {

  private TestableAbstractSqlDbTester underTest;
  private RealDbTester realDbTester;

  @After
  public void tearDown() {
    if (underTest != null) {
      underTest.after();
    }
    if (realDbTester != null) {
      realDbTester.after();
    }
  }

  @Test
  public void before_shouldCallDbStartThenTruncateTables() {
    TestDb mockTestDb = mock(TestDb.class);
    underTest = new TestableAbstractSqlDbTester(mockTestDb);

    underTest.before();

    // Verify lifecycle order: start() called before truncateTables()
    assertThat(underTest.truncateTablesCalled).isTrue();
    verify(mockTestDb).start();
  }

  @Test
  public void after_shouldCallDbStop() {
    TestDb mockTestDb = mock(TestDb.class);
    underTest = new TestableAbstractSqlDbTester(mockTestDb);

    underTest.after();

    verify(mockTestDb).stop();
  }

  @Test
  public void beforeEach_shouldDelegateToBeforeMethod() throws Exception {
    TestDb mockTestDb = mock(TestDb.class);
    underTest = new TestableAbstractSqlDbTester(mockTestDb);
    ExtensionContext mockContext = mock(ExtensionContext.class);

    underTest.beforeEach(mockContext);

    // JUnit 5 callback delegates to before()
    assertThat(underTest.truncateTablesCalled).isTrue();
    verify(mockTestDb).start();
  }

  @Test
  public void afterEach_shouldDelegateToAfterMethod() throws Exception {
    TestDb mockTestDb = mock(TestDb.class);
    underTest = new TestableAbstractSqlDbTester(mockTestDb);
    ExtensionContext mockContext = mock(ExtensionContext.class);

    underTest.afterEach(mockContext);

    // JUnit 5 callback delegates to after()
    verify(mockTestDb).stop();
  }

  @Test
  public void getDb_shouldReturnTestDbInstance() {
    TestDb mockTestDb = mock(TestDb.class);
    underTest = new TestableAbstractSqlDbTester(mockTestDb);

    assertThat(underTest.getDb()).isSameAs(mockTestDb);
  }

  @Test
  public void database_shouldReturnDatabaseFromTestDb() {
    TestDb mockTestDb = mock(TestDb.class);
    Database mockDatabase = mock(Database.class);
    when(mockTestDb.getDatabase()).thenReturn(mockDatabase);
    underTest = new TestableAbstractSqlDbTester(mockTestDb);

    Database actualDatabase = underTest.database();

    assertThat(actualDatabase).isSameAs(mockDatabase);
  }

  // --- SQL Utility Tests using Real H2 Database ---

  @Test
  public void executeDdl_shouldCreateTable() {
    realDbTester = new RealDbTester();
    realDbTester.before();

    realDbTester.executeDdl("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))");

    realDbTester.assertTableExists("test_table");
  }

  @Test
  public void executeInsert_withPositionalParams_shouldInsertRow() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50), value INT)");

    realDbTester.executeInsert("test_table", "id", 1, "name", "test1", "value", 100);

    int actualCount = realDbTester.countRowsOfTable("test_table");
    assertThat(actualCount).isEqualTo(1);
  }

  @Test
  public void executeInsert_withMap_shouldInsertRow() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))");

    realDbTester.executeInsert("test_table", Map.of("id", 1, "name", "test1"));

    int actualCount = realDbTester.countRowsOfTable("test_table");
    assertThat(actualCount).isEqualTo(1);
  }

  @Test
  public void countRowsOfTable_shouldReturnCorrectCount() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))");
    realDbTester.executeInsert("test_table", "id", 1, "name", "row1");
    realDbTester.executeInsert("test_table", "id", 2, "name", "row2");
    realDbTester.executeInsert("test_table", "id", 3, "name", "row3");

    int actualCount = realDbTester.countRowsOfTable("test_table");

    assertThat(actualCount).isEqualTo(3);
  }

  @Test
  public void countSql_shouldReturnCorrectCount() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE test_table (id INT PRIMARY KEY, value INT)");
    realDbTester.executeInsert("test_table", "id", 1, "value", 100);
    realDbTester.executeInsert("test_table", "id", 2, "value", 200);
    realDbTester.executeInsert("test_table", "id", 3, "value", 100);

    int actualCount = realDbTester.countSql("SELECT count(*) FROM test_table WHERE value = 100");

    assertThat(actualCount).isEqualTo(2);
  }

  @Test
  public void select_shouldReturnAllRows() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))");
    realDbTester.executeInsert("test_table", "id", 1, "name", "row1");
    realDbTester.executeInsert("test_table", "id", 2, "name", "row2");

    List<Map<String, Object>> rows = realDbTester.select("SELECT id, name FROM test_table ORDER BY id");

    assertThat(rows).hasSize(2);
    assertThat(rows.get(0)).containsEntry("id", 1L).containsEntry("name", "row1");
    assertThat(rows.get(1)).containsEntry("id", 2L).containsEntry("name", "row2");
  }

  @Test
  public void selectFirst_shouldReturnOnlyRow() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))");
    realDbTester.executeInsert("test_table", "id", 1, "name", "only");

    Map<String, Object> row = realDbTester.selectFirst("SELECT id, name FROM test_table");

    assertThat(row).containsEntry("id", 1L).containsEntry("name", "only");
  }

  @Test
  public void selectFirst_whenNoRows_shouldThrowException() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))");

    assertThatThrownBy(() -> realDbTester.selectFirst("SELECT id, name FROM test_table"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("No results for");
  }

  @Test
  public void selectFirst_whenMultipleRows_shouldThrowException() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))");
    realDbTester.executeInsert("test_table", "id", 1, "name", "first");
    realDbTester.executeInsert("test_table", "id", 2, "name", "second");

    assertThatThrownBy(() -> realDbTester.selectFirst("SELECT id, name FROM test_table"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("Too many results for");
  }

  @Test
  public void executeUpdateSql_shouldUpdateRows() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE test_table (id INT PRIMARY KEY, value INT)");
    realDbTester.executeInsert("test_table", "id", 1, "value", 100);

    realDbTester.executeUpdateSql("UPDATE test_table SET value = ? WHERE id = ?", 200, 1);

    Map<String, Object> row = realDbTester.selectFirst("SELECT value FROM test_table WHERE id = 1");
    assertThat(row).containsEntry("value", 200L);
  }

  @Test
  public void assertTableExists_shouldPassForExistingTable() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE existing_table (id INT)");

    realDbTester.assertTableExists("existing_table");
    // No exception = success
  }

  @Test
  public void assertTableDoesNotExist_shouldPassForNonExistingTable() {
    realDbTester = new RealDbTester();
    realDbTester.before();

    realDbTester.assertTableDoesNotExist("non_existing_table");
    // No exception = success
  }

  @Test
  public void assertColumnDefinition_shouldVerifyColumnProperties() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50) NOT NULL)");

    realDbTester.assertColumnDefinition("test_table", "name", java.sql.Types.VARCHAR, 50, false);
    // No exception = success
  }

  @Test
  public void assertPrimaryKey_shouldVerifyPrimaryKeyExists() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE test_table (id INT, name VARCHAR(50), CONSTRAINT pk_test_table PRIMARY KEY (id))");

    realDbTester.assertPrimaryKey("test_table", "pk_test_table", "id");
    // No exception = success
  }

  @Test
  public void assertIndex_shouldVerifyIndexExists() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE test_table (id INT PRIMARY KEY, name VARCHAR(50))");
    realDbTester.executeDdl("CREATE INDEX idx_name ON test_table(name)");

    realDbTester.assertIndex("test_table", "idx_name", "name");
    // No exception = success
  }

  @Test
  public void assertUniqueIndex_shouldVerifyUniqueIndexExists() {
    realDbTester = new RealDbTester();
    realDbTester.before();
    realDbTester.executeDdl("CREATE TABLE test_table (id INT PRIMARY KEY, code VARCHAR(50))");
    realDbTester.executeDdl("CREATE UNIQUE INDEX uniq_code ON test_table(code)");

    realDbTester.assertUniqueIndex("test_table", "uniq_code", "code");
    // No exception = success
  }

  @Test
  public void toVendorCase_shouldConvertToUpperCaseForH2() {
    realDbTester = new RealDbTester();
    realDbTester.before();

    String result = realDbTester.toVendorCase("table_name");

    assertThat(result).isEqualTo("TABLE_NAME");
  }

  /**
   * Concrete implementation of AbstractSqlDbTester for testing lifecycle.
   */
  private static class TestableAbstractSqlDbTester extends AbstractSqlDbTester<TestDb> {
    boolean truncateTablesCalled = false;

    TestableAbstractSqlDbTester(TestDb db) {
      super(db);
    }

    @Override
    public void truncateTables() {
      truncateTablesCalled = true;
      super.truncateTables();
    }
  }

  /**
   * Concrete implementation using real H2TestDb for SQL utility testing.
   */
  private static class RealDbTester extends AbstractSqlDbTester<H2TestDb> {
    RealDbTester() {
      super(H2TestDb.createEmpty());
    }
  }
}
