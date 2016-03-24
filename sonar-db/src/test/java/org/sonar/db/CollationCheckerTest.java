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
package org.sonar.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.utils.MessageException;
import org.sonar.db.dialect.MsSql;
import org.sonar.db.dialect.MySql;
import org.sonar.db.dialect.Oracle;
import org.sonar.db.dialect.PostgreSql;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CollationCheckerTest {

  private static final String TABLE_ISSUES = "issues";
  private static final String TABLE_PROJECTS = "projects";
  private static final String COLUMN_KEE = "kee";
  private static final String COLUMN_NAME = "name";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  Database db = mock(Database.class, Mockito.RETURNS_MOCKS);
  CollationChecker.StatementExecutor statementExecutor = mock(CollationChecker.StatementExecutor.class);
  CollationChecker underTest = new CollationChecker(db, statementExecutor);

  @Test
  public void valid_oracle() throws Exception {
    when(db.getDialect()).thenReturn(new Oracle());
    answerSql(
      singletonList(new String[] {"UTF8"}), singletonList(new String[] {"BINARY"}));

    underTest.start();
  }

  @Test
  public void support_oracle_al32utf8() throws Exception {
    when(db.getDialect()).thenReturn(new Oracle());
    answerSql(
      singletonList(new String[] {"AL32UTF8"}), singletonList(new String[] {"BINARY"}));

    underTest.start();
  }

  @Test
  public void fail_if_oracle_is_not_utf8() throws Exception {
    when(db.getDialect()).thenReturn(new Oracle());
    answerSql(
      singletonList(new String[] {"LATIN"}), singletonList(new String[] {"BINARY"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Oracle must be have UTF8 charset and BINARY sort. NLS_CHARACTERSET is LATIN and NLS_SORT is BINARY.");

    underTest.start();
  }

  @Test
  public void fail_if_oracle_is_not_case_sensitive() throws Exception {
    when(db.getDialect()).thenReturn(new Oracle());
    answerSql(
      singletonList(new String[] {"UTF8"}), singletonList(new String[] {"LINGUISTIC"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Oracle must be have UTF8 charset and BINARY sort. NLS_CHARACTERSET is UTF8 and NLS_SORT is LINGUISTIC.");

    underTest.start();
  }

  @Test
  public void fail_if_can_not_get_oracle_charset() throws Exception {
    when(db.getDialect()).thenReturn(new Oracle());
    answerSql(Collections.<String[]>emptyList(), Collections.<String[]>emptyList());

    expectedException.expect(MessageException.class);

    underTest.start();
  }

  @Test
  public void valid_postgresql() throws Exception {
    when(db.getDialect()).thenReturn(new PostgreSql());
    answerSql(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "utf8"}));

    underTest.start();
  }

  @Test
  public void fail_if_postgresql_has_non_utf8_column() throws Exception {
    when(db.getDialect()).thenReturn(new PostgreSql());
    answerSql(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
      new String[] {TABLE_PROJECTS, COLUMN_KEE, "latin"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "latin"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Database columns [projects.kee, projects.name] must have UTF8 charset.");

    underTest.start();
  }

  @Test
  public void fail_if_postgresql_has_non_utf8_db() throws Exception {
    when(db.getDialect()).thenReturn(new PostgreSql());
    answerSql(
      // first request to get columns
      asList(
        new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
        new String[] {TABLE_PROJECTS, COLUMN_NAME, "" /* unset -> uses db collation */}),

      // second request to get db collation
      Arrays.<String[]>asList(new String[] {"latin"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Database charset is latin. It must be UTF8.");

    underTest.start();
  }

  @Test
  public void valid_postgresql_if_utf8_db() throws Exception {
    when(db.getDialect()).thenReturn(new PostgreSql());
    answerSql(
      // first request to get columns
      asList(
        new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
        new String[] {TABLE_PROJECTS, COLUMN_NAME, "" /* unset -> uses db collation */}),

      // second request to get db collation
      Arrays.<String[]>asList(new String[] {"utf8"}));

    // no error
    underTest.start();
  }

  @Test
  public void valid_mysql() throws Exception {
    when(db.getDialect()).thenReturn(new MySql());
    answerSql(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8", "utf8_bin"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "utf8", "utf8_bin"}));

    underTest.start();
  }

  @Test
  public void fail_if_mysql_is_not_utf8_charset() throws Exception {
    when(db.getDialect()).thenReturn(new MySql());
    answerSql(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8", "utf8_bin"},
      new String[] {TABLE_PROJECTS, COLUMN_KEE, "latin1", "utf8_bin"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "latin1", "utf8_bin"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("UTF8 charset and case-sensitive collation are required for database columns [projects.kee, projects.name]");

    underTest.start();
  }

  @Test
  public void fail_if_mysql_is_not_case_sensitive() throws Exception {
    when(db.getDialect()).thenReturn(new MySql());
    answerSql(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8", "utf8_bin"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "utf8", "latin1_swedish_ci"}));

    expectedException.expect(MessageException.class);

    underTest.start();
  }

  @Test
  public void valid_mssql() throws Exception {
    when(db.getDialect()).thenReturn(new MsSql());
    answerSql(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "Latin1_General_CS_AS"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "Latin1_General_CS_AS"}));

    underTest.start();
  }

  @Test
  public void fail_if_mssql_is_not_case_sensitive() throws Exception {
    when(db.getDialect()).thenReturn(new MsSql());
    answerSql(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "Latin1_General_CS_AS"},
      new String[] {TABLE_PROJECTS, COLUMN_KEE, "Latin1_General_CI_AI"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "Latin1_General_CI_AI"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Case-sensitive and accent-sensitive charset (CS_AS) is required for database columns [projects.kee, projects.name]");

    underTest.start();
  }

  private void answerSql(List<String[]> firstRequest, List<String[]>... otherRequests) throws SQLException {
    when(statementExecutor.executeQuery(any(Connection.class), anyString(), anyInt())).thenReturn(firstRequest, otherRequests);
  }
}
