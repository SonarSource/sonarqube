/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class MySqlTest {

  private MySql mySql = new MySql();

  @Test
  public void matchesJdbcURL() {
    assertThat(mySql.matchesJdbcURL("jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8"), is(true));
    assertThat(mySql.matchesJdbcURL("JDBC:MYSQL://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8"), is(true));

    assertThat(mySql.matchesJdbcURL("jdbc:hsql:foo"), is(false));
    assertThat(mySql.matchesJdbcURL("jdbc:oracle:foo"), is(false));
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(mySql.getTrueSqlValue(), is("true"));
    assertThat(mySql.getFalseSqlValue(), is("false"));
  }
}

