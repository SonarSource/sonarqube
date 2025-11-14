/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.api.utils.MessageException;
import org.sonar.db.version.SqTables;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class PostgresCharsetHandlerTest {

  private static final String TABLE_ISSUES = "issues";
  private static final String TABLE_PROJECTS = "projects";
  private static final String COLUMN_KEE = "kee";
  private static final String COLUMN_NAME = "name";

  private final SqlExecutor sqlExecutor = mock(SqlExecutor.class);
  private final Connection connection = mock(Connection.class);
  private final PostgresMetadataReader metadata = mock(PostgresMetadataReader.class);
  private final PostgresCharsetHandler underTest = new PostgresCharsetHandler(sqlExecutor, metadata);

  @Test
  public void fresh_install_verifies_that_default_charset_is_utf8() throws SQLException {
    answerDefaultCharset("utf8");

    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
    // no errors, charset has been verified
    verify(metadata).getDefaultCharset(same(connection));
    verifyNoInteractions(sqlExecutor);
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
    assertThatCode(() -> underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE))
      .doesNotThrowAnyException();

    verify(sqlExecutor).select(same(connection), eq("select table_name, column_name,"
      + " collation_name "
      + "from information_schema.columns "
      + "where table_schema='public' "
      + "and table_name in (" + SqTables.TABLES.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")) + ") "
      + "and udt_name='varchar' order by table_name, column_name"), any(SqlExecutor.StringsConverter.class));
  }

  @Test
  public void schema_is_taken_into_account_when_selecting_columns() throws Exception {
    answerDefaultCharset("utf8");
    answerSchema("test-schema");
    answerColumns(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "" /* unset -> uses db collation */}));

    // no error
    assertThatCode(() -> underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE))
      .doesNotThrowAnyException();

    verify(sqlExecutor).select(same(connection), eq("select table_name, column_name,"
      + " collation_name "
      + "from information_schema.columns "
      + "where table_schema='test-schema' "
      + "and table_name in (" + SqTables.TABLES.stream().map(s -> "'" + s + "'").collect(Collectors.joining(",")) + ") "
      + "and udt_name='varchar' order by table_name, column_name"), any(SqlExecutor.StringsConverter.class));
  }

  @Test
  public void upgrade_fails_if_non_utf8_column() throws Exception {
    // default charset is ok but two columns are not
    answerDefaultCharset("utf8");
    answerColumns(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"},
      new String[] {TABLE_PROJECTS, COLUMN_KEE, "latin"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "latin"}));

    assertThatThrownBy(() -> underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE))
      .isInstanceOf(MessageException.class)
      .hasMessage("Database columns [projects.kee, projects.name] must have UTF8 charset.");
  }

  @Test
  public void upgrade_fails_if_default_charset_is_not_utf8() throws Exception {
    answerDefaultCharset("latin");
    answerColumns(
      List.<String[]>of(new String[] {TABLE_ISSUES, COLUMN_KEE, "utf8"}));

    assertThatThrownBy(() -> underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE))
      .isInstanceOf(MessageException.class)
      .hasMessage("Database charset is latin. It must support UTF8.");
  }

  private void answerDefaultCharset(String defaultCollation) throws SQLException {
    when(metadata.getDefaultCharset(same(connection))).thenReturn(defaultCollation);
  }

  private void answerSchema(String schema) throws SQLException {
    when(connection.getSchema()).thenReturn(schema);
  }

  private void answerColumns(List<String[]> firstRequest) throws SQLException {
    when(sqlExecutor.select(same(connection), anyString(), any(SqlExecutor.StringsConverter.class))).thenReturn(firstRequest);
  }
}
