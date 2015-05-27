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
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.computation.ComputationContext;
import org.sonar.server.computation.component.ComponentTreeBuilders;
import org.sonar.server.computation.component.DbComponentsRefCache;
import org.sonar.server.computation.component.DbComponentsRefCache.DbComponent;
import org.sonar.server.computation.component.DumbComponent;
import org.sonar.server.computation.language.LanguageRepository;
import org.sonar.server.computation.step.PersistFileSourcesStep;
import org.sonar.server.db.DbClient;
import org.sonar.server.source.db.FileSourceDao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class PersistFileSourcesStepTest {

  public static final Logger LOGGER = LoggerFactory.getLogger("perfTestPersistFileSourcesStep");

  public static final int NUMBER_OF_FILES = 1000;
  public static final int NUMBER_OF_LINES = 1000;
  public static final String PROJECT_UUID = Uuids.create();

  DbComponentsRefCache dbComponentsRefCache = new DbComponentsRefCache();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public DbTester dbTester = new DbTester();

  @Rule
  public Benchmark benchmark = new Benchmark();

  @Test
  public void benchmark() throws Exception {
    File reportDir = prepareReport();
    persistFileSources(reportDir);
  }

  private void persistFileSources(File reportDir) {
    LOGGER.info("Persist file sources");
    DbClient dbClient = new DbClient(dbTester.database(), dbTester.myBatis(), new FileSourceDao(dbTester.myBatis()));

    long start = System.currentTimeMillis();

    PersistFileSourcesStep step = new PersistFileSourcesStep(dbClient, System2.INSTANCE, dbComponentsRefCache);
    step.execute(new ComputationContext(new BatchReportReader(reportDir), "PROJECT_KEY", new Settings(), dbClient, ComponentTreeBuilders.from(DumbComponent.DUMB_PROJECT), mock(LanguageRepository.class)));

    long end = System.currentTimeMillis();
    long duration = end - start;

    assertThat(dbTester.countRowsOfTable("file_sources")).isEqualTo(NUMBER_OF_FILES);
    LOGGER.info(String.format("File sources has been persisted in %d ms", duration));

    benchmark.expectAround("Duration to persist FILE_SOURCES", duration, 105000, Benchmark.DEFAULT_ERROR_MARGIN_PERCENTS);
  }

  private File prepareReport() throws IOException {
    LOGGER.info("Create report");
    File reportDir = temp.newFolder();

    BatchReportWriter writer = new BatchReportWriter(reportDir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    BatchReport.Component.Builder project = BatchReport.Component.newBuilder()
      .setRef(1)
      .setType(Constants.ComponentType.PROJECT);

    dbComponentsRefCache.addComponent(1, new DbComponent(1L, "PROJECT", PROJECT_UUID));

    for (int fileRef = 2; fileRef <= NUMBER_OF_FILES + 1; fileRef++) {
      generateFileReport(writer, fileRef);
      project.addChildRef(fileRef);
    }

    writer.writeComponent(project.build());

    return reportDir;
  }

  private void generateFileReport(BatchReportWriter writer, int fileRef) throws IOException {
    LineData lineData = new LineData();
    for (int line = 1; line <= NUMBER_OF_LINES; line++) {
      lineData.generateLineData(line);
    }
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(fileRef)
      .setType(Constants.ComponentType.FILE)
      .setLines(NUMBER_OF_LINES)
      .build());

    dbComponentsRefCache.addComponent(fileRef, new DbComponent((long) fileRef, "PROJECT:" + fileRef, Uuids.create()));

    FileUtils.writeLines(writer.getSourceFile(fileRef), lineData.lines);
    writer.writeComponentCoverage(fileRef, lineData.coverages);
    writer.writeComponentChangesets(lineData.changesetsBuilder.setComponentRef(fileRef).build());
    writer.writeComponentSyntaxHighlighting(fileRef, lineData.highlightings);
    writer.writeComponentSymbols(fileRef, lineData.symbols);
    writer.writeComponentDuplications(fileRef, lineData.duplications);
  }

  private static class LineData {
    List<String> lines = new ArrayList<>();
    BatchReport.Changesets.Builder changesetsBuilder = BatchReport.Changesets.newBuilder();
    List<BatchReport.Coverage> coverages = new ArrayList<>();
    List<BatchReport.SyntaxHighlighting> highlightings = new ArrayList<>();
    List<BatchReport.Symbols.Symbol> symbols = new ArrayList<>();
    List<BatchReport.Duplication> duplications = new ArrayList<>();

    void generateLineData(int line){
      lines.add("line-" + line);

      changesetsBuilder.addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setAuthor("author-" + line)
        .setDate(123456789L)
        .setRevision("rev-" + line)
        .build())
        .addChangesetIndexByLine(line - 1);

      coverages.add(BatchReport.Coverage.newBuilder()
        .setLine(line)
        .setConditions(10)
        .setUtHits(true)
        .setUtCoveredConditions(2)
        .setItHits(true)
        .setItCoveredConditions(3)
        .setOverallCoveredConditions(4)
        .build());

      highlightings.add(BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(line).setEndLine(line)
          .setStartOffset(1).setEndOffset(3)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build());

      symbols.add(BatchReport.Symbols.Symbol.newBuilder()
        .setDeclaration(BatchReport.Range.newBuilder()
          .setStartLine(line).setEndLine(line).setStartOffset(2).setEndOffset(4)
          .build())
        .addReference(BatchReport.Range.newBuilder()
            .setStartLine(line + 1).setEndLine(line + 1).setStartOffset(1).setEndOffset(3)
            .build()
        ).build());

      duplications.add(BatchReport.Duplication.newBuilder()
        .setOriginPosition(BatchReport.Range.newBuilder()
          .setStartLine(line)
          .setEndLine(line)
          .build())
        .addDuplicate(BatchReport.Duplicate.newBuilder()
          .setRange(BatchReport.Range.newBuilder()
            .setStartLine(line + 1)
            .setEndLine(line + 1)
            .build())
          .build())
        .build());
    }
  }

}
