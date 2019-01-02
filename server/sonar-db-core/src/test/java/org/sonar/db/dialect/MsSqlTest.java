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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MsSqlTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MsSql underTest = new MsSql();

  @Test
  public void matchesJdbcURL() {
    assertThat(underTest.matchesJdbcUrl("jdbc:sqlserver://localhost:1433;databasename=sonar")).isTrue();

    assertThat(underTest.matchesJdbcUrl("jdbc:hsql:foo")).isFalse();
    assertThat(underTest.matchesJdbcUrl("jdbc:mysql:foo")).isFalse();
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(underTest.getTrueSqlValue()).isEqualTo("1");
    assertThat(underTest.getFalseSqlValue()).isEqualTo("0");
  }

  @Test
  public void should_configure() {
    assertThat(underTest.getId()).isEqualTo("mssql");
    assertThat(underTest.getDefaultDriverClassName()).isEqualTo("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    assertThat(underTest.getValidationQuery()).isEqualTo("SELECT 1");
  }

  @Test
  public void do_not_support_jtds_since_5_2() {
    assertThat(underTest.matchesJdbcUrl("jdbc:jtds:sqlserver://localhost;databaseName=SONAR;SelectMethod=Cursor")).isFalse();

  }

  @Test
  public void msSql_does_supportMigration() {
    assertThat(underTest.supportsMigration()).isTrue();
  }

  @Test
  public void getSqlFromDual() {
    assertThat(underTest.getSqlFromDual()).isEqualTo("");
  }

  @Test
  public void init_throws_MessageException_if_mssql_2012() throws Exception {
    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Unsupported mssql version: 11.0. Minimal supported version is 12.0.");

    DatabaseMetaData metadata = newMetadata( 11, 0);
    underTest.init(metadata);
  }

  @Test
  public void init_does_not_fail_if_mssql_2014() throws Exception {
    DatabaseMetaData metadata = newMetadata( 12, 0);
    underTest.init(metadata);
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
