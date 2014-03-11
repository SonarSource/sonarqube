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
package org.sonar.core.persistence.dialect;

import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

public class H2Test {

  private H2 h2 = new H2();

  @Test
  public void matchesJdbcURL() {
    assertThat(h2.matchesJdbcURL("jdbc:h2:foo")).isTrue();
    assertThat(h2.matchesJdbcURL("jdbc:hsql:foo")).isFalse();
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(h2.getTrueSqlValue()).isEqualTo("true");
    assertThat(h2.getFalseSqlValue()).isEqualTo("false");
  }

  @Test
  public void should_configure() {
    assertThat(h2.getId()).isEqualTo("h2");
    assertThat(h2.getActiveRecordDialectCode()).isEqualTo(".h2.");
    assertThat(h2.getDefaultDriverClassName()).isEqualTo("org.h2.Driver");
    assertThat(h2.getValidationQuery()).isEqualTo("SELECT 1");
  }
}
