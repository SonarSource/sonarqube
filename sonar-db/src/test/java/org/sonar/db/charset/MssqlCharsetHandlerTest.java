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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MssqlCharsetHandlerTest {

  private static final String TABLE_ISSUES = "issues";
  private static final String TABLE_PROJECTS = "projects";
  private static final String COLUMN_KEE = "kee";
  private static final String COLUMN_NAME = "name";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  CharsetHandler.SelectExecutor selectExecutor = mock(CharsetHandler.SelectExecutor.class);
  MssqlCharsetHandler underTest = new MssqlCharsetHandler(selectExecutor);

  @Test
  public void checks_case_sensibility() throws Exception {
    answerSql(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "Latin1_General_CS_AS"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "Latin1_General_CS_AS"}));

    underTest.handle(mock(Connection.class), true);
  }

  @Test
  public void fails_if_case_insensitive() throws Exception {
    answerSql(asList(
      new String[] {TABLE_ISSUES, COLUMN_KEE, "Latin1_General_CS_AS"},
      new String[] {TABLE_PROJECTS, COLUMN_KEE, "Latin1_General_CI_AI"},
      new String[] {TABLE_PROJECTS, COLUMN_NAME, "Latin1_General_CI_AI"}));

    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Case-sensitive and accent-sensitive collation (CS_AS) is required for database columns [projects.kee, projects.name]");

    underTest.handle(mock(Connection.class), true);
  }

  private void answerSql(List<String[]> firstRequest, List<String[]>... otherRequests) throws SQLException {
    when(selectExecutor.executeQuery(any(Connection.class), anyString(), anyInt())).thenReturn(firstRequest, otherRequests);
  }
}
