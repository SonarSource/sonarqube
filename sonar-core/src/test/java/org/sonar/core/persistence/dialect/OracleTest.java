/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.persistence.dialect;

import org.junit.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class OracleTest {

  private Oracle oracle = new Oracle();

  @Test
  public void matchesJdbcURL() {
    assertThat(oracle.matchesJdbcURL("jdbc:oracle:thin:@localhost/XE")).isTrue();
    assertThat(oracle.matchesJdbcURL("jdbc:hsql:foo")).isFalse();
  }

  /**
   * Avoid conflicts with other schemas
   */
  @Test
  public void shouldChangeOracleSchema() {
    List<String> initStatements = oracle.getConnectionInitStatements("my_schema");

    assertThat(initStatements).containsExactly("ALTER SESSION SET CURRENT_SCHEMA = \"my_schema\"");
  }

  @Test
  public void shouldNotChangeOracleSchemaByDefault() {
    assertThat(oracle.getConnectionInitStatements(null)).isEmpty();
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(oracle.getTrueSqlValue()).isEqualTo("1");
    assertThat(oracle.getFalseSqlValue()).isEqualTo("0");
  }

  @Test
  public void should_configure() {
    assertThat(oracle.getId()).isEqualTo("oracle");
    assertThat(oracle.getActiveRecordDialectCode()).isEqualTo("oracle");
    assertThat(oracle.getDefaultDriverClassName()).isEqualTo("oracle.jdbc.OracleDriver");
    assertThat(oracle.getValidationQuery()).isEqualTo("SELECT 1 FROM DUAL");
  }
}
