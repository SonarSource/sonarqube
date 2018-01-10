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
package org.sonar.db.dialect;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MySqlTest {

  private MySql mySql = new MySql();

  @Test
  public void matchesJdbcURL() {
    assertThat(mySql.matchesJdbcURL("jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8")).isTrue();
    assertThat(mySql.matchesJdbcURL("JDBC:MYSQL://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8")).isTrue();

    assertThat(mySql.matchesJdbcURL("jdbc:hsql:foo")).isFalse();
    assertThat(mySql.matchesJdbcURL("jdbc:oracle:foo")).isFalse();
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(mySql.getTrueSqlValue()).isEqualTo("true");
    assertThat(mySql.getFalseSqlValue()).isEqualTo("false");
  }

  @Test
  public void should_configure() {
    assertThat(mySql.getId()).isEqualTo("mysql");
    assertThat(mySql.getDefaultDriverClassName()).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(mySql.getValidationQuery()).isEqualTo("SELECT 1");
  }

  @Test
  public void testFetchSizeForScrolling() {
    assertThat(mySql.getScrollDefaultFetchSize()).isEqualTo(Integer.MIN_VALUE);
    assertThat(mySql.getScrollSingleRowFetchSize()).isEqualTo(Integer.MIN_VALUE);
  }

  @Test
  public void mysql_does_supportMigration() {
    assertThat(mySql.supportsMigration()).isTrue();
  }
}
