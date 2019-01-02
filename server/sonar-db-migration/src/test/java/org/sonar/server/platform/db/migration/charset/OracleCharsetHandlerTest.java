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
import java.util.Collections;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;

import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
    answerCharset("UTF8");

    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
  }

  @Test
  public void upgrade_does_not_verify_utf8_charset() throws Exception {
    underTest.handle(connection, DatabaseCharsetChecker.State.UPGRADE);

    verifyZeroInteractions(sqlExecutor);
  }

  @Test
  public void fresh_install_supports_al32utf8() throws Exception {
    answerCharset("AL32UTF8");

    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
  }

  @Test
  public void fresh_install_fails_if_charset_is_not_utf8() throws Exception {
    answerCharset("LATIN");

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Oracle NLS_CHARACTERSET does not support UTF8: LATIN");

    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
  }

  @Test
  public void fails_if_can_not_get_charset() throws Exception {
    answerCharset(null);

    expectedException.expect(MessageException.class);

    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
  }

  @Test
  public void does_nothing_if_regular_startup() throws Exception {
    underTest.handle(connection, DatabaseCharsetChecker.State.STARTUP);
    verifyZeroInteractions(sqlExecutor);
  }

  private void answerCharset(@Nullable String charset) throws SQLException {
    when(sqlExecutor.select(any(Connection.class), anyString(), any(SqlExecutor.StringsConverter.class)))
      .thenReturn(charset == null ? Collections.emptyList() : singletonList(new String[] {charset}));
  }
}
