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
package org.sonar.server.platform.db.migration.charset;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;

import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class OracleCharsetHandlerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private SqlExecutor sqlExecutor = mock(SqlExecutor.class);
  private Connection connection = mock(Connection.class);
  private OracleCharsetHandler underTest = new OracleCharsetHandler(sqlExecutor);

  @Test
  public void fresh_install_verifies_utf8_charset() throws Exception {
    answerSql(singletonList(new String[] {"UTF8"}), singletonList(new String[] {"BINARY"}));

    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
  }

  @Test
  public void upgrade_does_not_verify_utf8_charset() throws Exception {
    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);

    verifyZeroInteractions(sqlExecutor);
  }

  @Test
  public void fresh_install_supports_al32utf8() throws Exception {
    answerSql(
      singletonList(new String[] {"AL32UTF8"}), singletonList(new String[] {"BINARY"}));

    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
  }

  @Test
  public void fresh_install_fails_if_charset_is_not_utf8() throws Exception {
    answerSql(
      singletonList(new String[] {"LATIN"}), singletonList(new String[] {"BINARY"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Oracle must be have UTF8 charset and BINARY sort. NLS_CHARACTERSET is LATIN and NLS_SORT is BINARY.");

    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
  }

  @Test
  public void fresh_install_fails_if_not_case_sensitive() throws Exception {
    answerSql(
      singletonList(new String[] {"UTF8"}), singletonList(new String[] {"LINGUISTIC"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Oracle must be have UTF8 charset and BINARY sort. NLS_CHARACTERSET is UTF8 and NLS_SORT is LINGUISTIC.");

    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
  }

  @Test
  public void fails_if_can_not_get_charset() throws Exception {
    answerSql(Collections.emptyList(), Collections.emptyList());

    expectedException.expect(MessageException.class);

    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
  }

  @Test
  public void does_nothing_if_regular_startup() throws Exception {
    underTest.handle(connection, DatabaseCharsetChecker.State.STARTUP);
    verifyZeroInteractions(sqlExecutor);
  }

  private void answerSql(List<String[]> firstRequest, List<String[]>... otherRequests) throws SQLException {
    when(sqlExecutor.select(any(Connection.class), anyString(), any(SqlExecutor.StringsConverter.class))).thenReturn(firstRequest, otherRequests);
  }
}
