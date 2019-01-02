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

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class MysqlCharsetHandlerTest {

  private static final String TABLE_ISSUES = "issues";
  private static final String TABLE_PROJECTS = "projects";
  private static final String COLUMN_KEE = "kee";
  private static final String COLUMN_NAME = "name";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SqlExecutor sqlExecutor = mock(SqlExecutor.class);
  private Connection connection = mock(Connection.class);
  private MysqlCharsetHandler underTest = new MysqlCharsetHandler(sqlExecutor);

  @Test
  public void upgrade_verifies_that_columns_are_utf8_and_case_sensitive() throws Exception {
    answerColumnDef(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "utf8", "utf8_bin", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "utf8", "utf8_bin", "varchar", 10, false));

    // all columns are utf8
    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);
  }

  @Test
  public void fresh_install_does_not_verify_anything() throws Exception {
    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
    verifyZeroInteractions(sqlExecutor);
  }

  @Test
  public void regular_startup_does_not_verify_anything() throws Exception {
    underTest.handle(connection, DatabaseCharsetChecker.State.STARTUP);
    verifyZeroInteractions(sqlExecutor);
  }

  @Test
  public void repair_case_insensitive_column() throws Exception {
    answerColumnDef(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "big5_chinese", "big5_chinese_ci", "varchar", 10, false),
      new ColumnDef(TABLE_PROJECTS, COLUMN_NAME, "latin1", "latin1_swedish_ci", "varchar", 10, false));

    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);

    verify(sqlExecutor).executeDdl(connection, "ALTER TABLE issues MODIFY kee varchar(10) CHARACTER SET 'big5_chinese' COLLATE 'big5_bin' NOT NULL");
    verify(sqlExecutor).executeDdl(connection, "ALTER TABLE projects MODIFY name varchar(10) CHARACTER SET 'latin1' COLLATE 'latin1_bin' NOT NULL");
  }

  @Test
  public void size_should_be_ignored_on_longtext_column() throws Exception {
    answerColumnDef(
      new ColumnDef(TABLE_ISSUES, COLUMN_KEE, "latin1", "latin1_german1_ci", "longtext", 4_294_967_295L, false));

    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);

    verify(sqlExecutor).executeDdl(connection, "ALTER TABLE " + TABLE_ISSUES + " MODIFY " + COLUMN_KEE + " longtext CHARACTER SET 'latin1' COLLATE 'latin1_bin' NOT NULL");
  }

  private void answerColumnDef(ColumnDef... columnDefs) throws SQLException {
    when(sqlExecutor.select(any(Connection.class), anyString(), eq(ColumnDef.ColumnDefRowConverter.INSTANCE)))
      .thenReturn(asList(columnDefs));
  }
}
