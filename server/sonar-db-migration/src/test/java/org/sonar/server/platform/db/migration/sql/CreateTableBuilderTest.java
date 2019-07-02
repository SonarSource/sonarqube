/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.sql;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;
import org.sonar.server.platform.db.migration.def.ColumnDef;
import org.sonar.server.platform.db.migration.def.TinyIntColumnDef;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BlobColumnDef.newBlobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;
import static org.sonar.server.platform.db.migration.sql.CreateTableBuilder.ColumnFlag.AUTO_INCREMENT;

@RunWith(DataProviderRunner.class)
public class CreateTableBuilderTest {
  private static final H2 H2 = new H2();
  private static final Oracle ORACLE = new Oracle();
  private static final PostgreSql POSTGRESQL = new PostgreSql();
  private static final MsSql MS_SQL = new MsSql();
  private static final Dialect[] ALL_DIALECTS = {H2, MS_SQL, POSTGRESQL, ORACLE};
  private static final String TABLE_NAME = "table_42";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CreateTableBuilder underTest = new CreateTableBuilder(mock(Dialect.class), TABLE_NAME);

  @Test
  public void constructor_fails_with_NPE_if_dialect_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("dialect can't be null");

    new CreateTableBuilder(null, TABLE_NAME);
  }

  @Test
  public void constructor_fails_with_NPE_if_tablename_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Table name cannot be null");

    new CreateTableBuilder(mock(Dialect.class), null);
  }

  @Test
  public void constructor_throws_IAE_if_table_name_is_not_lowercase() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Table name must be lower case and contain only alphanumeric chars or '_', got 'Tooo");

    new CreateTableBuilder(mock(Dialect.class), "Tooo");
  }

  @Test
  public void constructor_throws_IAE_if_table_name_is_26_chars_long() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Table name length can't be more than 25");

    new CreateTableBuilder(mock(Dialect.class), "abcdefghijklmnopqrstuvwxyz");
  }

  @Test
  public void constructor_does_not_fail_if_table_name_is_25_chars_long() {
    new CreateTableBuilder(mock(Dialect.class), "abcdefghijklmnopqrstuvwxy");
  }

  @Test
  public void constructor_does_not_fail_if_table_name_contains_ascii_letters() {
    new CreateTableBuilder(mock(Dialect.class), "abcdefghijklmnopqrstuvwxy");
    new CreateTableBuilder(mock(Dialect.class), "z");
  }

  @Test
  public void constructor_throws_IAE_if_table_name_starts_with_underscore() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Table name must not start by a number or '_', got '_a'");

    new CreateTableBuilder(mock(Dialect.class), "_a");
  }

  @Test
  @UseDataProvider("digitCharsDataProvider")
  public void constructor_throws_IAE_if_table_name_starts_with_number(char number) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Table name must not start by a number or '_', got '" + number + "a'");

    new CreateTableBuilder(mock(Dialect.class), number + "a");
  }

  @DataProvider
  public static Object[][] digitCharsDataProvider() {
    return new Object[][]{
      {'0'},
      {'1'},
      {'2'},
      {'3'},
      {'4'},
      {'5'},
      {'6'},
      {'7'},
      {'8'},
      {'9'},
    };
  }

  @Test
  public void constructor_does_not_fail_if_table_name_contains_underscore_or_numbers() {
    new CreateTableBuilder(mock(Dialect.class), "a1234567890");
    new CreateTableBuilder(mock(Dialect.class), "a_");
  }

  @Test
  public void build_throws_ISE_if_no_column_has_been_set() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("at least one column must be specified");

    underTest.build();
  }

  @Test
  public void addColumn_throws_NPE_if_ColumnDef_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("column def can't be null");

    underTest.addColumn(null);
  }

  @Test
  public void addPkColumn_throws_NPE_if_ColumnDef_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("column def can't be null");

    underTest.addPkColumn(null);
  }

  @Test
  public void addPkColumn_throws_IAE_when_AUTO_INCREMENT_flag_is_provided_with_column_name_other_than_id() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Auto increment column name must be id");

    underTest.addPkColumn(newIntegerColumnDefBuilder().setColumnName("toto").build(), AUTO_INCREMENT);
  }

  @Test
  public void addPkColumn_throws_ISE_when_adding_multiple_autoincrement_columns() {
    underTest.addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("There can't be more than one auto increment column");

    underTest.addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT);
  }

  @Test
  public void addPkColumn_throws_IAE_when_AUTO_INCREMENT_flag_is_provided_with_def_other_than_Integer_and_BigInteger() {
    ColumnDef[] columnDefs = {
      newBooleanColumnDefBuilder().setColumnName("id").build(),
      newClobColumnDefBuilder().setColumnName("id").build(),
      newDecimalColumnDefBuilder().setColumnName("id").build(),
      new TinyIntColumnDef.Builder().setColumnName("id").build(),
      newVarcharColumnDefBuilder().setColumnName("id").setLimit(40).build(),
      newBlobColumnDefBuilder().setColumnName("id").build()
    };
    Arrays.stream(columnDefs)
      .forEach(columnDef -> {
        try {
          underTest.addPkColumn(columnDef, AUTO_INCREMENT);
          fail("A IllegalArgumentException should have been raised");
        } catch (IllegalArgumentException e) {
          assertThat(e).hasMessage("Auto increment column must either be BigInteger or Integer");
        }
      });
  }

  @Test
  public void addPkColumn_throws_IAE_when_AUTO_INCREMENT_flag_is_provided_and_column_is_nullable() {
    ColumnDef[] columnDefs = {
      newIntegerColumnDefBuilder().setColumnName("id").build(),
      newBigIntegerColumnDefBuilder().setColumnName("id").build()
    };
    Arrays.stream(columnDefs)
      .forEach(columnDef -> {
        try {
          underTest.addPkColumn(columnDef, AUTO_INCREMENT);
          fail("A IllegalArgumentException should have been raised");
        } catch (IllegalArgumentException e) {
          assertThat(e).hasMessage("Auto increment column can't be nullable");
        }
      });
  }

  @Test
  public void build_sets_type_SERIAL_for_autoincrement_integer_pk_column_on_Postgresql() {
    List<String> stmts = new CreateTableBuilder(POSTGRESQL, TABLE_NAME)
      .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
      .build();
    assertThat(stmts).hasSize(1);
    assertThat(stmts.iterator().next())
      .isEqualTo(
        "CREATE TABLE table_42 (id SERIAL NOT NULL, CONSTRAINT pk_table_42 PRIMARY KEY (id))");
  }

  @Test
  public void build_sets_type_BIGSERIAL_for_autoincrement_biginteger_pk_column_on_Postgresql() {
    List<String> stmts = new CreateTableBuilder(POSTGRESQL, TABLE_NAME)
      .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
      .build();
    assertThat(stmts).hasSize(1);
    assertThat(stmts.iterator().next())
      .isEqualTo(
        "CREATE TABLE table_42 (id BIGSERIAL NOT NULL, CONSTRAINT pk_table_42 PRIMARY KEY (id))");
  }

  @Test
  public void build_generates_a_create_trigger_statement_when_an_autoincrement_pk_column_is_specified_and_on_Oracle() {
    List<String> stmts = new CreateTableBuilder(ORACLE, TABLE_NAME)
      .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
      .build();
    assertThat(stmts).hasSize(3);
    assertThat(stmts.get(0))
      .isEqualTo("CREATE TABLE table_42 (id NUMBER(38,0) NOT NULL, CONSTRAINT pk_table_42 PRIMARY KEY (id))");
    assertThat(stmts.get(1))
      .isEqualTo("CREATE SEQUENCE table_42_seq START WITH 1 INCREMENT BY 1");
    assertThat(stmts.get(2))
      .isEqualTo("CREATE OR REPLACE TRIGGER table_42_idt" +
        " BEFORE INSERT ON table_42" +
        " FOR EACH ROW" +
        " BEGIN" +
        " IF :new.id IS null THEN" +
        " SELECT table_42_seq.nextval INTO :new.id FROM dual;" +
        " END IF;" +
        " END;");
  }

  @Test
  public void build_adds_IDENTITY_clause_on_MsSql() {
    List<String> stmts = new CreateTableBuilder(MS_SQL, TABLE_NAME)
      .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
      .build();
    assertThat(stmts).hasSize(1);
    assertThat(stmts.iterator().next())
      .isEqualTo(
        "CREATE TABLE table_42 (id INT NOT NULL IDENTITY (1,1), CONSTRAINT pk_table_42 PRIMARY KEY (id))");
  }

  @Test
  public void build_adds_AUTO_INCREMENT_clause_on_H2() {
    List<String> stmts = new CreateTableBuilder(H2, TABLE_NAME)
      .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
      .build();
    assertThat(stmts).hasSize(1);
    assertThat(stmts.iterator().next())
      .isEqualTo(
        "CREATE TABLE table_42 (id INTEGER NOT NULL AUTO_INCREMENT (1,1), CONSTRAINT pk_table_42 PRIMARY KEY (id))");
  }

  @Test
  public void withPkConstraintName_throws_NPE_if_name_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("Constraint name cannot be null");

    underTest.withPkConstraintName(null);
  }

  @Test
  public void withPkConstraintName_throws_IAE_if_name_is_not_lowercase() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Constraint name must be lower case and contain only alphanumeric chars or '_', got 'Too'");

    underTest.withPkConstraintName("Too");
  }

  @Test
  public void withPkConstraintName_throws_IAE_if_name_is_more_than_30_char_long() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Constraint name length can't be more than 30");

    underTest.withPkConstraintName("abcdefghijklmnopqrstuvwxyzabcdf");
  }

  @Test
  public void withPkConstraintName_throws_IAE_if_name_starts_with_underscore() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Constraint name must not start by a number or '_', got '_a'");

    underTest.withPkConstraintName("_a");
  }

  @Test
  @UseDataProvider("digitCharsDataProvider")
  public void withPkConstraintName_throws_IAE_if_name_starts_with_number(char number) {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Constraint name must not start by a number or '_', got '" + number + "a'");

    underTest.withPkConstraintName(number + "a");
  }

  @Test
  public void withPkConstraintName_does_not_fail_if_name_is_30_char_long() {
    underTest.withPkConstraintName("abcdefghijklmnopqrstuvwxyzabcd");
  }

  @Test
  public void withPkConstraintName_does_not_fail_if_name_contains_ascii_letters() {
    underTest.withPkConstraintName("abcdefghijklmnopqrstuvwxyz");
  }

  @Test
  public void withPkConstraintName_does_not_fail_if_name_contains_underscore() {
    underTest.withPkConstraintName("a_");
  }

  @Test
  public void withPkConstraintName_does_not_fail_if_name_contains_numbers() {
    underTest.withPkConstraintName("a0123456789");
  }

  @Test
  public void build_adds_NULL_when_column_is_nullable_for_all_DBs() {
    Arrays.stream(ALL_DIALECTS)
      .forEach(dialect -> {
        List<String> stmts = new CreateTableBuilder(dialect, TABLE_NAME)
          .addColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col").build())
          .build();
        assertThat(stmts).hasSize(1);

        assertThat(stmts.iterator().next())
          .startsWith("CREATE TABLE " + TABLE_NAME + " (" +
            "bg_col " +
            bigIntSqlType(dialect) + " NULL" +
            ")");
      });
  }

  @Test
  public void build_adds_NOT_NULL_when_column_is_not_nullable_for_all_DBs() {
    Arrays.stream(ALL_DIALECTS)
      .forEach(dialect -> {
        List<String> stmts = new CreateTableBuilder(dialect, TABLE_NAME)
          .addColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col").setIsNullable(false).build())
          .build();
        assertThat(stmts).hasSize(1);

        assertThat(stmts.iterator().next())
          .startsWith("CREATE TABLE " + TABLE_NAME + " (" +
            "bg_col " +
            bigIntSqlType(dialect) +
            " NOT NULL" +
            ")");
      });
  }

  @Test
  public void build_of_single_column_table() {
    List<String> stmts = new CreateTableBuilder(H2, TABLE_NAME)
      .addColumn(newBooleanColumnDefBuilder().setColumnName("bool_col_1").build())
      .build();
    assertThat(stmts).hasSize(1);

    assertThat(stmts.iterator().next()).isEqualTo("CREATE TABLE table_42 (bool_col_1 BOOLEAN NULL)");
  }

  @Test
  public void build_table_with_pk() {
    List<String> stmts = new CreateTableBuilder(H2, TABLE_NAME)
      .addPkColumn(newBooleanColumnDefBuilder().setColumnName("bool_col").build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("varchar_col").setLimit(40).build())
      .build();
    assertThat(stmts).hasSize(1);

    assertThat(stmts.iterator().next())
      .isEqualTo("CREATE TABLE " + TABLE_NAME + " (" +
        "bool_col BOOLEAN NULL," +
        "varchar_col VARCHAR (40) NULL," +
        " CONSTRAINT pk_" + TABLE_NAME + " PRIMARY KEY (bool_col)" +
        ")");

  }

  @Test
  public void build_adds_PRIMARY_KEY_constraint_on_single_column_with_name_computed_from_tablename() {
    Arrays.asList(ALL_DIALECTS)
      .forEach(dialect -> {
        List<String> stmts = new CreateTableBuilder(dialect, TABLE_NAME)
          .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col").setIsNullable(false).build())
          .build();
        assertThat(stmts).hasSize(1);

        assertThat(stmts.iterator().next())
          .startsWith("CREATE TABLE " + TABLE_NAME + " (" +
            "bg_col " + bigIntSqlType(dialect) + " NOT NULL," +
            " CONSTRAINT pk_" + TABLE_NAME + " PRIMARY KEY (bg_col)" +
            ")");
      });
  }

  @Test
  public void build_adds_PRIMARY_KEY_constraint_on_single_column_with_lower_case_of_specified_name() {
    Arrays.asList(ALL_DIALECTS)
      .forEach(dialect -> {
        List<String> stmts = new CreateTableBuilder(dialect, TABLE_NAME)
          .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col").setIsNullable(false).build())
          .withPkConstraintName("my_pk")
          .build();
        assertThat(stmts).hasSize(1);

        assertThat(stmts.iterator().next())
          .startsWith("CREATE TABLE " + TABLE_NAME + " (" +
            "bg_col " +
            bigIntSqlType(dialect) +
            " NOT NULL," +
            " CONSTRAINT my_pk PRIMARY KEY (bg_col)" +
            ")");
      });
  }

  @Test
  public void build_adds_PRIMARY_KEY_constraint_on_multiple_columns_with_name_computed_from_tablename() {
    Arrays.asList(ALL_DIALECTS)
      .forEach(dialect -> {
        List<String> stmts = new CreateTableBuilder(dialect, TABLE_NAME)
          .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col_1").setIsNullable(false).build())
          .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col_2").setIsNullable(false).build())
          .build();
        assertThat(stmts).hasSize(1);

        assertThat(stmts.iterator().next())
          .startsWith("CREATE TABLE " + TABLE_NAME + " (" +
            "bg_col_1 " + bigIntSqlType(dialect) + " NOT NULL," +
            "bg_col_2 " + bigIntSqlType(dialect) + " NOT NULL," +
            " CONSTRAINT pk_" + TABLE_NAME + " PRIMARY KEY (bg_col_1,bg_col_2)" +
            ")");
      });
  }

  @Test
  public void build_adds_PRIMARY_KEY_constraint_on_multiple_columns_with_lower_case_of_specified_name() {
    Arrays.asList(ALL_DIALECTS)
      .forEach(dialect -> {
        List<String> stmts = new CreateTableBuilder(dialect, TABLE_NAME)
          .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col_1").setIsNullable(false).build())
          .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("bg_col_2").setIsNullable(false).build())
          .withPkConstraintName("my_pk")
          .build();
        assertThat(stmts).hasSize(1);

        assertThat(stmts.iterator().next())
          .startsWith("CREATE TABLE " + TABLE_NAME + " (" +
            "bg_col_1 " + bigIntSqlType(dialect) + " NOT NULL," +
            "bg_col_2 " + bigIntSqlType(dialect) + " NOT NULL," +
            " CONSTRAINT my_pk PRIMARY KEY (bg_col_1,bg_col_2)" +
            ")");
      });
  }

  @Test
  public void build_adds_DEFAULT_clause_on_varchar_column_on_H2() {
    verifyDefaultClauseOnVarcharColumn(H2, "CREATE TABLE table_42 (status VARCHAR (1) DEFAULT 'P' NOT NULL)");
  }

  @Test
  public void build_adds_DEFAULT_clause_on_varchar_column_on_MSSQL() {
    verifyDefaultClauseOnVarcharColumn(MS_SQL, "CREATE TABLE table_42 (status NVARCHAR (1) DEFAULT 'P' NOT NULL)");
  }

  @Test
  public void build_adds_DEFAULT_clause_on_varchar_column_on_Oracle() {
    verifyDefaultClauseOnVarcharColumn(ORACLE, "CREATE TABLE table_42 (status VARCHAR2 (1 CHAR) DEFAULT 'P' NOT NULL)");
  }

  @Test
  public void build_adds_DEFAULT_clause_on_varchar_column_on_PostgreSQL() {
    verifyDefaultClauseOnVarcharColumn(POSTGRESQL, "CREATE TABLE table_42 (status VARCHAR (1) DEFAULT 'P' NOT NULL)");
  }

  private static void verifyDefaultClauseOnVarcharColumn(Dialect dialect, String expectedSql) {
    List<String> stmts = new CreateTableBuilder(dialect, TABLE_NAME)
      .addColumn(newVarcharColumnDefBuilder().setColumnName("status").setLimit(1).setIsNullable(false).setDefaultValue("P").build())
      .build();
    assertThat(stmts).containsExactly(expectedSql);
  }

  @Test
  public void build_adds_DEFAULT_clause_on_boolean_column_on_H2() {
    verifyDefaultClauseOnBooleanColumn(H2, "CREATE TABLE table_42 (enabled BOOLEAN DEFAULT true NOT NULL)");
  }

  @Test
  public void build_adds_DEFAULT_clause_on_boolean_column_on_MSSQL() {
    verifyDefaultClauseOnBooleanColumn(MS_SQL, "CREATE TABLE table_42 (enabled BIT DEFAULT 1 NOT NULL)");
  }

  @Test
  public void build_adds_DEFAULT_clause_on_boolean_column_on_Oracle() {
    verifyDefaultClauseOnBooleanColumn(ORACLE, "CREATE TABLE table_42 (enabled NUMBER(1) DEFAULT 1 NOT NULL)");
  }

  @Test
  public void build_adds_DEFAULT_clause_on_boolean_column_on_PostgreSQL() {
    verifyDefaultClauseOnBooleanColumn(POSTGRESQL, "CREATE TABLE table_42 (enabled BOOLEAN DEFAULT true NOT NULL)");
  }

  private static void verifyDefaultClauseOnBooleanColumn(Dialect dialect, String expectedSql) {
    List<String> stmts = new CreateTableBuilder(dialect, TABLE_NAME)
      .addColumn(newBooleanColumnDefBuilder().setColumnName("enabled").setIsNullable(false).setDefaultValue(true).build())
      .build();
    assertThat(stmts).containsExactly(expectedSql);
  }

  private static String bigIntSqlType(Dialect dialect) {
    return Oracle.ID.equals(dialect.getId()) ? "NUMBER (38)" : "BIGINT";
  }

}
