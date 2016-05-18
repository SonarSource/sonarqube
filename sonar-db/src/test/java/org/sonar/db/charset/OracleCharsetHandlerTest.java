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
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.MessageException;
import org.sonar.db.charset.DatabaseCharsetChecker.Flag;

import static com.google.common.collect.Sets.immutableEnumSet;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.charset.DatabaseCharsetChecker.Flag.ENFORCE_UTF8;

public class OracleCharsetHandlerTest {

  private static final Set<Flag> ENFORCE_UTF8_FLAGS = immutableEnumSet(ENFORCE_UTF8);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  SqlExecutor selectExecutor = mock(SqlExecutor.class);
  OracleCharsetHandler underTest = new OracleCharsetHandler(selectExecutor);

  @Test
  public void checks_utf8() throws Exception {
    answerSql(
      singletonList(new String[] {"UTF8"}), singletonList(new String[] {"BINARY"}));

    underTest.handle(mock(Connection.class), ENFORCE_UTF8_FLAGS);
  }

  @Test
  public void supports_al32utf8() throws Exception {
    answerSql(
      singletonList(new String[] {"AL32UTF8"}), singletonList(new String[] {"BINARY"}));

    underTest.handle(mock(Connection.class), ENFORCE_UTF8_FLAGS);
  }

  @Test
  public void fails_if_charset_is_not_utf8() throws Exception {
    answerSql(
      singletonList(new String[] {"LATIN"}), singletonList(new String[] {"BINARY"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Oracle must be have UTF8 charset and BINARY sort. NLS_CHARACTERSET is LATIN and NLS_SORT is BINARY.");

    underTest.handle(mock(Connection.class), ENFORCE_UTF8_FLAGS);
  }

  @Test
  public void fails_if_not_case_sensitive() throws Exception {
    answerSql(
      singletonList(new String[] {"UTF8"}), singletonList(new String[] {"LINGUISTIC"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Oracle must be have UTF8 charset and BINARY sort. NLS_CHARACTERSET is UTF8 and NLS_SORT is LINGUISTIC.");

    underTest.handle(mock(Connection.class), ENFORCE_UTF8_FLAGS);
  }

  @Test
  public void fails_if_can_not_get_charset() throws Exception {
    answerSql(Collections.<String[]>emptyList(), Collections.<String[]>emptyList());

    expectedException.expect(MessageException.class);

    underTest.handle(mock(Connection.class), ENFORCE_UTF8_FLAGS);
  }

  @Test
  public void does_nothing_if_utf8_must_not_verified() throws Exception {
    underTest.handle(mock(Connection.class), Collections.<Flag>emptySet());
  }

  private void answerSql(List<String[]> firstRequest, List<String[]>... otherRequests) throws SQLException {
    when(selectExecutor.executeSelect(any(Connection.class), anyString(), any(SqlExecutor.StringsConverter.class))).thenReturn(firstRequest, otherRequests);
  }
}
