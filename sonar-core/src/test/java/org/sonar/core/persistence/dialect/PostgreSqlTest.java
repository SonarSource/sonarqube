/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.persistence.dialect;

import org.fest.assertions.Condition;
import org.junit.Test;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class PostgreSqlTest {

  private PostgreSql postgreSql = new PostgreSql();

  @Test
  public void matchesJdbcURL() {
    assertThat(postgreSql.matchesJdbcURL("jdbc:postgresql://localhost/sonar")).isTrue();
    assertThat(postgreSql.matchesJdbcURL("jdbc:hsql:foo")).isFalse();
  }

  @Test
  public void should_avoid_conflict_with_other_schemas() {
    List<String> initStatements = postgreSql.getConnectionInitStatements("my_schema");

    assertThat(initStatements).contains("SET SEARCH_PATH TO my_schema");
  }

  @Test
  public void shouldNotChangePostgreSearchPathByDefault() {
    for (String statement : postgreSql.getConnectionInitStatements(null)) {
      assertThat(statement).doesNotMatch("SET SEARCH_PATH .*");
    }
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(postgreSql.getTrueSqlValue()).isEqualTo("true");
    assertThat(postgreSql.getFalseSqlValue()).isEqualTo("false");
  }

  @Test
  public void should_configure() {
    assertThat(postgreSql.getId()).isEqualTo("postgresql");
    assertThat(postgreSql.getActiveRecordDialectCode()).isEqualTo("postgre");
    assertThat(postgreSql.getDefaultDriverClassName()).isEqualTo("org.postgresql.Driver");
    assertThat(postgreSql.getValidationQuery()).isEqualTo("SELECT 1");
  }
}
