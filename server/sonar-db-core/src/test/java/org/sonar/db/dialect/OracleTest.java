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

public class OracleTest {

  private Oracle underTest = new Oracle();

  @Test
  public void matchesJdbcURL() {
    assertThat(underTest.matchesJdbcURL("jdbc:oracle:thin:@localhost/XE")).isTrue();
    assertThat(underTest.matchesJdbcURL("jdbc:hsql:foo")).isFalse();
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(underTest.getTrueSqlValue()).isEqualTo("1");
    assertThat(underTest.getFalseSqlValue()).isEqualTo("0");
  }

  @Test
  public void should_configure() {
    assertThat(underTest.getId()).isEqualTo("oracle");
    assertThat(underTest.getDefaultDriverClassName()).isEqualTo("oracle.jdbc.OracleDriver");
    assertThat(underTest.getValidationQuery()).isEqualTo("SELECT 1 FROM DUAL");
  }

  @Test
  public void testFetchSizeForScrolling() {
    assertThat(underTest.getScrollDefaultFetchSize()).isEqualTo(200);
    assertThat(underTest.getScrollSingleRowFetchSize()).isEqualTo(1);
  }

  @Test
  public void oracle_does_supportMigration() {
    assertThat(underTest.supportsMigration()).isTrue();
  }
}
