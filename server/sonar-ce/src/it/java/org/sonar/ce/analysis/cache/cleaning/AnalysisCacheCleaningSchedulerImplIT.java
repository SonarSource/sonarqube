/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.analysis.cache.cleaning;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.System2;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.scannercache.ScannerAnalysisCacheDao;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AnalysisCacheCleaningSchedulerImplIT {
  private System2 system2 = mock(System2.class);
  private final static UuidFactory uuidFactory = new SequenceUuidFactory();
  @Rule
  public DbTester dbTester = DbTester.create(system2);
  private DbSession dbSession = dbTester.getSession();
  private ScannerAnalysisCacheDao scannerAnalysisCacheDao = dbTester.getDbClient().scannerAnalysisCacheDao();

  AnalysisCacheCleaningExecutorService executorService = mock(AnalysisCacheCleaningExecutorService.class);

  AnalysisCacheCleaningSchedulerImpl underTest = new AnalysisCacheCleaningSchedulerImpl(executorService, dbTester.getDbClient());

  @Test
  public void startSchedulingOnServerStart() {
    underTest.onServerStart(mock(Server.class));
    verify(executorService, times(1)).scheduleAtFixedRate(any(Runnable.class), anyLong(), eq(DAYS.toSeconds(1)), eq(SECONDS));
  }

  @Test
  public void clean_data_older_than_7_days() {
    var snapshotDao = dbTester.getDbClient().snapshotDao();
    var snapshot1 = createSnapshot(LocalDateTime.now().minusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli());
    snapshotDao.insert(dbSession, snapshot1);
    scannerAnalysisCacheDao.insert(dbSession, snapshot1.getRootComponentUuid(), new ByteArrayInputStream("data".getBytes()));
    var snapshot2 = createSnapshot(LocalDateTime.now().minusDays(6).toInstant(ZoneOffset.UTC).toEpochMilli());
    snapshotDao.insert(dbSession, snapshot2);
    scannerAnalysisCacheDao.insert(dbSession, snapshot2.getRootComponentUuid(), new ByteArrayInputStream("data".getBytes()));
    var snapshot3 = createSnapshot(LocalDateTime.now().minusDays(8).toInstant(ZoneOffset.UTC).toEpochMilli());
    snapshotDao.insert(dbSession, snapshot3);
    scannerAnalysisCacheDao.insert(dbSession, snapshot3.getRootComponentUuid(), new ByteArrayInputStream("data".getBytes()));
    var snapshot4 = createSnapshot(LocalDateTime.now().minusDays(30).toInstant(ZoneOffset.UTC).toEpochMilli());
    snapshotDao.insert(dbSession, snapshot4);
    scannerAnalysisCacheDao.insert(dbSession, snapshot4.getRootComponentUuid(), new ByteArrayInputStream("data".getBytes()));

    assertThat(dbTester.countRowsOfTable("scanner_analysis_cache")).isEqualTo(4);

    underTest.clean();

    assertThat(dbTester.countRowsOfTable("scanner_analysis_cache")).isEqualTo(2);
    assertThat(scannerAnalysisCacheDao.selectData(dbSession, snapshot1.getRootComponentUuid())).isNotNull();
    assertThat(scannerAnalysisCacheDao.selectData(dbSession, snapshot2.getRootComponentUuid())).isNotNull();
    assertThat(scannerAnalysisCacheDao.selectData(dbSession, snapshot3.getRootComponentUuid())).isNull();
    assertThat(scannerAnalysisCacheDao.selectData(dbSession, snapshot4.getRootComponentUuid())).isNull();
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
