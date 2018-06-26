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

  private MySql underTest = new MySql();

  @Test
  public void matchesJdbcURL() {
    assertThat(underTest.matchesJdbcURL("jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8")).isTrue();
    assertThat(underTest.matchesJdbcURL("JDBC:MYSQL://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8")).isTrue();

    assertThat(underTest.matchesJdbcURL("jdbc:hsql:foo")).isFalse();
    assertThat(underTest.matchesJdbcURL("jdbc:oracle:foo")).isFalse();
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(underTest.getTrueSqlValue()).isEqualTo("true");
    assertThat(underTest.getFalseSqlValue()).isEqualTo("false");
  }

  @Test
  public void should_configure() {
    assertThat(underTest.getId()).isEqualTo("mysql");
    assertThat(underTest.getDefaultDriverClassName()).isEqualTo("com.mysql.jdbc.Driver");
    assertThat(underTest.getValidationQuery()).isEqualTo("SELECT 1");
  }

  @Test
  public void testFetchSizeForScrolling() {
    assertThat(underTest.getScrollDefaultFetchSize()).isEqualTo(Integer.MIN_VALUE);
    assertThat(underTest.getScrollSingleRowFetchSize()).isEqualTo(Integer.MIN_VALUE);
  }

  @Test
  public void mysql_does_supportMigration() {
    assertThat(underTest.supportsMigration()).isTrue();
  }

  @Test
  public void getSqlFromDual() {
    assertThat(underTest.getSqlFromDual()).isEqualTo("from dual");
  }
}
