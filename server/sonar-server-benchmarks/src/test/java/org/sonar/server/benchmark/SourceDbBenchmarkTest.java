/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.benchmark;

import org.sonar.server.source.db.FileSourceDao;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.server.source.index.FileSourcesUpdaterHelper;
import org.sonar.server.source.index.SourceLineResultSetIterator;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceDbBenchmarkTest {

  public static final Logger LOGGER = LoggerFactory.getLogger("benchmarkSourceDbScrolling");

  public static final int NUMBER_OF_FILES = 1000;
  public static final int NUMBER_OF_LINES = 3220;
  public static final String PROJECT_UUID = Uuids.create();

  @Rule
  public DbTester dbTester = new DbTester();

  @Rule
  public Benchmark benchmark = new Benchmark();

  @Test
  public void benchmark() throws Exception {
    prepareTable();
    scrollRows();
  }

  private void scrollRows() throws SQLException {
    LOGGER.info("Scroll table FILE_SOURCES");
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis());
    Connection connection = dbTester.openConnection();
    AtomicLong counter = new AtomicLong();
    ProgressTask progress = new ProgressTask(LOGGER, "source file", counter);
    Timer timer = new Timer("SourceDbScroll");
    timer.schedule(progress, ProgressTask.PERIOD_MS, ProgressTask.PERIOD_MS);

    try {
      long start = System.currentTimeMillis();
      SourceLineResultSetIterator it = SourceLineResultSetIterator.create(dbClient, connection, 0L, null);
      while (it.hasNext()) {
        FileSourcesUpdaterHelper.Row row = it.next();
        assertThat(row.getUpdateRequests().size()).isEqualTo(NUMBER_OF_LINES);
        assertThat(row.getFileUuid()).isNotEmpty();
        counter.incrementAndGet();
      }
      long end = System.currentTimeMillis();
      long period = end - start;
      long throughputPerSecond = 1000L * counter.get() / period;
      LOGGER.info(String.format("%d FILE_SOURCES rows scrolled in %d ms (%d rows/second)", counter.get(), period, throughputPerSecond));
      benchmark.expectBetween("Throughput to scroll FILE_SOURCES", throughputPerSecond, 9, 13);

    } finally {
      DbUtils.closeQuietly(connection);
      timer.cancel();
    }
  }

  private void prepareTable() throws IOException {
    LOGGER.info("Populate table FILE_SOURCES");
    FileSourceDao dao = new FileSourceDao(dbTester.myBatis());
    for (int i = 0; i < NUMBER_OF_FILES; i++) {
      dao.insert(generateDto());
    }
  }

  private FileSourceDto generateDto() throws IOException {
    long now = System.currentTimeMillis();
    byte[] data = generateData();
    FileSourceDto dto = new FileSourceDto();
    dto.setCreatedAt(now);
    dto.setUpdatedAt(now);
    dto.setBinaryData(data);
    dto.setDataHash("49d7230271f2bd24c759e54bcd66547d");
    dto.setProjectUuid(PROJECT_UUID);
    dto.setFileUuid(Uuids.create());
    dto.setLineHashes(IOUtils.toString(getClass().getResourceAsStream("SourceDbBenchmarkTest/line_hashes.txt")));
    return dto;
  }

  private byte[] generateData() throws IOException {
    FileSourceDb.Data.Builder dataBuilder = FileSourceDb.Data.newBuilder();
    FileSourceDb.Line.Builder lineBuilder = FileSourceDb.Line.newBuilder();
    for (int i = 1; i <= NUMBER_OF_LINES; i++) {
      lineBuilder.clear();
      dataBuilder.addLines(lineBuilder
        .setLine(i)
        .setScmRevision("REVISION_" + i)
        .setScmAuthor("a_guy")
        .setSource("this is not java code " + i)
        .setUtLineHits(i)
        .setUtConditions(i + 1)
        .setUtCoveredConditions(i)
        .setItLineHits(i)
        .setItConditions(i + 1)
        .setItCoveredConditions(i)
        .setOverallLineHits(i)
        .setOverallConditions(i + 1)
        .setOverallCoveredConditions(i)
        .setScmDate(1_500_000_000_000L)
        .setHighlighting("2,9,k;9,18,k")
        .addAllDuplication(Arrays.asList(19, 33, 141))
        .build());
    }
    return FileSourceDto.encodeSourceData(dataBuilder.build());
  }
}
