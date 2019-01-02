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
package org.sonar.server.platform.db.migration.charset;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.utils.MessageException;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
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

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Database collation must be case-sensitive and accent-sensitive. It is Latin1_General_CI_AI but should be Latin1_General_CS_AS.");
    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
  }

  @Test
  public void upgrade_fails_if_default_collation_is_not_CS_AS() throws SQLException {
    answerDefaultCollation("Latin1_General_CI_AI");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Database collation must be case-sensitive and accent-sensitive. It is Latin1_General_CI_AI but should be Latin1_General_CS_AS.");
    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);
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
    return res.stream().toArray(Object[][]::new);
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
