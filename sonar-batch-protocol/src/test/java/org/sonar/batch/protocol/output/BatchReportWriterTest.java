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
package org.sonar.batch.protocol.output;

import com.google.common.collect.Iterators;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.Constants;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.Protobuf;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class BatchReportWriterTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  File dir;
  BatchReportWriter underTest;

  @Before
  public void setUp() throws Exception {
    dir = temp.newFolder();
    underTest = new BatchReportWriter(dir);
  }

  @Test
  public void create_dir_if_does_not_exist() {
    FileUtils.deleteQuietly(dir);
    underTest = new BatchReportWriter(dir);

    assertThat(dir).isDirectory().exists();
  }

  @Test
  public void write_metadata() {
    BatchReport.Metadata.Builder metadata = BatchReport.Metadata.newBuilder()
      .setAnalysisDate(15000000L)
      .setProjectKey("PROJECT_A")
      .setRootComponentRef(1);
    underTest.writeMetadata(metadata.build());

    BatchReport.Metadata read = Protobuf.read(underTest.getFileStructure().metadataFile(), BatchReport.Metadata.PARSER);
    assertThat(read.getAnalysisDate()).isEqualTo(15000000L);
    assertThat(read.getProjectKey()).isEqualTo("PROJECT_A");
    assertThat(read.getRootComponentRef()).isEqualTo(1);
  }

  @Test
  public void write_component() {
    // no data yet
    assertThat(underTest.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isFalse();

    // write data
    BatchReport.Component.Builder component = BatchReport.Component.newBuilder()
      .setRef(1)
      .setLanguage("java")
      .setPath("src/Foo.java")
      .setType(Constants.ComponentType.FILE)
      .setIsTest(false)
      .addChildRef(5)
      .addChildRef(42);
    underTest.writeComponent(component.build());

    assertThat(underTest.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isTrue();
    File file = underTest.getFileStructure().fileFor(FileStructure.Domain.COMPONENT, 1);
    assertThat(file).exists().isFile();
    BatchReport.Component read = Protobuf.read(file, BatchReport.Component.PARSER);
    assertThat(read.getRef()).isEqualTo(1);
    assertThat(read.getChildRefList()).containsOnly(5, 42);
    assertThat(read.hasName()).isFalse();
    assertThat(read.getIsTest()).isFalse();
  }

  @Test
  public void write_issues() {
    // no data yet
    assertThat(underTest.hasComponentData(FileStructure.Domain.ISSUES, 1)).isFalse();

    // write data
    BatchReport.Issue issue = BatchReport.Issue.newBuilder()
      .setLine(50)
      .setMsg("the message")
      .build();

    underTest.writeComponentIssues(1, asList(issue));

    assertThat(underTest.hasComponentData(FileStructure.Domain.ISSUES, 1)).isTrue();
    File file = underTest.getFileStructure().fileFor(FileStructure.Domain.ISSUES, 1);
    assertThat(file).exists().isFile();
    try (CloseableIterator<BatchReport.Issue> read = Protobuf.readStream(file, BatchReport.Issue.PARSER)) {
      assertThat(Iterators.size(read)).isEqualTo(1);
    }
  }

  @Test
  public void write_measures() {
    assertThat(underTest.hasComponentData(FileStructure.Domain.MEASURES, 1)).isFalse();

    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setStringValue("text-value")
      .setDoubleValue(2.5d)
      .setValueType(Constants.MeasureValueType.DOUBLE)
      .build();

    underTest.writeComponentMeasures(1, asList(measure));

    assertThat(underTest.hasComponentData(FileStructure.Domain.MEASURES, 1)).isTrue();
    File file = underTest.getFileStructure().fileFor(FileStructure.Domain.MEASURES, 1);
    assertThat(file).exists().isFile();
    try (CloseableIterator<BatchReport.Measure> read = Protobuf.readStream(file, BatchReport.Measure.PARSER)) {
      assertThat(Iterators.size(read)).isEqualTo(1);
    }
  }

  @Test
  public void write_scm() {
    assertThat(underTest.hasComponentData(FileStructure.Domain.CHANGESETS, 1)).isFalse();

    BatchReport.Changesets scm = BatchReport.Changesets.newBuilder()
      .setComponentRef(1)
      .addChangesetIndexByLine(0)
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setRevision("123-456-789")
        .setAuthor("author")
        .setDate(123_456_789L))
      .build();

    underTest.writeComponentChangesets(scm);

    assertThat(underTest.hasComponentData(FileStructure.Domain.CHANGESETS, 1)).isTrue();
    File file = underTest.getFileStructure().fileFor(FileStructure.Domain.CHANGESETS, 1);
    assertThat(file).exists().isFile();
    BatchReport.Changesets read = Protobuf.read(file, BatchReport.Changesets.PARSER);
    assertThat(read.getComponentRef()).isEqualTo(1);
    assertThat(read.getChangesetCount()).isEqualTo(1);
    assertThat(read.getChangesetList()).hasSize(1);
    assertThat(read.getChangeset(0).getDate()).isEqualTo(123_456_789L);
  }

  @Test
  public void write_duplications() {
    assertThat(underTest.hasComponentData(FileStructure.Domain.DUPLICATIONS, 1)).isFalse();

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
      .setOriginPosition(BatchReport.TextRange.newBuilder()
        .setStartLine(1)
        .setEndLine(5)
        .build())
      .addDuplicate(BatchReport.Duplicate.newBuilder()
        .setOtherFileRef(2)
        .setRange(BatchReport.TextRange.newBuilder()
          .setStartLine(6)
          .setEndLine(10)
          .build())
        .build())
      .build();
    underTest.writeComponentDuplications(1, asList(duplication));

    assertThat(underTest.hasComponentData(FileStructure.Domain.DUPLICATIONS, 1)).isTrue();
    File file = underTest.getFileStructure().fileFor(FileStructure.Domain.DUPLICATIONS, 1);
    assertThat(file).exists().isFile();
    try (CloseableIterator<BatchReport.Duplication> duplications = Protobuf.readStream(file, BatchReport.Duplication.PARSER)) {
      BatchReport.Duplication dup = duplications.next();
      assertThat(dup.getOriginPosition()).isNotNull();
      assertThat(dup.getDuplicateList()).hasSize(1);
    }
  }

  @Test
  public void write_duplication_blocks() {
    assertThat(underTest.hasComponentData(FileStructure.Domain.CPD_TEXT_BLOCKS, 1)).isFalse();

    BatchReport.CpdTextBlock duplicationBlock = BatchReport.CpdTextBlock.newBuilder()
      .setHash("abcdefghijklmnop")
      .setStartLine(1)
      .setEndLine(2)
      .setStartTokenIndex(10)
      .setEndTokenIndex(15)
      .build();
    underTest.writeCpdTextBlocks(1, asList(duplicationBlock));

    assertThat(underTest.hasComponentData(FileStructure.Domain.CPD_TEXT_BLOCKS, 1)).isTrue();
    File file = underTest.getFileStructure().fileFor(FileStructure.Domain.CPD_TEXT_BLOCKS, 1);
    assertThat(file).exists().isFile();
    try (CloseableIterator<BatchReport.CpdTextBlock> duplicationBlocks = Protobuf.readStream(file, BatchReport.CpdTextBlock.parser())) {
      BatchReport.CpdTextBlock duplicationBlockResult = duplicationBlocks.next();
      assertThat(duplicationBlockResult.getHash()).isEqualTo("abcdefghijklmnop");
      assertThat(duplicationBlockResult.getStartLine()).isEqualTo(1);
      assertThat(duplicationBlockResult.getEndLine()).isEqualTo(2);
      assertThat(duplicationBlockResult.getStartTokenIndex()).isEqualTo(10);
      assertThat(duplicationBlockResult.getEndTokenIndex()).isEqualTo(15);
    }
  }

  @Test
  public void write_symbols() {
    // no data yet
    assertThat(underTest.hasComponentData(FileStructure.Domain.SYMBOLS, 1)).isFalse();

    // write data
    BatchReport.Symbol symbol = BatchReport.Symbol.newBuilder()
      .setDeclaration(BatchReport.TextRange.newBuilder()
        .setStartLine(1)
        .setStartOffset(3)
        .setEndLine(1)
        .setEndOffset(5)
        .build())
      .addReference(BatchReport.TextRange.newBuilder()
        .setStartLine(10)
        .setStartOffset(15)
        .setEndLine(11)
        .setEndOffset(2)
        .build())
      .build();

    underTest.writeComponentSymbols(1, asList(symbol));

    assertThat(underTest.hasComponentData(FileStructure.Domain.SYMBOLS, 1)).isTrue();

    File file = underTest.getFileStructure().fileFor(FileStructure.Domain.SYMBOLS, 1);
    assertThat(file).exists().isFile();
    try (CloseableIterator<BatchReport.Symbol> read = Protobuf.readStream(file, BatchReport.Symbol.PARSER)) {
      assertThat(read).hasSize(1);
    }
  }

  @Test
  public void write_syntax_highlighting() {
    // no data yet
    assertThat(underTest.hasComponentData(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, 1)).isFalse();

    underTest.writeComponentSyntaxHighlighting(1, asList(
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.TextRange.newBuilder()
          .setStartLine(1)
          .setEndLine(1)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build()));

    assertThat(underTest.hasComponentData(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, 1)).isTrue();
  }

  @Test
  public void write_coverage() {
    // no data yet
    assertThat(underTest.hasComponentData(FileStructure.Domain.COVERAGES, 1)).isFalse();

    underTest.writeComponentCoverage(1, asList(
      BatchReport.Coverage.newBuilder()
        .setLine(1)
        .setConditions(1)
        .setUtHits(true)
        .setItHits(false)
        .setUtCoveredConditions(1)
        .setItCoveredConditions(1)
        .setOverallCoveredConditions(1)
        .build()));

    assertThat(underTest.hasComponentData(FileStructure.Domain.COVERAGES, 1)).isTrue();
  }

  @Test
  public void write_tests() {
    assertThat(underTest.hasComponentData(FileStructure.Domain.TESTS, 1)).isFalse();

    underTest.writeTests(1, asList(
      BatchReport.Test.getDefaultInstance()));

    assertThat(underTest.hasComponentData(FileStructure.Domain.TESTS, 1)).isTrue();

  }

  @Test
  public void write_coverage_details() {
    assertThat(underTest.hasComponentData(FileStructure.Domain.COVERAGE_DETAILS, 1)).isFalse();

    underTest.writeCoverageDetails(1, asList(
      BatchReport.CoverageDetail.getDefaultInstance()));

    assertThat(underTest.hasComponentData(FileStructure.Domain.COVERAGE_DETAILS, 1)).isTrue();
  }
}
