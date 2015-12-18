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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.utils.System2;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportWriter;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.server.computation.analysis.AnalysisMetadataHolderRule;
import org.sonar.server.computation.batch.BatchReportDirectoryHolderImpl;
import org.sonar.server.computation.batch.BatchReportReaderImpl;
import org.sonar.server.computation.batch.TreeRootHolderRule;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportComponent;
import org.sonar.server.computation.duplication.Duplicate;
import org.sonar.server.computation.duplication.Duplication;
import org.sonar.server.computation.duplication.DuplicationRepositoryRule;
import org.sonar.server.computation.duplication.InnerDuplicate;
import org.sonar.server.computation.duplication.TextBlock;
import org.sonar.server.computation.scm.ScmInfoRepositoryImpl;
import org.sonar.server.computation.source.SourceHashRepositoryImpl;
import org.sonar.server.computation.source.SourceLinesRepositoryImpl;
import org.sonar.server.computation.step.PersistFileSourcesStep;

import static org.assertj.core.api.Assertions.assertThat;

public class PersistFileSourcesStepTest {

  public static final Logger LOGGER = LoggerFactory.getLogger("perfTestPersistFileSourcesStep");

  public static final int NUMBER_OF_FILES = 1000;
  public static final int NUMBER_OF_LINES = 1000;
  public static final String PROJECT_UUID = Uuids.create();

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public Benchmark benchmark = new Benchmark();
  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule();
  @Rule
  public AnalysisMetadataHolderRule analysisMetadataHolder = new AnalysisMetadataHolderRule();
  @Rule
  public DuplicationRepositoryRule duplicationRepository = DuplicationRepositoryRule.create(treeRootHolder);

  @Test
  public void benchmark() throws Exception {
    File reportDir = prepareReport();
    persistFileSources(reportDir);
  }

  private void persistFileSources(File reportDir) {
    LOGGER.info("Persist file sources");
    DbClient dbClient = dbTester.getDbClient();

    long start = System.currentTimeMillis();

    BatchReportDirectoryHolderImpl batchReportDirectoryHolder = new BatchReportDirectoryHolderImpl();
    batchReportDirectoryHolder.setDirectory(reportDir);
    org.sonar.server.computation.batch.BatchReportReader batchReportReader = new BatchReportReaderImpl(batchReportDirectoryHolder);
    analysisMetadataHolder.setBaseProjectSnapshot(null);
    SourceLinesRepositoryImpl sourceLinesRepository = new SourceLinesRepositoryImpl(batchReportReader);
    SourceHashRepositoryImpl sourceHashRepository = new SourceHashRepositoryImpl(sourceLinesRepository);
    ScmInfoRepositoryImpl scmInfoRepository = new ScmInfoRepositoryImpl(batchReportReader, analysisMetadataHolder, dbClient, sourceHashRepository);
    PersistFileSourcesStep step = new PersistFileSourcesStep(dbClient, System2.INSTANCE, treeRootHolder, batchReportReader, sourceLinesRepository, scmInfoRepository,
      duplicationRepository);
    step.execute();

    long end = System.currentTimeMillis();
    long duration = end - start;

    assertThat(dbTester.countRowsOfTable("file_sources")).isEqualTo(NUMBER_OF_FILES);
    LOGGER.info(String.format("File sources have been persisted in %d ms", duration));

    benchmark.expectAround("Duration to persist FILE_SOURCES", duration, 93700, Benchmark.DEFAULT_ERROR_MARGIN_PERCENTS);
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

    List<Component> components = new ArrayList<>();
    for (int fileRef = 2; fileRef <= NUMBER_OF_FILES + 1; fileRef++) {
      ReportComponent component = ReportComponent.builder(Component.Type.FILE, fileRef).setUuid(Uuids.create()).setKey("PROJECT:" + fileRef).build();
      components.add(component);
    }
    treeRootHolder.setRoot(ReportComponent.builder(Component.Type.PROJECT, 1)
      .setUuid(PROJECT_UUID)
      .setKey("PROJECT")
      .addChildren(components.toArray(new Component[components.size()]))
      .build());
    for (int fileRef = 2; fileRef <= NUMBER_OF_FILES + 1; fileRef++) {
      generateFileReport(writer, fileRef);
      project.addChildRef(fileRef);
    }

    writer.writeComponent(project.build());

    return reportDir;
  }

  private Component generateFileReport(BatchReportWriter writer, int fileRef) throws IOException {
    LineData lineData = new LineData();
    for (int line = 1; line <= NUMBER_OF_LINES; line++) {
      lineData.generateLineData(line);
      duplicationRepository.add(
        fileRef,
        new Duplication(
          new TextBlock(line, line),
          Arrays.<Duplicate>asList(new InnerDuplicate(new TextBlock(line + 1, line + 1)))
        )
        );
    }
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(fileRef)
      .setType(Constants.ComponentType.FILE)
      .setLines(NUMBER_OF_LINES)
      .build());

    FileUtils.writeLines(writer.getSourceFile(fileRef), lineData.lines);
    writer.writeComponentCoverage(fileRef, lineData.coverages);
    writer.writeComponentChangesets(lineData.changesetsBuilder.setComponentRef(fileRef).build());
    writer.writeComponentSyntaxHighlighting(fileRef, lineData.highlightings);
    writer.writeComponentSymbols(fileRef, lineData.symbols);

    return ReportComponent.builder(Component.Type.FILE, fileRef).setUuid(Uuids.create()).setKey("PROJECT:" + fileRef).build();
  }

  private static class LineData {
    List<String> lines = new ArrayList<>();
    BatchReport.Changesets.Builder changesetsBuilder = BatchReport.Changesets.newBuilder();
    List<BatchReport.Coverage> coverages = new ArrayList<>();
    List<BatchReport.SyntaxHighlighting> highlightings = new ArrayList<>();
    List<BatchReport.Symbol> symbols = new ArrayList<>();

    void generateLineData(int line) {
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
        .setRange(BatchReport.TextRange.newBuilder()
          .setStartLine(line).setEndLine(line)
          .setStartOffset(1).setEndOffset(3)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build());

      symbols.add(BatchReport.Symbol.newBuilder()
        .setDeclaration(BatchReport.TextRange.newBuilder()
          .setStartLine(line).setEndLine(line).setStartOffset(2).setEndOffset(4)
          .build())
        .addReference(BatchReport.TextRange.newBuilder()
          .setStartLine(line + 1).setEndLine(line + 1).setStartOffset(1).setEndOffset(3)
          .build())
        .build());
    }
  }

}
