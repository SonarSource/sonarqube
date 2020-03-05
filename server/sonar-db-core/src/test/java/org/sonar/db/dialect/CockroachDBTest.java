/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CockroachDBTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Rule
    public LogTester logs = new LogTester();

    private CockroachDB underTest = new CockroachDB();

    @Test
    public void testMatchesJdbcURL() {
        assertThat(underTest.matchesJdbcUrl("jdbc:cockroachdb://localhost/sonar")).isTrue();
        assertThat(underTest.matchesJdbcUrl("jdbc:postgresql:foo")).isFalse();
    }

    @Test
    public void testShouldSetConnectionProperties() {
        assertThat(underTest.getConnectionInitStatements()).isEqualTo(CockroachDB.INIT_STATEMENTS);
    }

    @Test
    public void testBooleanSqlValues() {
        assertThat(underTest.getTrueSqlValue()).isEqualTo("true");
        assertThat(underTest.getFalseSqlValue()).isEqualTo("false");
    }

    @Test
    public void testShouldConfigure() {
        assertThat(underTest.getId()).isEqualTo("cockroachdb");
        assertThat(underTest.getDefaultDriverClassName()).isEqualTo("org.postgresql.Driver");
        assertThat(underTest.getValidationQuery()).isEqualTo("SELECT 1");
    }

    @Test
    public void testFetchSizeForScrolling() {
        assertThat(underTest.getScrollDefaultFetchSize()).isEqualTo(200);
    }

    @Test
    public void testCockroachDBDoesSupportMigration() {
        assertThat(underTest.supportsMigration()).isTrue();
    }

    @Test
    public void testGetSqlFromDual() {
        assertThat(underTest.getSqlFromDual()).isEqualTo("");
    }

    @Test
    public void testCockroachDB21IsNotSupported() throws Exception {
        expectedException.expect(MessageException.class);
        expectedException.expectMessage("Unsupported cockroachdb version: 2.1. Minimal supported version is 19.1.");

        DatabaseMetaData metadata = newMetadata(2, 1);
        underTest.init(metadata);
    }

    @Test
    public void testCockroach191IsSupportedWithUpsert() throws Exception {
        DatabaseMetaData metadata = newMetadata(19, 1);
        underTest.init(metadata);

        assertThat(underTest.supportsUpsert()).isTrue();
    }

    @Test
    public void testInitThrowsISEIfCalledTwice() throws Exception {
        DatabaseMetaData metaData = newMetadata(19, 1);
        underTest.init(metaData);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("onInit() must be called once");

        underTest.init(metaData);
    }

    @Test
    public void testSupportsUpsertThrowsISEIfNotInitialized() {
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
