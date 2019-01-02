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

public class PostgreSqlTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public LogTester logs = new LogTester();

  private PostgreSql underTest = new PostgreSql();

  @Test
  public void matchesJdbcURL() {
    assertThat(underTest.matchesJdbcUrl("jdbc:postgresql://localhost/sonar")).isTrue();
    assertThat(underTest.matchesJdbcUrl("jdbc:hsql:foo")).isFalse();
  }

  @Test
  public void should_set_connection_properties() {
    assertThat(underTest.getConnectionInitStatements()).isEqualTo(PostgreSql.INIT_STATEMENTS);
  }

  @Test
  public void testBooleanSqlValues() {
    assertThat(underTest.getTrueSqlValue()).isEqualTo("true");
    assertThat(underTest.getFalseSqlValue()).isEqualTo("false");
  }

  @Test
  public void should_configure() {
    assertThat(underTest.getId()).isEqualTo("postgresql");
    assertThat(underTest.getDefaultDriverClassName()).isEqualTo("org.postgresql.Driver");
    assertThat(underTest.getValidationQuery()).isEqualTo("SELECT 1");
  }

  @Test
  public void testFetchSizeForScrolling() {
    assertThat(underTest.getScrollDefaultFetchSize()).isEqualTo(200);
  }

  @Test
  public void postgres_does_supportMigration() {
    assertThat(underTest.supportsMigration()).isTrue();
  }

  @Test
  public void getSqlFromDual() {
    assertThat(underTest.getSqlFromDual()).isEqualTo("");
  }

  @Test
  public void postgresql_9_2_is_not_supported() throws Exception {
    expectedException.expect(MessageException.class);
    expectedException.expectMessage("Unsupported postgresql version: 9.2. Minimal supported version is 9.3.");

    DatabaseMetaData metadata = newMetadata( 9, 2);
    underTest.init(metadata);
  }

  @Test
  public void postgresql_9_3_is_supported_without_upsert() throws Exception {
    DatabaseMetaData metadata = newMetadata( 9, 3);
    underTest.init(metadata);

    assertThat(underTest.supportsUpsert()).isFalse();
    assertThat(logs.logs(LoggerLevel.WARN)).contains("Upgrading PostgreSQL to 9.5 or greater is recommended for better performances");
  }

  @Test
  public void postgresql_9_5_is_supported_with_upsert() throws Exception {
    DatabaseMetaData metadata = newMetadata( 9, 5);
    underTest.init(metadata);

    assertThat(underTest.supportsUpsert()).isTrue();
    assertThat(logs.logs(LoggerLevel.WARN)).isEmpty();
  }

  @Test
  public void init_throws_ISE_if_called_twice() throws Exception {
    DatabaseMetaData metaData = newMetadata(9, 5);
    underTest.init(metaData);

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("onInit() must be called once");

    underTest.init(metaData);
  }

  @Test
  public void supportsUpsert_throws_ISE_if_not_initialized() {
    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("onInit() must be called before calling supportsUpsert()");

    underTest.supportsUpsert();
  }

  private DatabaseMetaData newMetadata(int dbMajorVersion, int dbMinorVersion) throws SQLException {
    DatabaseMetaData metadata = mock(DatabaseMetaData.class, Mockito.RETURNS_DEEP_STUBS);
    when(metadata.getDatabaseMajorVersion()).thenReturn(dbMajorVersion);
    when(metadata.getDatabaseMinorVersion()).thenReturn(dbMinorVersion);
    return metadata;
  }

}
