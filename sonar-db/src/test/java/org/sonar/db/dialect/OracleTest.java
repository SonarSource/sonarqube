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
package org.sonar.db.dialect;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OracleTest {

  Oracle dialect = new Oracle();

  @Test
  public void matchesJdbcURL() {
    assertThat(dialect.matchesJdbcURL("jdbc:oracle:thin:@localhost/XE")).isTrue();
    assertThat(dialect.matchesJdbcURL("jdbc:hsql:foo")).isFalse();
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(dialect.getTrueSqlValue()).isEqualTo("1");
    assertThat(dialect.getFalseSqlValue()).isEqualTo("0");
  }

  @Test
  public void should_configure() {
    assertThat(dialect.getId()).isEqualTo("oracle");
    assertThat(dialect.getActiveRecordDialectCode()).isEqualTo("oracle");
    assertThat(dialect.getDefaultDriverClassName()).isEqualTo("oracle.jdbc.OracleDriver");
    assertThat(dialect.getValidationQuery()).isEqualTo("SELECT 1 FROM DUAL");
  }

  @Test
  public void testFetchSizeForScrolling() throws Exception {
    assertThat(dialect.getScrollDefaultFetchSize()).isEqualTo(200);
    assertThat(dialect.getScrollSingleRowFetchSize()).isEqualTo(1);
  }

  @Test
  public void oracle_does_supportMigration() {
    assertThat(dialect.supportsMigration()).isTrue();
  }
}
