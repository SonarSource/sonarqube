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
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PostgresCharsetHandlerTest {

  private static final String TABLE_ISSUES = "issues";
  private static final String TABLE_PROJECTS = "projects";
  private static final String COLUMN_KEE = "kee";
  private static final String COLUMN_NAME = "name";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  SqlExecutor selectExecutor = mock(SqlExecutor.class);
  PostgresCharsetHandler underTest = new PostgresCharsetHandler(selectExecutor);

  @Test
  public void checks_that_column_is_utf8() throws Exception {
    answerSql(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "utf8"}));

    underTest.handle(mock(Connection.class), true);
  }

  @Test
  public void checks_that_db_is_utf8_if_column_collation_is_not_defined() throws Exception {
    answerSql(
      // first request to get columns
      asList(
        new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
        new String[] {TABLE_PROJECTS, COLUMN_NAME, "" /* unset -> uses db collation */}),

      // second request to get db collation
      Arrays.<String[]>asList(new String[] {"utf8"}));

    // no error
    underTest.handle(mock(Connection.class), true);
  }

  @Test
  public void fails_if_non_utf8_column() throws Exception {
    answerSql(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
      new String[] {TABLE_PROJECTS, COLUMN_KEE, "latin"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "latin"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Database columns [projects.kee, projects.name] must support UTF8 collation.");

    underTest.handle(mock(Connection.class), true);
  }

  @Test
  public void fails_if_non_utf8_db() throws Exception {
    answerSql(
      // first request to get columns
      asList(
        new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
        new String[] {TABLE_PROJECTS, COLUMN_NAME, "" /* unset -> uses db collation */}),

      // second request to get db collation
      Arrays.<String[]>asList(new String[] {"latin"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Database collation is latin. It must support UTF8.");

    underTest.handle(mock(Connection.class), true);
  }

  @Test
  public void does_nothing_if_utf8_must_not_verified() throws Exception {
    underTest.handle(mock(Connection.class), false);
  }

  private void answerSql(List<String[]> firstRequest, List<String[]>... otherRequests) throws SQLException {
    when(selectExecutor.executeSelect(any(Connection.class), anyString(), any(SqlExecutor.StringsConverter.class))).thenReturn(firstRequest, otherRequests);
  }
}
