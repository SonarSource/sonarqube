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

public class H2Test {

  H2 dialect = new H2();

  @Test
  public void matchesJdbcURL() {
    assertThat(dialect.matchesJdbcURL("jdbc:h2:foo")).isTrue();
    assertThat(dialect.matchesJdbcURL("jdbc:hsql:foo")).isFalse();
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(dialect.getTrueSqlValue()).isEqualTo("true");
    assertThat(dialect.getFalseSqlValue()).isEqualTo("false");
  }

  @Test
  public void should_configure() {
    assertThat(dialect.getId()).isEqualTo("h2");
    assertThat(dialect.getDefaultDriverClassName()).isEqualTo("org.h2.Driver");
    assertThat(dialect.getValidationQuery()).isEqualTo("SELECT 1");
  }

  @Test
  public void testFetchSizeForScrolling() {
    assertThat(dialect.getScrollDefaultFetchSize()).isEqualTo(200);
  }

  @Test
  public void h2_does_not_supportMigration() {
    assertThat(dialect.supportsMigration()).isFalse();
  }
}
