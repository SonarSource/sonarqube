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

import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.Constants;
import org.sonar.core.util.CloseableIterator;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class BatchReportReaderTest {

  private static int UNKNOWN_COMPONENT_REF = 123;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File dir;

  BatchReportReader underTest;

  @Before
  public void setUp() throws Exception {
    dir = temp.newFolder();
    underTest = new BatchReportReader(dir);
  }

  @Test
  public void read_metadata() {
    BatchReportWriter writer = new BatchReportWriter(dir);
    BatchReport.Metadata.Builder metadata = BatchReport.Metadata.newBuilder()
      .setAnalysisDate(15000000L)
      .setProjectKey("PROJECT_A")
      .setRootComponentRef(1)
      .setCrossProjectDuplicationActivated(true);
    writer.writeMetadata(metadata.build());

    BatchReport.Metadata readMetadata = underTest.readMetadata();
    assertThat(readMetadata.getAnalysisDate()).isEqualTo(15000000L);
    assertThat(readMetadata.getProjectKey()).isEqualTo("PROJECT_A");
    assertThat(readMetadata.getRootComponentRef()).isEqualTo(1);
    assertThat(readMetadata.getCrossProjectDuplicationActivated()).isTrue();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_metadata_file() {
    underTest.readMetadata();
  }

  @Test
  public void read_components() {
    BatchReportWriter writer = new BatchReportWriter(dir);
    BatchReport.Component.Builder component = BatchReport.Component.newBuilder()
      .setRef(1)
      .setPath("src/main/java/Foo.java");
    writer.writeComponent(component.build());

    assertThat(underTest.readComponent(1).getPath()).isEqualTo("src/main/java/Foo.java");
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_file_on_component() {
    underTest.readComponent(UNKNOWN_COMPONENT_REF);
  }

  @Test
  public void read_issues() {
    BatchReportWriter writer = new BatchReportWriter(dir);
    BatchReport.Issue issue = BatchReport.Issue.newBuilder()
      .setLine(50)
      .build();
    writer.writeComponentIssues(1, asList(issue));

    assertThat(underTest.readComponentIssues(1)).hasSize(1);
    assertThat(underTest.readComponentIssues(200)).isEmpty();
  }

  @Test
  public void empty_list_if_no_issue_found() {
    assertThat(underTest.readComponentIssues(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_measures() {
    BatchReportWriter writer = new BatchReportWriter(dir);
    BatchReport.Measure.Builder measure = BatchReport.Measure.newBuilder()
      .setStringValue("value_a");
    writer.writeComponentMeasures(1, asList(measure.build()));

    assertThat(underTest.readComponentMeasures(1)).hasSize(1);
  }

  @Test
  public void empty_list_if_no_measure_found() {
    assertThat(underTest.readComponentMeasures(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_changesets() {
    BatchReportWriter writer = new BatchReportWriter(dir);
    BatchReport.Changesets.Builder scm = BatchReport.Changesets.newBuilder()
      .setComponentRef(1)
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder().setDate(123_456_789).setAuthor("jack.daniels").setRevision("123-456-789"));
    writer.writeComponentChangesets(scm.build());

    assertThat(underTest.readChangesets(1).getChangesetList()).hasSize(1);
    assertThat(underTest.readChangesets(1).getChangeset(0).getDate()).isEqualTo(123_456_789L);
  }

  @Test
  public void null_if_no_changeset_found() {
    assertThat(underTest.readChangesets(UNKNOWN_COMPONENT_REF)).isNull();
  }

  @Test
  public void read_duplications() {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1).build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1).build());

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
    writer.writeComponentDuplications(1, asList(duplication));

    BatchReportReader sut = new BatchReportReader(dir);
    assertThat(sut.readComponentDuplications(1)).hasSize(1);
  }

  @Test
  public void empty_list_if_no_duplication_found() {
    assertThat(underTest.readComponentDuplications(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_duplication_blocks() {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1).build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1).build());

    BatchReport.CpdTextBlock duplicationBlock = BatchReport.CpdTextBlock.newBuilder()
      .setHash("abcdefghijklmnop")
      .setStartLine(1)
      .setEndLine(2)
      .setStartTokenIndex(10)
      .setEndTokenIndex(15)
      .build();
    writer.writeCpdTextBlocks(1, singletonList(duplicationBlock));

    BatchReportReader sut = new BatchReportReader(dir);
    assertThat(sut.readCpdTextBlocks(1)).hasSize(1);
  }

  @Test
  public void empty_list_if_no_duplication_block_found() {
    assertThat(underTest.readComponentDuplications(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_syntax_highlighting() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1).build());

    writer.writeComponentSyntaxHighlighting(1, asList(
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.TextRange.newBuilder()
          .setStartLine(1)
          .setEndLine(10)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build()));

    try (CloseableIterator<BatchReport.SyntaxHighlighting> it = underTest.readComponentSyntaxHighlighting(1)) {
      BatchReport.SyntaxHighlighting syntaxHighlighting = it.next();
      assertThat(syntaxHighlighting.getRange()).isNotNull();
      assertThat(syntaxHighlighting.getRange().getStartLine()).isEqualTo(1);
      assertThat(syntaxHighlighting.getRange().getEndLine()).isEqualTo(10);
      assertThat(syntaxHighlighting.getType()).isEqualTo(Constants.HighlightingType.ANNOTATION);
    }
  }

  @Test
  public void return_empty_if_no_highlighting_found() {
    assertThat(underTest.readComponentSyntaxHighlighting(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_symbols() {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1).build());

    writer.writeComponentSymbols(1, asList(BatchReport.Symbol.newBuilder()
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
      .build()));

    underTest = new BatchReportReader(dir);
    assertThat(underTest.readComponentSymbols(1)).hasSize(1);
  }

  @Test
  public void empty_list_if_no_symbol_found() {
    assertThat(underTest.readComponentSymbols(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_coverage() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1).build());

    writer.writeComponentCoverage(1, asList(
      BatchReport.Coverage.newBuilder()
        .setLine(1)
        .setConditions(1)
        .setUtHits(true)
        .setItHits(false)
        .setUtCoveredConditions(1)
        .setItCoveredConditions(1)
        .setOverallCoveredConditions(1)
        .build(),
      BatchReport.Coverage.newBuilder()
        .setLine(2)
        .setConditions(5)
        .setUtHits(false)
        .setItHits(false)
        .setUtCoveredConditions(4)
        .setItCoveredConditions(5)
        .setOverallCoveredConditions(5)
        .build()));

    underTest = new BatchReportReader(dir);
    try (CloseableIterator<BatchReport.Coverage> it = new BatchReportReader(dir).readComponentCoverage(1)) {
      BatchReport.Coverage coverage = it.next();
      assertThat(coverage.getLine()).isEqualTo(1);
      assertThat(coverage.getConditions()).isEqualTo(1);
      assertThat(coverage.getUtHits()).isTrue();
      assertThat(coverage.getItHits()).isFalse();
      assertThat(coverage.getUtCoveredConditions()).isEqualTo(1);
      assertThat(coverage.getItCoveredConditions()).isEqualTo(1);
      assertThat(coverage.getOverallCoveredConditions()).isEqualTo(1);
    }
  }

  @Test
  public void return_empty_iterator_if_no_coverage_found() {
    assertThat(underTest.readComponentCoverage(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_source_lines() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.SOURCE, 1);
    FileUtils.writeLines(file, Lists.newArrayList("line1", "line2"));

    File sourceFile = new BatchReportReader(dir).readFileSource(1);
    assertThat(sourceFile).isEqualTo(file);
  }

  @Test
  public void read_tests() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeTests(1, asList(
      BatchReport.Test.newBuilder()
        .setDurationInMs(60_000)
        .setStacktrace("stacktrace")
        .setMsg("message")
        .setStatus(Constants.TestStatus.OK)
        .build()));

    try (InputStream inputStream = FileUtils.openInputStream(underTest.readTests(1))) {
      BatchReport.Test testResult = BatchReport.Test.PARSER.parseDelimitedFrom(inputStream);
      assertThat(testResult.getDurationInMs()).isEqualTo(60_000);
      assertThat(testResult.getStacktrace()).isEqualTo("stacktrace");
      assertThat(testResult.getMsg()).isEqualTo("message");
      assertThat(testResult.getStatus()).isEqualTo(Constants.TestStatus.OK);
    }
  }

  @Test
  public void null_if_no_test_found() {
    assertThat(underTest.readTests(UNKNOWN_COMPONENT_REF)).isNull();
  }

  @Test
  public void read_coverage_details() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeCoverageDetails(1, asList(
      BatchReport.CoverageDetail.newBuilder()
        .setTestName("test-name")
        .addCoveredFile(BatchReport.CoverageDetail.CoveredFile.newBuilder()
          .addAllCoveredLine(asList(1, 2, 3, 5, 7))
          .setFileRef(2))
        .build()));

    try (InputStream inputStream = FileUtils.openInputStream(underTest.readCoverageDetails(1))) {
      BatchReport.CoverageDetail coverageDetail = BatchReport.CoverageDetail.PARSER.parseDelimitedFrom(inputStream);
      assertThat(coverageDetail.getTestName()).isEqualTo("test-name");
      assertThat(coverageDetail.getCoveredFile(0).getFileRef()).isEqualTo(2);
      assertThat(coverageDetail.getCoveredFile(0).getCoveredLineList()).containsExactly(1, 2, 3, 5, 7);
    }
  }

  @Test
  public void null_if_no_coverage_detail_found() {
    assertThat(underTest.readCoverageDetails(UNKNOWN_COMPONENT_REF)).isNull();
  }

  @Test
  public void read_file_source() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    try (FileOutputStream outputStream = new FileOutputStream(writer.getSourceFile(1))) {
      IOUtils.write("line1\nline2", outputStream);
    }

    try (InputStream inputStream = FileUtils.openInputStream(underTest.readFileSource(1))) {
      assertThat(IOUtils.readLines(inputStream)).containsOnly("line1", "line2");
    }
  }

  @Test
  public void return_null_when_no_file_source() throws Exception {
    assertThat(underTest.readFileSource(UNKNOWN_COMPONENT_REF)).isNull();
  }
}
