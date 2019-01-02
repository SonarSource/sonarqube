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
import java.sql.SQLException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MySqlTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public LogTester logs = new LogTester();

  private MySql underTest = new MySql();

  @Test
  public void matchesJdbcURL() {
    assertThat(underTest.matchesJdbcUrl("jdbc:mysql://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8")).isTrue();
    assertThat(underTest.matchesJdbcUrl("JDBC:MYSQL://localhost:3306/sonar?useUnicode=true&characterEncoding=utf8")).isTrue();

    assertThat(underTest.matchesJdbcUrl("jdbc:hsql:foo")).isFalse();
    assertThat(underTest.matchesJdbcUrl("jdbc:oracle:foo")).isFalse();
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

  @Test
  public void init_throws_MessageException_if_mysql_5_5() throws Exception {
    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Unsupported mysql version: 5.5. Minimal supported version is 5.6.");

    DatabaseMetaData metadata = newMetadata( 5, 5);
    underTest.init(metadata);
  }

  @Test
  public void init_does_not_fail_if_mysql_5_6() throws Exception {
    DatabaseMetaData metadata = newMetadata( 5, 6);
    underTest.init(metadata);
  }

  @Test
  public void init_logs_warning() throws SQLException {
    underTest.init(newMetadata(5, 6));

    assertThat(logs.logs(LoggerLevel.WARN)).contains("MySQL support is deprecated and will be dropped soon.");
  }

  @Test
  public void supportsUpsert_returns_false() {
    assertThat(underTest.supportsUpsert()).isFalse();
  }

  private DatabaseMetaData newMetadata(int dbMajorVersion, int dbMinorVersion) throws SQLException {
    DatabaseMetaData metadata = mock(DatabaseMetaData.class, Mockito.RETURNS_DEEP_STUBS);
    when(metadata.getDatabaseMajorVersion()).thenReturn(dbMajorVersion);
    when(metadata.getDatabaseMinorVersion()).thenReturn(dbMinorVersion);
    return metadata;
  }
}
