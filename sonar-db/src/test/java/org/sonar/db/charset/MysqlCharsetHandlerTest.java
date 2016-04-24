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
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MysqlCharsetHandlerTest {

  private static final String TABLE_ISSUES = "issues";
  private static final String TABLE_PROJECTS = "projects";
  private static final String COLUMN_KEE = "kee";
  private static final String COLUMN_NAME = "name";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  SqlExecutor selectExecutor = mock(SqlExecutor.class);
  MysqlCharsetHandler underTest = new MysqlCharsetHandler(selectExecutor);

  @Test
  public void does_not_fail_if_charsets_of_all_columns_are_utf8() throws Exception {
    answerColumnDef(asList(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "utf8", "utf8_bin", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "utf8", "utf8_bin", "varchar", 10, false)));

    // all columns are utf8
    underTest.handle(mock(Connection.class), true);
  }

  @Test
  public void fails_if_not_utf8() throws Exception {
    answerColumnDef(asList(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "utf8", "utf8_bin", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_KEE, "latin1", "latin1_german1_ci", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "latin1", "latin1_swedish_ci", "varchar", 20, false)));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("UTF8 case-sensitive collation is required for database columns [projects.kee, projects.name]");
    underTest.handle(mock(Connection.class), true);
  }

  @Test
  public void repairs_case_insensitive_column() throws Exception {
    answerColumnDef(asList(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "utf8", "utf8_bin", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "latin1", "latin1_swedish_ci", "varchar", 10, false)));

    Connection connection = mock(Connection.class);
    underTest.handle(connection, false);

    verify(selectExecutor).executeUpdate(connection, "ALTER TABLE projects MODIFY name varchar(10) CHARACTER SET 'latin1' COLLATE 'latin1_bin' NOT NULL");
  }

  @Test
  public void size_should_be_ignored_on_longtext_column() throws Exception {
    answerColumnDef(asList(new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "latin1", "latin1_german1_ci", "longtext", 4_294_967_295L, false)));

    Connection connection = mock(Connection.class);
    underTest.handle(connection, false);

    verify(selectExecutor).executeUpdate(connection, "ALTER TABLE " + TABLE_ISSUES + " MODIFY " + COLUMN_KEE + " longtext CHARACTER SET 'latin1' COLLATE 'latin1_bin' NOT NULL");
  }

  @Test
  public void tests_toCaseSensitive() {
    assertThat(MysqlCharsetHandler.toCaseSensitive("big5_chinese_ci")).isEqualTo("big5_bin");
  }

  private void answerColumnDef(List<ColumnDef> columnDefs) throws SQLException {
    when(selectExecutor.executeSelect(any(Connection.class), anyString(), eq(ColumnDef.ColumnDefRowConverter.INSTANCE))).thenReturn(columnDefs);
  }
}
