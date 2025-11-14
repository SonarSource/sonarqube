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
import javax.annotation.Nullable;
import org.junit.Test;
import org.sonar.api.utils.MessageException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class OracleCharsetHandlerTest {


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

    verifyNoInteractions(sqlExecutor);
  }

  @Test
  public void fresh_install_supports_al32utf8() throws Exception {
    answerCharset("AL32UTF8");

    underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL);
  }

  @Test
  public void fresh_install_fails_if_charset_is_not_utf8() throws Exception {
    answerCharset("LATIN");

    assertThatThrownBy(() -> underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL))
      .isInstanceOf(MessageException.class)
      .hasMessage("Oracle NLS_CHARACTERSET does not support UTF8: LATIN");
  }

  @Test
  public void fails_if_can_not_get_charset() throws Exception {
    answerCharset(null);

    assertThatThrownBy(() -> underTest.handle(connection, DatabaseCharsetChecker.State.FRESH_INSTALL))
      .isInstanceOf(MessageException.class);
  }

  @Test
  public void does_nothing_if_regular_startup() throws Exception {
    underTest.handle(connection, DatabaseCharsetChecker.State.STARTUP);
    verifyNoInteractions(sqlExecutor);
  }

  private void answerCharset(@Nullable String charset) throws SQLException {
    when(sqlExecutor.selectSingleString(any(Connection.class), anyString()))
      .thenReturn(charset);
  }
}
