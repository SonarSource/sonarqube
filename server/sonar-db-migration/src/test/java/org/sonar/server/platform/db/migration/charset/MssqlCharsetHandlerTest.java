/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.charset;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.sonar.api.utils.MessageException;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(DataProviderRunner.class)
public class MssqlCharsetHandlerTest {

  private static final String TABLE_ISSUES = "issues";
  private static final String TABLE_PROJECTS = "projects";
  private static final String COLUMN_KEE = "kee";
  private static final String COLUMN_NAME = "name";


  private SqlExecutor sqlExecutor = mock(SqlExecutor.class);
  private MssqlMetadataReader metadata = mock(MssqlMetadataReader.class);
  private MssqlCharsetHandler underTest = new MssqlCharsetHandler(sqlExecutor, metadata);
  private Connection connection = mock(Connection.class);

  @Test
  public void fresh_install_verifies_that_default_collation_is_CS_AS() throws SQLException {
    answerDefaultCollation("Latin1_General_CS_AS");

    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);

    verify(metadata).getDefaultCollation(connection);
  }

  @Test
  public void fresh_install_fails_if_default_collation_is_not_CS_AS() throws SQLException {
    answerDefaultCollation("Latin1_General_CI_AI");

    assertThatThrownBy(() -> underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL))
      .isInstanceOf(MessageException.class)
      .hasMessage("Database collation must be case-sensitive and accent-sensitive. It is Latin1_General_CI_AI but should be Latin1_General_CS_AS.");
  }

  @Test
  public void upgrade_fails_if_default_collation_is_not_CS_AS() throws SQLException {
    answerDefaultCollation("Latin1_General_CI_AI");

    assertThatThrownBy(() -> underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE))
      .isInstanceOf(MessageException.class)
      .hasMessage("Database collation must be case-sensitive and accent-sensitive. It is Latin1_General_CI_AI but should be Latin1_General_CS_AS.");
  }

  @Test
  public void upgrade_checks_that_columns_are_CS_AS() throws SQLException {
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "Latin1_General", "Latin1_General_CS_AS", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CS_AS", "varchar", 10, false));

    // do not fail
    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);
  }

  @Test
  public void upgrade_repairs_CI_AI_columns() throws SQLException {
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "Latin1_General", "Latin1_General_CS_AS", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CI_AI", "varchar", 10, false));

    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);

    verify(sqlExecutor).executeDdl(connection, "ALTER TABLE projects ALTER COLUMN name varchar(10) COLLATE Latin1_General_CS_AS NOT NULL");
  }

  @Test
  public void upgrade_repairs_indexed_CI_AI_columns() throws SQLException {
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "Latin1_General", "Latin1_General_CS_AS", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CI_AI", "varchar", 10, false));
    answerIndices(
      new MssqlCharsetHandler.ColumnIndex("projects_name", false, "name"),
      // This index is on two columns. Note that it does not make sense for table "projects" !
      new MssqlCharsetHandler.ColumnIndex("projects_login_and_name", true, "login,name"));

    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);

    verify(sqlExecutor).executeDdl(connection, "DROP INDEX projects.projects_name");
    verify(sqlExecutor).executeDdl(connection, "DROP INDEX projects.projects_login_and_name");
    verify(sqlExecutor).executeDdl(connection, "ALTER TABLE projects ALTER COLUMN name varchar(10) COLLATE Latin1_General_CS_AS NOT NULL");
    verify(sqlExecutor).executeDdl(connection, "CREATE  INDEX projects_name ON projects (name)");
    verify(sqlExecutor).executeDdl(connection, "CREATE UNIQUE INDEX projects_login_and_name ON projects (login,name)");
  }

  @Test
  @UseDataProvider("combinationsOfCsAsAndSuffix")
  public void repair_case_insensitive_accent_insensitive_combinations_with_or_without_suffix(String collation, String expectedCollation)
    throws Exception {
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "Latin1_General", collation, "varchar", 10, false));

    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);

    verify(sqlExecutor).executeDdl(connection, "ALTER TABLE issues ALTER COLUMN kee varchar(10) COLLATE " + expectedCollation + " NOT NULL");
  }

  @DataProvider
  public static Object[][] combinationsOfCsAsAndSuffix() {
    List<String[]> res = new ArrayList<>();
    for (String sensitivity : asList("CI_AI", "CI_AS", "CS_AI")) {
      for (String suffix : asList("", "_KS_WS")) {
        res.add(new String[] {
          format("Latin1_General_%s%s", sensitivity, suffix),
          format("Latin1_General_CS_AS%s", suffix)
        });
      }
    }
    return res.toArray(new Object[0][]);
  }

  @Test
  public void repair_drops_and_recreates_computed_columns_that_depend_on_the_column() throws SQLException {
    when(connection.getAutoCommit()).thenReturn(true);
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(new ColumnDef("components", COLUMN_KEE, "Latin1_General", "Latin1_General_CI_AI", "nvarchar", 1000, true));
    answerIndices(new MssqlCharsetHandler.ColumnIndex("components_lower_kee", false, "kee_lower_hash"));
    when(metadata.getDependentComputedColumns(same(connection), any(ColumnDef.class))).thenReturn(asList(
      new MssqlCharsetHandler.ComputedColumn("kee_lower_hash", "(CONVERT(binary(32),hashbytes('SHA2_256',lower([kee]))))")));

    underTest.handle(connection, DatabaseCharsetChecker.State.STARTUP);

    InOrder order = inOrder(connection, sqlExecutor);
    order.verify(connection).setAutoCommit(false);
    order.verify(sqlExecutor).executeDdl(connection, "DROP INDEX components.components_lower_kee");
    order.verify(sqlExecutor).executeDdl(connection, "ALTER TABLE components DROP COLUMN kee_lower_hash");
    order.verify(sqlExecutor).executeDdl(connection, "ALTER TABLE components ALTER COLUMN kee nvarchar(1000) COLLATE Latin1_General_CS_AS NULL");
    order.verify(sqlExecutor).executeDdl(connection, "ALTER TABLE components ADD kee_lower_hash AS (CONVERT(binary(32),hashbytes('SHA2_256',lower([kee]))))");
    order.verify(sqlExecutor).executeDdl(connection, "CREATE  INDEX components_lower_kee ON components (kee_lower_hash)");
    order.verify(connection).commit();
    order.verify(connection).setAutoCommit(true);
  }

  @Test
  public void repair_rolls_back_when_a_statement_fails() throws SQLException {
    when(connection.getAutoCommit()).thenReturn(true);
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(new ColumnDef("components", COLUMN_KEE, "Latin1_General", "Latin1_General_CI_AI", "nvarchar", 1000, true));
    answerIndices(new MssqlCharsetHandler.ColumnIndex("components_lower_kee", false, "kee_lower_hash"));
    when(metadata.getDependentComputedColumns(same(connection), any(ColumnDef.class))).thenReturn(asList(
      new MssqlCharsetHandler.ComputedColumn("kee_lower_hash", "(lower([kee]))")));
    doThrow(new SQLException("alter failed")).when(sqlExecutor).executeDdl(same(connection), contains("ALTER COLUMN"));

    assertThatThrownBy(() -> underTest.handle(connection, DatabaseCharsetChecker.State.STARTUP))
      .isInstanceOf(SQLException.class)
      .hasMessage("alter failed");

    verify(sqlExecutor).executeDdl(connection, "DROP INDEX components.components_lower_kee");
    verify(sqlExecutor).executeDdl(connection, "ALTER TABLE components DROP COLUMN kee_lower_hash");
    verify(connection).rollback();
    verify(connection, never()).commit();
    verify(connection).setAutoCommit(true);
  }

  @Test
  public void repair_rolls_back_when_an_error_escapes() throws SQLException {
    when(connection.getAutoCommit()).thenReturn(true);
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(new ColumnDef("components", COLUMN_KEE, "Latin1_General", "Latin1_General_CI_AI", "nvarchar", 1000, true));
    answerIndices(new MssqlCharsetHandler.ColumnIndex("components_lower_kee", false, "kee_lower_hash"));
    when(metadata.getDependentComputedColumns(same(connection), any(ColumnDef.class))).thenReturn(asList(
      new MssqlCharsetHandler.ComputedColumn("kee_lower_hash", "(lower([kee]))")));
    doThrow(new OutOfMemoryError("oom")).when(sqlExecutor).executeDdl(same(connection), contains("ALTER COLUMN"));

    assertThatThrownBy(() -> underTest.handle(connection, DatabaseCharsetChecker.State.STARTUP))
      .isInstanceOf(OutOfMemoryError.class)
      .hasMessage("oom");

    verify(connection).rollback();
    verify(connection, never()).commit();
    verify(connection).setAutoCommit(true);
  }

  @Test
  public void repair_does_not_restore_auto_commit_when_rollback_fails() throws SQLException {
    when(connection.getAutoCommit()).thenReturn(true);
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(new ColumnDef("components", COLUMN_KEE, "Latin1_General", "Latin1_General_CI_AI", "nvarchar", 1000, true));
    answerIndices(new MssqlCharsetHandler.ColumnIndex("components_lower_kee", false, "kee_lower_hash"));
    when(metadata.getDependentComputedColumns(same(connection), any(ColumnDef.class))).thenReturn(asList(
      new MssqlCharsetHandler.ComputedColumn("kee_lower_hash", "(lower([kee]))")));
    doThrow(new SQLException("alter failed")).when(sqlExecutor).executeDdl(same(connection), contains("ALTER COLUMN"));
    doThrow(new SQLException("rollback failed")).when(connection).rollback();

    assertThatThrownBy(() -> underTest.handle(connection, DatabaseCharsetChecker.State.STARTUP))
      .isInstanceOf(SQLException.class)
      .hasMessage("alter failed")
      .satisfies(thrown -> assertThat(thrown.getSuppressed()).extracting(Throwable::getMessage).containsExactly("rollback failed"));

    verify(connection, never()).commit();
    verify(connection, never()).setAutoCommit(true);
  }

  @Test
  public void repair_keeps_the_original_failure_when_restoring_auto_commit_fails() throws SQLException {
    when(connection.getAutoCommit()).thenReturn(true);
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(new ColumnDef("components", COLUMN_KEE, "Latin1_General", "Latin1_General_CI_AI", "nvarchar", 1000, true));
    answerIndices(new MssqlCharsetHandler.ColumnIndex("components_lower_kee", false, "kee_lower_hash"));
    when(metadata.getDependentComputedColumns(same(connection), any(ColumnDef.class))).thenReturn(asList(
      new MssqlCharsetHandler.ComputedColumn("kee_lower_hash", "(lower([kee]))")));
    doThrow(new SQLException("alter failed")).when(sqlExecutor).executeDdl(same(connection), contains("ALTER COLUMN"));
    doThrow(new SQLException("restore failed")).when(connection).setAutoCommit(true);

    assertThatThrownBy(() -> underTest.handle(connection, DatabaseCharsetChecker.State.STARTUP))
      .isInstanceOf(SQLException.class)
      .hasMessage("alter failed")
      .satisfies(thrown -> assertThat(thrown.getSuppressed()).extracting(Throwable::getMessage).containsExactly("restore failed"));
  }

  @Test
  public void repair_recreates_a_persisted_computed_column() throws SQLException {
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(new ColumnDef("components", COLUMN_KEE, "Latin1_General", "Latin1_General_CI_AI", "nvarchar", 1000, true));
    answerIndices();
    when(metadata.getDependentComputedColumns(same(connection), any(ColumnDef.class))).thenReturn(asList(
      new MssqlCharsetHandler.ComputedColumn("kee_lower_hash", "(CONVERT(binary(32),hashbytes('SHA2_256',lower([kee]))))", true)));

    underTest.handle(connection, DatabaseCharsetChecker.State.STARTUP);

    verify(sqlExecutor).executeDdl(connection, "ALTER TABLE components ADD kee_lower_hash AS (CONVERT(binary(32),hashbytes('SHA2_256',lower([kee])))) PERSISTED");
  }

  @Test
  public void repair_drops_an_index_that_exists_only_on_the_computed_column() throws SQLException {
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(new ColumnDef("components", COLUMN_KEE, "Latin1_General", "Latin1_General_CI_AI", "nvarchar", 1000, true));
    when(metadata.getColumnIndices(same(connection), any(ColumnDef.class))).thenAnswer(invocation -> {
      ColumnDef column = invocation.getArgument(1);
      if ("kee_lower_hash".equals(column.getColumn())) {
        return asList(new MssqlCharsetHandler.ColumnIndex("components_lower_kee", false, "kee_lower_hash"));
      }
      return asList(new MssqlCharsetHandler.ColumnIndex("components_kee", false, "kee"));
    });
    when(metadata.getDependentComputedColumns(same(connection), any(ColumnDef.class))).thenReturn(asList(
      new MssqlCharsetHandler.ComputedColumn("kee_lower_hash", "(lower([kee]))")));

    underTest.handle(connection, DatabaseCharsetChecker.State.STARTUP);

    verify(sqlExecutor).executeDdl(connection, "DROP INDEX components.components_kee");
    verify(sqlExecutor).executeDdl(connection, "DROP INDEX components.components_lower_kee");
    verify(sqlExecutor).executeDdl(connection, "CREATE  INDEX components_lower_kee ON components (kee_lower_hash)");
  }

  @Test
  public void computedColumnConverter_readsTheRow() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getString(1)).thenReturn("kee_lower_hash");
    when(rs.getString(2)).thenReturn("(lower([kee]))");
    when(rs.getBoolean(3)).thenReturn(false);

    MssqlCharsetHandler.ComputedColumn column = MssqlCharsetHandler.ComputedColumnConverter.INSTANCE.convert(rs);

    assertThat(column.getName()).isEqualTo("kee_lower_hash");
    assertThat(column.getDefinition()).isEqualTo("(lower([kee]))");
  }

  @Test
  public void support_the_max_size_of_varchar_column() throws Exception {
    answerDefaultCollation("Latin1_General_CS_AS");
    // returned size is -1
    answerColumnDefs(new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CI_AI", "nvarchar", -1, false));
    answerIndices();

    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);

    verify(sqlExecutor).executeDdl(connection, "ALTER TABLE projects ALTER COLUMN name nvarchar(max) COLLATE Latin1_General_CS_AS NOT NULL");
  }

  @Test
  public void do_not_repair_system_tables_of_sql_azure() throws Exception {
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(new ColumnDef("sys.sysusers", COLUMN_NAME, "Latin1_General", "Latin1_General_CI_AI", "varchar", 10, false));

    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);

    verify(sqlExecutor, never()).executeDdl(any(Connection.class), anyString());
  }

  @Test
  @UseDataProvider("combinationOfBinAndSuffix")
  public void do_not_repair_if_collation_contains_BIN(String collation) throws Exception {
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", collation, "varchar", 10, false));

    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);

    verify(sqlExecutor, never()).executeDdl(any(Connection.class), anyString());
  }

  @DataProvider
  public static Object[][] combinationOfBinAndSuffix() {
    return Stream.of("", "_KS_WS")
      .map(suffix -> new String[] {format("Latin1_General_BIN%s", suffix)})
      .toArray(Object[][]::new);
  }

  @Test
  @UseDataProvider("combinationOfBin2AndSuffix")
  public void do_not_repair_if_collation_contains_BIN2(String collation) throws Exception {
    answerDefaultCollation("Latin1_General_CS_AS");
    answerColumnDefs(new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", collation, "varchar", 10, false));

    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);

    verify(sqlExecutor, never()).executeDdl(any(Connection.class), anyString());
  }

  @DataProvider
  public static Object[][] combinationOfBin2AndSuffix() {
    return Stream.of("", "_KS_WS")
      .map(suffix -> new String[] {format("Latin1_General_BIN2%s", suffix)})
      .toArray(Object[][]::new);
  }

  /**
   * SONAR-7988
   */
  @Test
  public void fix_Latin1_CS_AS_columns_created_in_5_x() throws SQLException {
    answerDefaultCollation("SQL_Latin1_General_CP1_CS_AS");
    answerColumnDefs(new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CS_AS", "nvarchar", 10, false));

    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);

    verify(sqlExecutor).executeDdl(connection, "ALTER TABLE projects ALTER COLUMN name nvarchar(10) COLLATE SQL_Latin1_General_CP1_CS_AS NOT NULL");
  }

  private void answerColumnDefs(ColumnDef... columnDefs) throws SQLException {
    when(metadata.getColumnDefs(connection)).thenReturn(asList(columnDefs));
  }

  private void answerDefaultCollation(String defaultCollation) throws SQLException {
    when(metadata.getDefaultCollation(connection)).thenReturn(defaultCollation);
  }

  private void answerIndices(MssqlCharsetHandler.ColumnIndex... indices) throws SQLException {
    when(metadata.getColumnIndices(same(connection), any(ColumnDef.class))).thenReturn(asList(indices));
  }
}
