/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.db.migrations;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.dialect.Dialect;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MassUpdaterTest {

  @Mock
  Database db;

  @Test
  public void convert_select_sql() throws Exception {
    Dialect dialect = mock(Dialect.class);
    when(dialect.getTrueSqlValue()).thenReturn("true");
    when(dialect.getFalseSqlValue()).thenReturn("false");

    when(db.getDialect()).thenReturn(dialect);

    String result = MassUpdater.convertSelectSql("SELECT * FROM projects WHERE enabled=${_true} AND used=${_true} AND deleted=${_false}", db);
    assertThat(result).isEqualTo("SELECT * FROM projects WHERE enabled=true AND used=true AND deleted=false");
  }
}
