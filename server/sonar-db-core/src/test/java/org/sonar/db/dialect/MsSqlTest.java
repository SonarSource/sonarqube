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

public class MsSqlTest {

  private MsSql msSql = new MsSql();

  @Test
  public void matchesJdbcURL() {
    assertThat(msSql.matchesJdbcURL("jdbc:sqlserver://localhost:1433;databasename=sonar")).isTrue();

    assertThat(msSql.matchesJdbcURL("jdbc:hsql:foo")).isFalse();
    assertThat(msSql.matchesJdbcURL("jdbc:mysql:foo")).isFalse();
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(msSql.getTrueSqlValue()).isEqualTo("1");
    assertThat(msSql.getFalseSqlValue()).isEqualTo("0");
  }

  @Test
  public void should_configure() {
    assertThat(msSql.getId()).isEqualTo("mssql");
    assertThat(msSql.getDefaultDriverClassName()).isEqualTo("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    assertThat(msSql.getValidationQuery()).isEqualTo("SELECT 1");
  }

  @Test
  public void do_not_support_jtds_since_5_2() {
    assertThat(msSql.matchesJdbcURL("jdbc:jtds:sqlserver://localhost;databaseName=SONAR;SelectMethod=Cursor")).isFalse();

  }

  @Test
  public void msSql_does_supportMigration() {
    assertThat(msSql.supportsMigration()).isTrue();
  }
}
