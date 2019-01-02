/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.sql.DatabaseMetaData;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class H2Test {

  @Rule
  public LogTester logs = new LogTester();

  private H2 underTest = new H2();

  @Test
  public void matchesJdbcURL() {
    assertThat(underTest.matchesJdbcUrl("jdbc:h2:foo")).isTrue();
    assertThat(underTest.matchesJdbcUrl("jdbc:hsql:foo")).isFalse();
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(underTest.getTrueSqlValue()).isEqualTo("true");
    assertThat(underTest.getFalseSqlValue()).isEqualTo("false");
  }

  @Test
  public void should_configure() {
    assertThat(underTest.getId()).isEqualTo("h2");
    assertThat(underTest.getDefaultDriverClassName()).isEqualTo("org.h2.Driver");
    assertThat(underTest.getValidationQuery()).isEqualTo("SELECT 1");
  }

  @Test
  public void testFetchSizeForScrolling() {
    assertThat(underTest.getScrollDefaultFetchSize()).isEqualTo(200);
  }

  @Test
  public void h2_does_not_supportMigration() {
    assertThat(underTest.supportsMigration()).isFalse();
  }

  @Test
  public void getSqlFromDual() {
    assertThat(underTest.getSqlFromDual()).isEqualTo("");
  }

  @Test
  public void init_logs_warning() {
    underTest.init(mock(DatabaseMetaData.class));

    assertThat(logs.logs(LoggerLevel.WARN)).contains("H2 database should be used for evaluation purpose only.");
  }

  @Test
  public void supportsUpsert_returns_false() {
    assertThat(underTest.supportsUpsert()).isFalse();
  }
}
