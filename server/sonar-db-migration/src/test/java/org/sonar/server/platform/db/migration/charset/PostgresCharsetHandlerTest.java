/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import java.util.Arrays;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;

import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class PostgresCharsetHandlerTest {

  private static final String TABLE_ISSUES = "issues";
  private static final String TABLE_PROJECTS = "projects";
  private static final String COLUMN_KEE = "kee";
  private static final String COLUMN_NAME = "name";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SqlExecutor sqlExecutor = mock(SqlExecutor.class);
  private Connection connection = mock(Connection.class);
  private PostgresMetadataReader metadata = mock(PostgresMetadataReader.class);
  private PostgresCharsetHandler underTest = new PostgresCharsetHandler(sqlExecutor, metadata);

  @Test
  public void fresh_install_verifies_that_default_charset_is_utf8() throws SQLException {
    answerDefaultCharset("utf8");

    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
    // no errors, charset has been verified
    verify(metadata).getDefaultCharset(same(connection));
    verifyZeroInteractions(sqlExecutor);
  }

  @Test
  public void upgrade_verifies_that_default_charset_and_columns_are_utf8() throws Exception {
    answerDefaultCharset("utf8");
    answerColumns(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "utf8"}));

    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);
    // no errors, charsets have been verified
    verify(metadata).getDefaultCharset(same(connection));
  }

  @Test
  public void regular_startup_verifies_that_default_charset_and_columns_are_utf8() throws Exception {
    answerDefaultCharset("utf8");
    answerColumns(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "utf8"}));

    underTest.handle(connection, DatabaseCharsetChecker.State.STARTUP);
    // no errors, charsets have been verified
    verify(metadata).getDefaultCharset(same(connection));
  }

  @Test
  public void column_charset_can_be_empty() throws Exception {
    answerDefaultCharset("utf8");
    answerColumns(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "" /* unset -> uses db collation */}));

    // no error
    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);
  }

  @Test
  public void upgrade_fails_if_non_utf8_column() throws Exception {
    // default charset is ok but two columns are not
    answerDefaultCharset("utf8");
    answerColumns(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
      new String[] {TABLE_PROJECTS, COLUMN_KEE, "latin"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "latin"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Database columns [projects.kee, projects.name] must have UTF8 charset.");

    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);
  }

  @Test
  public void upgrade_fails_if_default_charset_is_not_utf8() throws Exception {
    answerDefaultCharset("latin");
    answerColumns(
      Arrays.<String[]>asList(new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Database charset is latin. It must support UTF8.");

    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);
  }

  private void answerDefaultCharset(String defaultCollation) throws SQLException {
    when(metadata.getDefaultCharset(same(connection))).thenReturn(defaultCollation);
  }

  private void answerColumns(List<String[]> firstRequest) throws SQLException {
    when(sqlExecutor.select(same(connection), anyString(), any(SqlExecutor.StringsConverter.class))).thenReturn(firstRequest);
  }
}
