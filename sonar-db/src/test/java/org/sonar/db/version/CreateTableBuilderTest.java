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
package org.sonar.db.version;

import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dialect.H2;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.db.version.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.db.version.BlobColumnDef.newBlobColumnDefBuilder;
import static org.sonar.db.version.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.db.version.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.db.version.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateTableBuilderTest {
  private static final H2 H2_DIALECT = new H2();
  private static final Oracle ORACLE = new Oracle();
  private static final Dialect[] ALL_DIALECTS = {H2_DIALECT, new MySql(), new MsSql(), new PostgreSql(), ORACLE};
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
    expectedException.expectMessage("table name can't be null");

    new CreateTableBuilder(mock(Dialect.class), null);
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
  public void withPkConstraintName_throws_NPE_if_ColumnDef_is_null() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("primary key constraint name can't be null");

    underTest.withPkConstraintName(null);
  }

  @Test
  public void build_lowers_case_of_table_name() {
    List<String> stmts = new CreateTableBuilder(H2_DIALECT, "SOmE_TABLe_NamE")
      .addColumn(newBooleanColumnDefBuilder().setColumnName("bool_col").build())
      .build();
    assertThat(stmts).hasSize(1);
    assertThat(stmts.iterator().next())
      .startsWith("CREATE TABLE some_table_name (")
      .endsWith(")");
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
          .isEqualTo("CREATE TABLE " + TABLE_NAME + " (" +
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
          .isEqualTo("CREATE TABLE " + TABLE_NAME + " (" +
            "bg_col " +
            bigIntSqlType(dialect) +
            " NOT NULL" +
            ")");
      });
  }

  @Test
  public void build_of_single_column_table() {
    List<String> stmts = new CreateTableBuilder(H2_DIALECT, TABLE_NAME)
      .addColumn(newBooleanColumnDefBuilder().setColumnName("bool_col_1").build())
      .build();
    assertThat(stmts).hasSize(1);

    assertThat(stmts.iterator().next()).isEqualTo("CREATE TABLE table_42 (bool_col_1 BOOLEAN NULL)");
  }

  @Test
  public void build_table_with_pk() {
    List<String> stmts = new CreateTableBuilder(H2_DIALECT, TABLE_NAME)
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
          .isEqualTo("CREATE TABLE " + TABLE_NAME + " (" +
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
          .withPkConstraintName("My_PK")
          .build();
        assertThat(stmts).hasSize(1);

        assertThat(stmts.iterator().next())
          .isEqualTo("CREATE TABLE " + TABLE_NAME + " (" +
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
          .isEqualTo("CREATE TABLE " + TABLE_NAME + " (" +
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
          .withPkConstraintName("My_PK")
          .build();
        assertThat(stmts).hasSize(1);

        assertThat(stmts.iterator().next())
          .isEqualTo("CREATE TABLE " + TABLE_NAME + " (" +
            "bg_col_1 " + bigIntSqlType(dialect) + " NOT NULL," +
            "bg_col_2 " + bigIntSqlType(dialect) + " NOT NULL," +
            " CONSTRAINT my_pk PRIMARY KEY (bg_col_1,bg_col_2)" +
            ")");
      });
  }

  @Test
  public void builds_adds_LOB_storage_clause_on_Oracle_for_CLOB_column() {
    List<String> stmts = new CreateTableBuilder(ORACLE, TABLE_NAME)
      .addColumn(newClobColumnDefBuilder().setColumnName("clob_1").setIsNullable(false).build())
      .build();
    assertThat(stmts).hasSize(1);

    assertThat(stmts.iterator().next()).isEqualTo(
      "CREATE TABLE " + TABLE_NAME + " (" +
        "clob_1 CLOB NOT NULL)" +
        " LOB (clob_1) STORE AS SECUREFILE (RETENTION NONE NOCACHE NOLOGGING)");
  }

  @Test
  public void builds_adds_LOB_storage_clause_on_Oracle_for_BLOB_column() {
    List<String> stmts = new CreateTableBuilder(ORACLE, TABLE_NAME)
      .addColumn(newBlobColumnDefBuilder().setColumnName("blob_1").setIsNullable(false).build())
      .build();
    assertThat(stmts).hasSize(1);

    assertThat(stmts.iterator().next()).isEqualTo(
      "CREATE TABLE " + TABLE_NAME + " (" +
        "blob_1 BLOB NOT NULL)" +
        " LOB (blob_1) STORE AS SECUREFILE (RETENTION NONE NOCACHE NOLOGGING)");
  }

  @Test
  public void build_does_not_add_LOB_storage_clause_for_CLOB_column_for_other_than_Oracle() {
    Arrays.stream(ALL_DIALECTS)
      .filter(dialect -> dialect != ORACLE)
      .forEach(dialect -> {
        List<String> stmts = new CreateTableBuilder(dialect, TABLE_NAME)
          .addColumn(newClobColumnDefBuilder().setColumnName("clob_1").setIsNullable(false).build())
          .build();
        assertThat(stmts).hasSize(1);

        assertThat(stmts.iterator().next()).doesNotContain("STORE AS SECUREFILE");
      });
  }

  @Test
  public void build_does_not_add_LOB_storage_clause_for_BLOB_column_for_other_than_Oracle() {
    Arrays.stream(ALL_DIALECTS)
      .filter(dialect -> dialect != ORACLE)
      .forEach(dialect -> {
        List<String> stmts = new CreateTableBuilder(dialect, TABLE_NAME)
          .addColumn(newBlobColumnDefBuilder().setColumnName("blob_1").setIsNullable(false).build())
          .build();
        assertThat(stmts).hasSize(1);

        assertThat(stmts.iterator().next()).doesNotContain("STORE AS SECUREFILE");
      });
  }

  private static String bigIntSqlType(Dialect dialect) {
    return Oracle.ID.equals(dialect.getId()) ? "NUMBER (38)" : "BIGINT";
  }

}
