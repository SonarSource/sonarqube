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
package org.sonar.db.charset;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sonar.api.utils.MessageException;

import static com.google.common.collect.Sets.immutableEnumSet;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.charset.DatabaseCharsetChecker.Flag.AUTO_REPAIR_COLLATION;

@RunWith(DataProviderRunner.class)
public class MssqlCharsetHandlerTest {

  private static final String TABLE_ISSUES = "issues";
  private static final String TABLE_PROJECTS = "projects";
  private static final String COLUMN_KEE = "kee";
  private static final String COLUMN_NAME = "name";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  SqlExecutor selectExecutor = mock(SqlExecutor.class);
  MssqlCharsetHandler underTest = new MssqlCharsetHandler(selectExecutor);

  @Test
  public void do_not_fail_if_charsets_of_all_columns_are_CS_AS() throws Exception {
    answerColumns(asList(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "Latin1_General", "Latin1_General_CS_AS", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CS_AS", "varchar", 10, false)));

    underTest.handle(mock(Connection.class), Collections.<DatabaseCharsetChecker.Flag>emptySet());
  }

  @Test
  public void fail_if_a_column_is_case_insensitive_and_repair_is_disabled() throws Exception {
    answerColumns(asList(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "Latin1_General", "Latin1_General_CS_AS", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CI_AI", "varchar", 10, false)));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Case-sensitive and accent-sensitive collation is required for database columns [projects.name]");
    Connection connection = mock(Connection.class);
    underTest.handle(connection, Collections.<DatabaseCharsetChecker.Flag>emptySet());

    verify(selectExecutor, never()).executeUpdate(any(Connection.class), anyString());
  }

  @Test
  public void repair_case_insensitive_column_without_index() throws Exception {
    answerColumns(asList(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "Latin1_General", "Latin1_General_CS_AS", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CI_AI", "varchar", 10, false)));

    Connection connection = mock(Connection.class);
    underTest.handle(connection, immutableEnumSet(AUTO_REPAIR_COLLATION));

    verify(selectExecutor).executeUpdate(connection, "ALTER TABLE projects ALTER COLUMN name varchar(10) COLLATE Latin1_General_CS_AS NOT NULL");
  }

  @Test
  public void repair_case_insensitive_column_with_indices() throws Exception {
    answerColumns(asList(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "Latin1_General", "Latin1_General_CS_AS", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CI_AI", "varchar", 10, false)));
    answerIndices(asList(
      new MssqlCharsetHandler.ColumnIndex("projects_name", false, "name"),
      // This index is on two columns. Note that it does not make sense for table "projects" !
      new MssqlCharsetHandler.ColumnIndex("projects_login_and_name", true, "login,name")));

    Connection connection = mock(Connection.class);
    underTest.handle(connection, immutableEnumSet(AUTO_REPAIR_COLLATION));

    verify(selectExecutor).executeUpdate(connection, "DROP INDEX projects.projects_name");
    verify(selectExecutor).executeUpdate(connection, "DROP INDEX projects.projects_login_and_name");
    verify(selectExecutor).executeUpdate(connection, "ALTER TABLE projects ALTER COLUMN name varchar(10) COLLATE Latin1_General_CS_AS NOT NULL");
    verify(selectExecutor).executeUpdate(connection, "CREATE  INDEX projects_name ON projects (name)");
    verify(selectExecutor).executeUpdate(connection, "CREATE UNIQUE INDEX projects_login_and_name ON projects (login,name)");
  }

  @Test
  @UseDataProvider("combinationsOfCsAsAndSuffix")
  public void repair_case_insensitive_accent_insensitive_combinations_with_or_without_suffix(String collation, String expectedCollation) throws Exception {
    answerColumns(Collections.singletonList(new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "Latin1_General", collation, "varchar", 10, false)));

    Connection connection = mock(Connection.class);
    underTest.handle(connection, immutableEnumSet(AUTO_REPAIR_COLLATION));

    verify(selectExecutor).executeUpdate(connection, "ALTER TABLE issues ALTER COLUMN kee varchar(10) COLLATE " + expectedCollation + " NOT NULL");
  }

  @DataProvider
  public static Object[][] combinationsOfCsAsAndSuffix() {
    List<String[]> res = new ArrayList<>();
    for (String sensitivity : Arrays.asList("CI_AI", "CI_AS", "CS_AI")) {
      for (String suffix : Arrays.asList("", "_KS_WS")) {
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
    // returned size is -1
    answerColumns(asList(new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CI_AI", "nvarchar", -1, false)));
    answerIndices(Collections.<MssqlCharsetHandler.ColumnIndex>emptyList());

    Connection connection = mock(Connection.class);
    underTest.handle(connection, immutableEnumSet(AUTO_REPAIR_COLLATION));

    verify(selectExecutor).executeUpdate(connection, "ALTER TABLE projects ALTER COLUMN name nvarchar(max) COLLATE Latin1_General_CS_AS NOT NULL");
  }

  @Test
  public void do_not_repair_system_tables_of_sql_azure() throws Exception {
    answerColumns(asList(new ColumnDef("sys.sysusers", COLUMN_NAME, "Latin1_General", "Latin1_General_CI_AI", "varchar", 10, false)));

    Connection connection = mock(Connection.class);
    underTest.handle(connection, immutableEnumSet(AUTO_REPAIR_COLLATION));

    verify(selectExecutor, never()).executeUpdate(any(Connection.class), anyString());
  }

  @Test
  @UseDataProvider("combinationOfBinAndSuffix")
  public void do_not_repair_if_collation_contains_BIN(String collation) throws Exception {
    answerColumns(asList(new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", collation, "varchar", 10, false)));

    Connection connection = mock(Connection.class);
    underTest.handle(connection, immutableEnumSet(AUTO_REPAIR_COLLATION));

    verify(selectExecutor, never()).executeUpdate(any(Connection.class), anyString());
  }

  @DataProvider
  public static Object[][] combinationOfBinAndSuffix() {
    return Arrays.asList("", "_KS_WS")
      .stream()
      .map(suffix -> new String[] {format("Latin1_General_BIN%s", suffix)})
      .toArray(Object[][]::new);
  }

  @Test
  @UseDataProvider("combinationOfBin2AndSuffix")
  public void do_not_repair_if_collation_contains_BIN2(String collation) throws Exception {
    answerColumns(asList(new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", collation, "varchar", 10, false)));

    Connection connection = mock(Connection.class);
    underTest.handle(connection, immutableEnumSet(AUTO_REPAIR_COLLATION));

    verify(selectExecutor, never()).executeUpdate(any(Connection.class), anyString());
  }

  @DataProvider
  public static Object[][] combinationOfBin2AndSuffix() {
    return Arrays.asList("", "_KS_WS")
      .stream()
      .map(suffix -> new String[] {format("Latin1_General_BIN2%s", suffix)})
      .toArray(Object[][]::new);
  }

  private void answerColumns(List<ColumnDef> columnDefs) throws SQLException {
    when(selectExecutor.executeSelect(any(Connection.class), anyString(), eq(ColumnDef.ColumnDefRowConverter.INSTANCE))).thenReturn(columnDefs);
  }

  private void answerIndices(List<MssqlCharsetHandler.ColumnIndex> indices) throws SQLException {
    when(selectExecutor.executeSelect(any(Connection.class), anyString(), eq(MssqlCharsetHandler.ColumnIndexConverter.INSTANCE))).thenReturn(indices);
  }
}
