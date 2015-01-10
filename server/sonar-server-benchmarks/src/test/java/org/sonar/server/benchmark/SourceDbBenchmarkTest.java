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

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.core.persistence.DbTester;
import org.sonar.core.source.db.FileSourceDao;
import org.sonar.core.source.db.FileSourceDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.index.SourceLineResultSetIterator;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class SourceDbBenchmarkTest {

  public static final Logger LOGGER = LoggerFactory.getLogger("benchmarkSourceDbScrolling");
  // files are 3'220 lines long
  public static final int NUMBER_OF_FILES = 1000;
  public static final String PROJECT_UUID = Uuids.create();

  @Rule
  public DbTester dbTester = new DbTester();

  @Test
  public void benchmark() throws Exception {
    prepareFileSources();
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
      SourceLineResultSetIterator it = SourceLineResultSetIterator.create(dbClient, connection, 0L);
      while (it.hasNext()) {
        SourceLineResultSetIterator.SourceFile row = it.next();
        assertThat(row.getLines().size()).isEqualTo(3220);
        assertThat(row.getFileUuid()).isNotEmpty();
        counter.incrementAndGet();
      }
      long end = System.currentTimeMillis();
      long period = end-start;
      long throughputPerSecond = 1000L * counter.get() / period;
      LOGGER.info(String.format("%d FILE_SOURCES rows scrolled in %d ms (%d rows/second)", counter.get(), period, throughputPerSecond));

    } finally {
      DbUtils.closeQuietly(connection);
      timer.cancel();
    }
  }

  private void prepareFileSources() throws IOException {
    LOGGER.info("Populate table FILE_SOURCES");
    FileSourceDao dao = new FileSourceDao(dbTester.myBatis());
    for (int i = 0; i < NUMBER_OF_FILES; i++) {
      dao.insert(newFileSourceDto());
    }
  }

  private FileSourceDto newFileSourceDto() throws IOException {
    long now = System.currentTimeMillis();
    FileSourceDto dto = new FileSourceDto();
    dto.setCreatedAt(now);
    dto.setUpdatedAt(now);
    dto.setProjectUuid(PROJECT_UUID);
    dto.setFileUuid(Uuids.create());
    // this fake data is 3220 lines long
    dto.setData(IOUtils.toString(getClass().getResourceAsStream("SourceDbBenchmarkTest/data.txt")));
    dto.setDataHash("49d7230271f2bd24c759e54bcd66547d");
    dto.setLineHashes(IOUtils.toString(getClass().getResourceAsStream("SourceDbBenchmarkTest/line_hashes.txt")));
    return dto;
  }
}
