/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.db.scannercache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.commons.io.IOUtils;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbInputStream;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.dialect.PostgreSql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScannerAnalysisCacheDaoIT {
  @RegisterExtension
  private final DbTester dbTester = DbTester.create(System2.INSTANCE);
  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();
  private static final UuidFactory uuidFactory = new SequenceUuidFactory();
  private final DbSession dbSession = dbTester.getSession();
  private final ScannerAnalysisCacheDao underTest = dbTester.getDbClient().scannerAnalysisCacheDao();
  private final ScannerAnalysisCacheDao slicedDao = new ScannerAnalysisCacheDao(3, 0, 10);

  @Test
  void insert_should_insert_in_db() throws IOException {
    underTest.insert(dbSession, "branch1", stringToInputStream("test data"));
    dbSession.commit();
    assertThat(dbTester.countRowsOfTable("scanner_analysis_cache")).isOne();
    assertThat(dataStreamToString(underTest.selectData(dbSession, "branch1"))).isEqualTo("test data");
  }

  @Test
  void select_returns_empty_if_entry_doesnt_exist() {
    underTest.insert(dbSession, "branch1", stringToInputStream("test data"));
    assertThat(underTest.selectData(dbSession, "branch2")).isNull();
  }

  @Test
  void remove_all_should_delete_all() {
    underTest.insert(dbSession, "branch1", stringToInputStream("test data"));
    underTest.insert(dbSession, "branch2", stringToInputStream("test data"));

    assertThat(dbTester.countRowsOfTable("scanner_analysis_cache")).isEqualTo(2);
    underTest.removeAll(dbSession);
    dbSession.commit();
    assertThat(dbTester.countRowsOfTable("scanner_analysis_cache")).isZero();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = PostgreSql.ID)
  void throw_illegalstateexception_when_sql_exception(String databaseId) throws SQLException {
    var mockDbSession = mock(DbSession.class);
    var connection = mock(Connection.class);
    var configuration = new Configuration();
    configuration.setDatabaseId(databaseId);
    when(mockDbSession.getConnection()).thenReturn(connection);
    when(mockDbSession.getConfiguration()).thenReturn(configuration);
    when(connection.prepareStatement(anyString())).thenThrow(new SQLException());

    assertThatThrownBy(() -> underTest.selectData(mockDbSession, "uuid"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Fail to select cache for branch uuid")
      .hasCauseInstanceOf(SQLException.class);

    assertThatThrownBy(() -> underTest.insert(mockDbSession, "uuid", mock(InputStream.class)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Fail to insert cache for branch uuid")
      .hasCauseInstanceOf(SQLException.class);
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 3, 6, 7, 8})
  void select_on_postgresql_reads_value_in_slices(int length) throws IOException {
    assumeTrue(isPostgreSql());
    String value = "abcdefgh".substring(0, length);
    slicedDao.insert(dbSession, "branch1", stringToInputStream(value));

    assertThat(dataStreamToString(slicedDao.selectData(dbSession, "branch1"))).isEqualTo(value);
  }

  @Test
  void select_on_postgresql_returns_null_if_entry_doesnt_exist() {
    assumeTrue(isPostgreSql());
    slicedDao.insert(dbSession, "branch1", stringToInputStream("test data"));

    assertThat(slicedDao.selectData(dbSession, "branch2")).isNull();
  }

  @Test
  void select_on_postgresql_supports_reads_crossing_slices() throws IOException {
    assumeTrue(isPostgreSql());
    slicedDao.insert(dbSession, "branch1", stringToInputStream("abcdefgh"));

    try (DbInputStream data = slicedDao.selectData(dbSession, "branch1")) {
      byte[] buffer = new byte[2];
      StringBuilder result = new StringBuilder();
      int count;
      while ((count = data.read(buffer, 0, buffer.length)) != -1) {
        result.append(new String(buffer, 0, count, StandardCharsets.UTF_8));
      }
      assertThat(result).hasToString("abcdefgh");
    }
  }

  @Test
  void select_on_postgresql_fails_if_entry_is_removed_while_reading() throws IOException {
    assumeTrue(isPostgreSql());
    slicedDao.insert(dbSession, "branch1", stringToInputStream("abcdefgh"));

    try (DbInputStream data = slicedDao.selectData(dbSession, "branch1")) {
      assertThat(data.read()).isEqualTo('a');
      slicedDao.remove(dbSession, "branch1");

      assertThatThrownBy(data::readAllBytes)
        .isInstanceOf(IOException.class)
        .hasMessageContaining("branch1");
    }
  }

  @Test
  void select_on_postgresql_fails_if_entry_is_replaced_by_same_size_value_while_reading() throws IOException {
    assumeTrue(isPostgreSql());
    slicedDao.insert(dbSession, "branch1", stringToInputStream("abcdefgh"));

    try (DbInputStream data = slicedDao.selectData(dbSession, "branch1")) {
      assertThat(data.read()).isEqualTo('a');
      slicedDao.remove(dbSession, "branch1");
      slicedDao.insert(dbSession, "branch1", stringToInputStream("ABCDEFGH"));

      assertThatThrownBy(data::readAllBytes)
        .isInstanceOf(IOException.class)
        .hasMessageContaining("branch1");
    }
  }

  @Test
  void select_on_postgresql_reads_value_up_to_single_read_size_atomically() throws IOException {
    assumeTrue(isPostgreSql());
    // 8 bytes fit the single-read limit (8), so the whole value is fetched by one statement even though slices would be 3 bytes
    var singleReadDao = new ScannerAnalysisCacheDao(3, 8, 10);
    singleReadDao.insert(dbSession, "branch1", stringToInputStream("abcdefgh"));

    try (DbInputStream data = singleReadDao.selectData(dbSession, "branch1")) {
      assertThat(data.read()).isEqualTo('a');
      // One statement is one snapshot: replacing the row after the first read cannot leak into the stream.
      // A sliced read would instead fail here, see select_on_postgresql_fails_if_entry_is_replaced_by_same_size_value_while_reading
      singleReadDao.remove(dbSession, "branch1");
      singleReadDao.insert(dbSession, "branch1", stringToInputStream("ABCDEFGH"));

      // The remaining bytes still come from the original value, never from the replacement
      assertThat(new String(data.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("bcdefgh");
    }
  }

  @Test
  void select_on_postgresql_reads_slice_by_slice_and_fails_if_row_version_changes() throws SQLException, IOException {
    ResultSet sizeRs = sizeResult(4, "100");
    // 4 bytes in slices of 3: "abc" at the pinned version, then "d" from a replaced row
    ResultSet sliceRs = mock(ResultSet.class);
    when(sliceRs.next()).thenReturn(true);
    when(sliceRs.getBytes(1)).thenReturn("abc".getBytes(StandardCharsets.UTF_8), "d".getBytes(StandardCharsets.UTF_8));
    when(sliceRs.getString(2)).thenReturn("100", "101");
    PreparedStatement sliceStmt = mock(PreparedStatement.class);
    when(sliceStmt.executeQuery()).thenReturn(sliceRs);

    try (DbInputStream data = slicedDaoOn(sizeRs, sliceStmt)) {
      assertThat(data.read()).isEqualTo('a');
      assertThat(data.read(new byte[1], 0, 0)).isZero();
      assertThatThrownBy(() -> data.read(new byte[1], 0, 2)).isInstanceOf(IndexOutOfBoundsException.class);
      // a range read never spans two slices
      byte[] buffer = new byte[5];
      assertThat(data.read(buffer, 0, 5)).isEqualTo(2);
      assertThat(new String(buffer, 0, 2, StandardCharsets.UTF_8)).isEqualTo("bc");

      assertThatThrownBy(data::read)
        .isInstanceOf(IOException.class)
        .hasMessage("Cache of branch branch1 changed while being read, at offset 3");
    }

    verify(sliceStmt, times(2)).executeQuery();
    verify(sliceStmt).close();
  }

  @Test
  void select_on_postgresql_wraps_sql_failure_when_reading_slice() throws SQLException {
    ResultSet sizeRs = sizeResult(4, "100");
    PreparedStatement sliceStmt = mock(PreparedStatement.class);
    doThrow(new SQLException("boom")).when(sliceStmt).executeQuery();

    try (DbInputStream data = slicedDaoOn(sizeRs, sliceStmt)) {
      assertThatThrownBy(data::read)
        .isInstanceOf(IOException.class)
        .hasMessage("Fail to read cache of branch branch1 at offset 0")
        .hasCauseInstanceOf(SQLException.class);
    }
  }

  @Test
  void insert_on_postgresql_warns_without_touching_connection_above_storable_size() {
    var session = mock(DbSession.class);
    var configuration = new Configuration();
    configuration.setDatabaseId(PostgreSql.ID);
    when(session.getConfiguration()).thenReturn(configuration);

    assertThat(slicedDao.insert(session, "branch1", mock(InputStream.class), 11)).isFalse();

    verify(session, never()).getConnection();
    assertThat(logTester.logs(Level.WARN))
      .containsExactly("Analysis cache of branch branch1 (11 bytes) is too large for the database and was not saved. The next analysis of this branch will run without cache.");
  }

  @Test
  void insert_on_postgresql_skips_value_above_storable_size() throws IOException {
    assumeTrue(isPostgreSql());

    assertThat(slicedDao.insert(dbSession, "branch1", stringToInputStream("test data"), 11)).isFalse();
    assertThat(slicedDao.insert(dbSession, "branch2", stringToInputStream("test data"), 10)).isTrue();

    assertThat(logTester.logs(Level.WARN))
      .containsExactly("Analysis cache of branch branch1 (11 bytes) is too large for the database and was not saved. The next analysis of this branch will run without cache.");

    assertThat(slicedDao.selectData(dbSession, "branch1")).isNull();
    assertThat(dataStreamToString(slicedDao.selectData(dbSession, "branch2"))).isEqualTo("test data");
  }

  @Test
  void insert_on_other_databases_ignores_storable_size() throws IOException {
    assumeFalse(isPostgreSql());

    assertThat(slicedDao.insert(dbSession, "branch1", stringToInputStream("test data"), 11)).isTrue();

    assertThat(logTester.logs(Level.WARN)).isEmpty();
    assertThat(dataStreamToString(slicedDao.selectData(dbSession, "branch1"))).isEqualTo("test data");
  }

  @Test
  void cleanOlderThanConfiguredDays() {
    var snapshotDao = dbTester.getDbClient().snapshotDao();
    var snapshot1 = createSnapshot(LocalDateTime.now(ZoneOffset.UTC).minusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli());
    snapshotDao.insert(dbSession, snapshot1);
    underTest.insert(dbSession, snapshot1.getRootComponentUuid(), stringToInputStream("test data"));
    var snapshot2 = createSnapshot(LocalDateTime.now(ZoneOffset.UTC).minusDays(6).toInstant(ZoneOffset.UTC).toEpochMilli());
    snapshotDao.insert(dbSession, snapshot2);
    underTest.insert(dbSession, snapshot2.getRootComponentUuid(), stringToInputStream("test data"));
    var snapshot3 = createSnapshot(LocalDateTime.now(ZoneOffset.UTC).minusDays(8).toInstant(ZoneOffset.UTC).toEpochMilli());
    snapshotDao.insert(dbSession, snapshot3);
    underTest.insert(dbSession, snapshot3.getRootComponentUuid(), stringToInputStream("test data"));
    var snapshot4 = createSnapshot(LocalDateTime.now(ZoneOffset.UTC).minusDays(30).toInstant(ZoneOffset.UTC).toEpochMilli());
    snapshotDao.insert(dbSession, snapshot4);
    underTest.insert(dbSession, snapshot4.getRootComponentUuid(), stringToInputStream("test data"));

    assertThat(dbTester.countRowsOfTable("scanner_analysis_cache")).isEqualTo(4);

    underTest.cleanOlderThan(dbSession, 7);
    dbSession.commit();

    assertThat(dbTester.countRowsOfTable("scanner_analysis_cache")).isEqualTo(2);
    assertThat(underTest.selectData(dbSession, snapshot1.getRootComponentUuid())).isNotNull();
    assertThat(underTest.selectData(dbSession, snapshot2.getRootComponentUuid())).isNotNull();
    assertThat(underTest.selectData(dbSession, snapshot3.getRootComponentUuid())).isNull();
    assertThat(underTest.selectData(dbSession, snapshot4.getRootComponentUuid())).isNull();
  }

  private boolean isPostgreSql() {
    return PostgreSql.ID.equals(dbTester.database().getDialect().getId());
  }

  private static ResultSet sizeResult(long size, String version) throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.next()).thenReturn(true);
    when(rs.getLong(1)).thenReturn(size);
    when(rs.getString(2)).thenReturn(version);
    return rs;
  }

  private DbInputStream slicedDaoOn(ResultSet sizeRs, PreparedStatement sliceStmt) throws SQLException {
    var session = mock(DbSession.class);
    var connection = mock(Connection.class);
    var sizeStmt = mock(PreparedStatement.class);
    var configuration = new Configuration();
    configuration.setDatabaseId(PostgreSql.ID);
    when(session.getConnection()).thenReturn(connection);
    when(session.getConfiguration()).thenReturn(configuration);
    when(sizeStmt.executeQuery()).thenReturn(sizeRs);
    when(connection.prepareStatement(anyString())).thenReturn(sizeStmt, sliceStmt);
    return slicedDao.selectData(session, "branch1");
  }

  private static String dataStreamToString(DbInputStream dbInputStream) throws IOException {
    try (DbInputStream is = dbInputStream) {
      return IOUtils.toString(is, StandardCharsets.UTF_8);
    }
  }

  private static InputStream stringToInputStream(String str) {
    return new ByteArrayInputStream(str.getBytes(StandardCharsets.UTF_8));
  }

  private static SnapshotDto createSnapshot(long analysisTime) {
    return new SnapshotDto()
      .setUuid(uuidFactory.create())
      .setRootComponentUuid(uuidFactory.create())
      .setStatus("P")
      .setLast(true)
      .setProjectVersion("2.1-SNAPSHOT")
      .setPeriodMode("days1")
      .setPeriodParam("30")
      .setPeriodDate(analysisTime)
      .setAnalysisDate(analysisTime);
  }

}
