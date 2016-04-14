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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
  public void does_not_fail_if_charsets_of_all_columns_are_utf8() throws Exception {
    answerColumns(asList(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "Latin1_General", "Latin1_General_CS_AS", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CS_AS", "varchar", 10, false)));

    underTest.handle(mock(Connection.class), true);
  }

  @Test
  public void repairs_case_insensitive_column_without_index() throws Exception {
    answerColumns(asList(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "Latin1_General", "Latin1_General_CS_AS", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CI_AI", "varchar", 10, false)));

    Connection connection = mock(Connection.class);
    underTest.handle(connection, false);

    verify(selectExecutor).executeUpdate(connection, "ALTER TABLE projects ALTER COLUMN name varchar(10) COLLATE Latin1_General_CS_AS NOT NULL");
  }

  @Test
  public void repairs_case_insensitive_column_with_indices() throws Exception {
    answerColumns(asList(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "Latin1_General", "Latin1_General_CS_AS", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CI_AI", "varchar", 10, false)));
    answerIndices(asList(
      new MssqlCharsetHandler.ColumnIndex("projects_name", false, "name"),
      // This index is on two columns. Note that it does not make sense for table "projects" !
      new MssqlCharsetHandler.ColumnIndex("projects_login_and_name", true, "login,name")));

    Connection connection = mock(Connection.class);
    underTest.handle(connection, false);

    verify(selectExecutor).executeUpdate(connection, "DROP INDEX projects.projects_name");
    verify(selectExecutor).executeUpdate(connection, "DROP INDEX projects.projects_login_and_name");
    verify(selectExecutor).executeUpdate(connection, "ALTER TABLE projects ALTER COLUMN name varchar(10) COLLATE Latin1_General_CS_AS NOT NULL");
    verify(selectExecutor).executeUpdate(connection, "CREATE  INDEX projects_name ON projects (name)");
    verify(selectExecutor).executeUpdate(connection, "CREATE UNIQUE INDEX projects_login_and_name ON projects (login,name)");
  }

  @Test
  public void support_the_max_size_of_varchar_column() throws Exception {
    // returned size is -1
    answerColumns(asList(new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "Latin1_General", "Latin1_General_CI_AI", "nvarchar", -1, false)));
    answerIndices(Collections.<MssqlCharsetHandler.ColumnIndex>emptyList());

    Connection connection = mock(Connection.class);
    underTest.handle(connection, false);

    verify(selectExecutor).executeUpdate(connection, "ALTER TABLE projects ALTER COLUMN name nvarchar(max) COLLATE Latin1_General_CS_AS NOT NULL");
  }

  private void answerColumns(List<ColumnDef> columnDefs) throws SQLException {
    when(selectExecutor.executeSelect(any(Connection.class), anyString(), eq(ColumnDef.ColumnDefRowConverter.INSTANCE))).thenReturn(columnDefs);
  }

  private void answerIndices(List<MssqlCharsetHandler.ColumnIndex> indices) throws SQLException {
    when(selectExecutor.executeSelect(any(Connection.class), anyString(), eq(MssqlCharsetHandler.ColumnIndexConverter.INSTANCE))).thenReturn(indices);
  }
}
