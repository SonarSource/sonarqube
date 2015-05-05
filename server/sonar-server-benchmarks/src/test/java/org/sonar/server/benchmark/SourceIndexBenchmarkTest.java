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

import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.server.es.EsClient;
import org.sonar.server.source.db.FileSourceDb;
import org.sonar.server.source.index.*;
import org.sonar.server.tester.ServerTester;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance tests of the Elasticsearch index sourcelines
 * <ul>
 *   <li>throughput of indexing of documents</li>
 *   <li>size of ES data directory</li>
 *   <li>time to request index</li>
 * </ul>
 */
public class SourceIndexBenchmarkTest {

  private static final Logger LOGGER = LoggerFactory.getLogger("benchmarkSourceIndexing");
  private static final long FILES = 1000L;
  private static final int LINES_PER_FILE = 3220;

  @Rule
  public ServerTester tester = new ServerTester();

  @Rule
  public Benchmark benchmark = new Benchmark();

  @Test
  public void benchmark() throws Exception {
    // index source lines
    benchmarkIndexing();

    // execute some queries
    benchmarkQueries();
  }

  private void benchmarkIndexing() {
    LOGGER.info("Indexing source lines");

    SourceIterator files = new SourceIterator(FILES, LINES_PER_FILE);
    ProgressTask progressTask = new ProgressTask(LOGGER, "files of " + LINES_PER_FILE + " lines", files.count());
    Timer timer = new Timer("SourceIndexer");
    timer.schedule(progressTask, ProgressTask.PERIOD_MS, ProgressTask.PERIOD_MS);

    long start = System.currentTimeMillis();
    tester.get(SourceLineIndexer.class).index(files);
    long end = System.currentTimeMillis();

    timer.cancel();
    long period = end - start;
    long nbLines = files.count.get() * LINES_PER_FILE;
    long throughputPerSecond = 1000L * nbLines / period;
    LOGGER.info(String.format("%d lines indexed in %d ms (%d docs/second)", nbLines, period, throughputPerSecond));
    benchmark.expectAround("Throughput to index source lines", throughputPerSecond, 8950, Benchmark.DEFAULT_ERROR_MARGIN_PERCENTS);

    // be sure that physical files do not evolve during estimation of size
    tester.get(EsClient.class).prepareOptimize(SourceLineIndexDefinition.INDEX).setWaitForMerge(true).get();
    long dirSize = FileUtils.sizeOfDirectory(tester.getEsServerHolder().getHomeDir());
    LOGGER.info(String.format("ES dir: " + FileUtils.byteCountToDisplaySize(dirSize)));
    benchmark.expectBetween("ES dir size (b)", dirSize, 172L * FileUtils.ONE_MB, 182L * FileUtils.ONE_MB);
  }

  private void benchmarkQueries() {
    SourceLineIndex index = tester.get(SourceLineIndex.class);
    for (int i = 1; i <= 100; i++) {
      long start = System.currentTimeMillis();
      List<SourceLineDoc> result = index.getLines("FILE" + i, 20, 150);
      long end = System.currentTimeMillis();
      assertThat(result).hasSize(131);
      LOGGER.info("Request: {} docs in {} ms", result.size(), end - start);
    }
    // TODO assertions
  }

  private static class SourceIterator implements Iterator<FileSourcesUpdaterHelper.Row> {
    private final long nbFiles;
    private final int nbLinesPerFile;
    private int currentProject = 0;
    private AtomicLong count = new AtomicLong(0L);
    private final FileSourceDb.Data.Builder dataBuilder = FileSourceDb.Data.newBuilder();
    private final FileSourceDb.Line.Builder lineBuilder = FileSourceDb.Line.newBuilder();

    SourceIterator(long nbFiles, int nbLinesPerFile) {
      this.nbFiles = nbFiles;
      this.nbLinesPerFile = nbLinesPerFile;
    }

    public AtomicLong count() {
      return count;
    }

    @Override
    public boolean hasNext() {
      return count.get() < nbFiles;
    }

    @Override
    public FileSourcesUpdaterHelper.Row next() {
      String projectUuid = "P" + currentProject;
      String fileUuid = "FILE" + count.get();
      dataBuilder.clear();

      for (int indexLine = 1; indexLine <= nbLinesPerFile; indexLine++) {
        lineBuilder.clear();
        dataBuilder.addLines(lineBuilder
          .setLine(indexLine)
          .setScmRevision("REVISION_" + indexLine)
          .setScmAuthor("a_guy")
          .setSource("this is not java code " + indexLine)
          .setUtLineHits(2)
          .setUtConditions(8)
          .setUtCoveredConditions(2)
          .setItLineHits(2)
          .setItConditions(8)
          .setItCoveredConditions(2)
          .setOverallLineHits(2)
          .setOverallConditions(8)
          .setOverallCoveredConditions(2)
          .setScmDate(1_500_000_000_000L)
          .setHighlighting("2,9,k;9,18,k")
          .addAllDuplication(Arrays.asList(19, 33, 141))
          .build());
      }
      count.incrementAndGet();
      if (count.get() % 500 == 0) {
        currentProject++;
      }
      return SourceLineResultSetIterator.toRow(projectUuid, fileUuid, new Date(), dataBuilder.build());
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

}
