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
package org.sonar.db.scannercache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbInputStream;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.SnapshotDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScannerAnalysisCacheDaoIT {
  @RegisterExtension
  private final DbTester dbTester = DbTester.create(System2.INSTANCE);
  private final static UuidFactory uuidFactory = new SequenceUuidFactory();
  private final DbSession dbSession = dbTester.getSession();
  private final ScannerAnalysisCacheDao underTest = dbTester.getDbClient().scannerAnalysisCacheDao();

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

  @Test
  void throw_illegalstateexception_when_sql_excpetion() throws SQLException {
    var dbSession = mock(DbSession.class);
    var connection = mock(Connection.class);
    when(dbSession.getConnection()).thenReturn(connection);
    when(connection.prepareStatement(anyString())).thenThrow(new SQLException());

    assertThatThrownBy(() -> underTest.selectData(dbSession, "uuid"))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Fail to select cache for branch uuid");

    assertThatThrownBy(() -> underTest.insert(dbSession, "uuid", mock(InputStream.class)))
      .isInstanceOf(IllegalStateException.class)
      .hasMessage("Fail to insert cache for branch uuid");
  }

  @Test
  void cleanOlderThan7Days() {
    var snapshotDao = dbTester.getDbClient().snapshotDao();
    var snapshot1 = createSnapshot(LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli());
    snapshotDao.insert(dbSession, snapshot1);
    underTest.insert(dbSession, snapshot1.getRootComponentUuid(), stringToInputStream("test data"));
    var snapshot2 = createSnapshot(LocalDateTime.now().minusDays(6).toInstant(ZoneOffset.UTC).toEpochMilli());
    snapshotDao.insert(dbSession, snapshot2);
    underTest.insert(dbSession, snapshot2.getRootComponentUuid(), stringToInputStream("test data"));
    var snapshot3 = createSnapshot(LocalDateTime.now().minusDays(8).toInstant(ZoneOffset.UTC).toEpochMilli());
    snapshotDao.insert(dbSession, snapshot3);
    underTest.insert(dbSession, snapshot3.getRootComponentUuid(), stringToInputStream("test data"));
    var snapshot4 = createSnapshot(LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.UTC).toEpochMilli());
    snapshotDao.insert(dbSession, snapshot4);
    underTest.insert(dbSession, snapshot4.getRootComponentUuid(), stringToInputStream("test data"));

    assertThat(dbTester.countRowsOfTable("scanner_analysis_cache")).isEqualTo(4);

    underTest.cleanOlderThan7Days(dbSession);
    dbSession.commit();

    assertThat(dbTester.countRowsOfTable("scanner_analysis_cache")).isEqualTo(2);
    assertThat(underTest.selectData(dbSession, snapshot1.getRootComponentUuid())).isNotNull();
    assertThat(underTest.selectData(dbSession, snapshot2.getRootComponentUuid())).isNotNull();
    assertThat(underTest.selectData(dbSession, snapshot3.getRootComponentUuid())).isNull();
    assertThat(underTest.selectData(dbSession, snapshot4.getRootComponentUuid())).isNull();
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
