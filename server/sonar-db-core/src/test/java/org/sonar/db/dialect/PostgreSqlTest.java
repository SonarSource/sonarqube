/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.MessageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostgreSqlTest {

  @RegisterExtension
  public final LogTesterJUnit5 logs = new LogTesterJUnit5();

  private final PostgreSql underTest = new PostgreSql();

  @Test
  void matchesJdbcURL() {
    assertThat(underTest.matchesJdbcUrl("jdbc:postgresql://localhost/sonar")).isTrue();
    assertThat(underTest.matchesJdbcUrl("jdbc:hsql:foo")).isFalse();
  }

  @Test
  void should_set_connection_properties() {
    assertThat(underTest.getConnectionInitStatements()).isEqualTo(PostgreSql.INIT_STATEMENTS);
  }

  @Test
  void testBooleanSqlValues() {
    assertThat(underTest.getTrueSqlValue()).isEqualTo("true");
    assertThat(underTest.getFalseSqlValue()).isEqualTo("false");
  }

  @Test
  void should_configure() {
    assertThat(underTest.getId()).isEqualTo("postgresql");
    assertThat(underTest.getDefaultDriverClassName()).isEqualTo("org.postgresql.Driver");
    assertThat(underTest.getValidationQuery()).isEqualTo("SELECT 1");
  }

  @Test
  void testFetchSizeForScrolling() {
    assertThat(underTest.getScrollDefaultFetchSize()).isEqualTo(200);
  }

  @Test
  void postgres_does_supportMigration() {
    assertThat(underTest.supportsMigration()).isTrue();
  }

  @Test
  void postgres_does_supportUpsert() {
    assertThat(underTest.supportsUpsert()).isTrue();
  }

  @Test
  void getSqlFromDual() {
    assertThat(underTest.getSqlFromDual()).isEmpty();
  }

  @Test
  void postgresql_9_2_is_not_supported() {
    assertThatThrownBy(() -> {
      DatabaseMetaData metadata = newMetadata(9, 2);
      underTest.init(metadata);
    })
      .isInstanceOf(MessageException.class)
      .hasMessage("Unsupported postgresql version: 9.2. Minimal supported version is 11.0.");
  }

  @Test
  void postgresql_11_0_is_supported_with_upsert() throws Exception {
    DatabaseMetaData metadata = newMetadata(11, 0);
    underTest.init(metadata);

    assertThat(underTest.supportsUpsert()).isTrue();
    assertThat(logs.logs(Level.WARN)).isEmpty();
  }

  @Test
  void init_throws_ISE_if_called_twice() throws Exception {
    DatabaseMetaData metaData = newMetadata(11, 0);
    underTest.init(metaData);

    assertThatThrownBy(() -> underTest.init(metaData))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("onInit() must be called once");
  }

  @Test
  void supportsUpsert_returns_true_even_if_not_initialized() {
    assertTrue(underTest.supportsUpsert());
  }

  @Test
  void supportsNullNotDistinct_throws_ISE_if_not_initialized() {
    assertThatThrownBy(underTest::supportsNullNotDistinct)
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("onInit() must be called before calling supportsNullNotDistinct()");
  }

  @Test
  void supportsNullNotDistinct_shouldReturnTrue_WhenPostgres15OrGreater() throws SQLException {
    DatabaseMetaData metadata = newMetadata(15, 0);
    underTest.init(metadata);
    assertThat(underTest.supportsNullNotDistinct()).isTrue();
  }

  @Test
  void supportsNullNotDistinct_shouldReturnFalse_WhenPostgres14OrLesser() throws SQLException {
    DatabaseMetaData metadata = newMetadata(14, 0);
    underTest.init(metadata);
    assertThat(underTest.supportsNullNotDistinct()).isFalse();
  }

  private DatabaseMetaData newMetadata(int dbMajorVersion, int dbMinorVersion) throws SQLException {
    DatabaseMetaData metadata = mock(DatabaseMetaData.class, Mockito.RETURNS_DEEP_STUBS);
    when(metadata.getDatabaseMajorVersion()).thenReturn(dbMajorVersion);
    when(metadata.getDatabaseMinorVersion()).thenReturn(dbMinorVersion);
    return metadata;
  }

}
