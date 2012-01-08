/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.persistence.dialect;

import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

public class PostgreSqlTest {

  private PostgreSql postgreSql = new PostgreSql();

  @Test
  public void matchesJdbcURL() {
    assertThat(postgreSql.matchesJdbcURL("jdbc:postgresql://localhost/sonar"), is(true));
    assertThat(postgreSql.matchesJdbcURL("jdbc:hsql:foo"), is(false));
  }

  /**
   * Avoid conflicts with other schemas
   */
  @Test
  public void shouldChangePostgreSearchPath() {
    String initStatement = postgreSql.getConnectionInitStatement("my_schema");

    assertThat(initStatement, Is.is("SET SEARCH_PATH TO my_schema"));
  }

  @Test
  public void shouldNotChangePostgreSearchPathByDefault() {
    assertNull(postgreSql.getConnectionInitStatement(null));
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(postgreSql.getTrueSqlValue(), is("true"));
    assertThat(postgreSql.getFalseSqlValue(), is("false"));
  }
}
