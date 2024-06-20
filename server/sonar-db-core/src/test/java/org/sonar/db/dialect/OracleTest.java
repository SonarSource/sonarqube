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
import org.mockito.Mockito;
import org.sonar.api.utils.MessageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OracleTest {

  private final Oracle underTest = new Oracle();

  @Test
  void matchesJdbcURL() {
    assertThat(underTest.matchesJdbcUrl("jdbc:oracle:thin:@localhost/XE")).isTrue();
    assertThat(underTest.matchesJdbcUrl("jdbc:hsql:foo")).isFalse();
  }

  @Test
  void testBooleanSqlValues() {
    assertThat(underTest.getTrueSqlValue()).isEqualTo("1");
    assertThat(underTest.getFalseSqlValue()).isEqualTo("0");
  }

  @Test
  void should_configure() {
    assertThat(underTest.getId()).isEqualTo("oracle");
    assertThat(underTest.getDefaultDriverClassName()).isEqualTo("oracle.jdbc.OracleDriver");
    assertThat(underTest.getValidationQuery()).isEqualTo("SELECT 1 FROM DUAL");
  }

  @Test
  void testFetchSizeForScrolling() {
    assertThat(underTest.getScrollDefaultFetchSize()).isEqualTo(200);
  }

  @Test
  void oracle_does_supportMigration() {
    assertThat(underTest.supportsMigration()).isTrue();
  }

  @Test
  void getSqlFromDual() {
    assertThat(underTest.getSqlFromDual()).isEqualTo("from dual");
  }

  @Test
  void test_db_versions() throws Exception {
    // oracle 11.0 is ok
    DatabaseMetaData metadata = newMetadata( 19, 0, "12.1.0.1.0");
    underTest.init(metadata);

    // oracle 11.1 is ok
    metadata = newMetadata(19, 1, "12.1.0.1.0");
    underTest.init(metadata);

    // oracle 11.2 is ok
    metadata = newMetadata(19, 2, "12.1.0.1.0");
    underTest.init(metadata);

    // oracle 21 is ok
    metadata = newMetadata(21, 1, "12.1.0.1.0");
    underTest.init(metadata);

    // oracle 18 is not supported
    metadata = newMetadata(18, 17, "12.1.0.1.0");
    try {
      underTest.init(metadata);
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Unsupported oracle version: 18.17. Minimal supported version is 19.0.");
    }
  }
  
  @Test
  void test_driver_versions() throws Exception {
    DatabaseMetaData metadata = newMetadata( 19, 2, "18.3.0.0.0");
    underTest.init(metadata);

    metadata = newMetadata(19, 2, "12.2.0.1.0");
    underTest.init(metadata);
    // no error

    metadata = newMetadata(19, 2, "12.1.0.2.0");
    underTest.init(metadata);
    // no error

    metadata = newMetadata(19, 2, "12.1.0.1.0");
    underTest.init(metadata);
    // no error

    metadata = newMetadata(19, 2, "12.0.2");
    underTest.init(metadata);
    // no error

    metadata = newMetadata(19, 2, "11.1.0.2");
    try {
      underTest.init(metadata);
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Unsupported Oracle driver version: 11.1.0.2. Minimal supported version is 12.1.");
    }
  }

  @Test
  void supportsUpsert_returns_false() {
    assertThat(underTest.supportsUpsert()).isFalse();
  }

  private DatabaseMetaData newMetadata(int dbMajorVersion, int dbMinorVersion, String driverVersion) throws SQLException {
    DatabaseMetaData metadata = mock(DatabaseMetaData.class, Mockito.RETURNS_DEEP_STUBS);
    when(metadata.getDatabaseMajorVersion()).thenReturn(dbMajorVersion);
    when(metadata.getDatabaseMinorVersion()).thenReturn(dbMinorVersion);
    when(metadata.getDriverVersion()).thenReturn(driverVersion);
    return metadata;
  }
}
