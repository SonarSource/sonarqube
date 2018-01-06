/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.protocol.output;

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
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.StringValue;
import org.sonar.scanner.protocol.output.ScannerReport.SyntaxHighlightingRule.HighlightingType;
import org.sonar.scanner.protocol.output.ScannerReport.Test.TestStatus;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ScannerReportReaderTest {

  private static int UNKNOWN_COMPONENT_REF = 123;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File dir;

  ScannerReportReader underTest;

  @Before
  public void setUp() throws Exception {
    dir = temp.newFolder();
    underTest = new ScannerReportReader(dir);
  }

  @Test
  public void read_metadata() {
    ScannerReportWriter writer = new ScannerReportWriter(dir);
    ScannerReport.Metadata.Builder metadata = ScannerReport.Metadata.newBuilder()
      .setAnalysisDate(15000000L)
      .setProjectKey("PROJECT_A")
      .setRootComponentRef(1)
      .setCrossProjectDuplicationActivated(true);
    writer.writeMetadata(metadata.build());

    ScannerReport.Metadata readMetadata = underTest.readMetadata();
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
    ScannerReportWriter writer = new ScannerReportWriter(dir);
    ScannerReport.Component.Builder component = ScannerReport.Component.newBuilder()
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
    ScannerReportWriter writer = new ScannerReportWriter(dir);
    ScannerReport.Issue issue = ScannerReport.Issue.newBuilder()
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
    ScannerReportWriter writer = new ScannerReportWriter(dir);
    ScannerReport.Measure.Builder measure = ScannerReport.Measure.newBuilder()
      .setStringValue(StringValue.newBuilder().setValue("value_a"));
    writer.writeComponentMeasures(1, asList(measure.build()));

    assertThat(underTest.readComponentMeasures(1)).hasSize(1);
  }

  @Test
  public void empty_list_if_no_measure_found() {
    assertThat(underTest.readComponentMeasures(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_changesets() {
    ScannerReportWriter writer = new ScannerReportWriter(dir);
    ScannerReport.Changesets.Builder scm = ScannerReport.Changesets.newBuilder()
      .setComponentRef(1)
      .addChangeset(ScannerReport.Changesets.Changeset.newBuilder().setDate(123_456_789).setAuthor("jack.daniels").setRevision("123-456-789"));
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
    ScannerReportWriter writer = new ScannerReportWriter(dir);
    writer.writeMetadata(ScannerReport.Metadata.newBuilder()
      .setRootComponentRef(1).build());
    writer.writeComponent(ScannerReport.Component.newBuilder()
      .setRef(1).build());

    ScannerReport.Duplication duplication = ScannerReport.Duplication.newBuilder()
      .setOriginPosition(ScannerReport.TextRange.newBuilder()
        .setStartLine(1)
        .setEndLine(5)
        .build())
      .addDuplicate(ScannerReport.Duplicate.newBuilder()
        .setOtherFileRef(2)
        .setRange(ScannerReport.TextRange.newBuilder()
          .setStartLine(6)
          .setEndLine(10)
          .build())
        .build())
      .build();
    writer.writeComponentDuplications(1, asList(duplication));

    ScannerReportReader sut = new ScannerReportReader(dir);
    assertThat(sut.readComponentDuplications(1)).hasSize(1);
  }

  @Test
  public void empty_list_if_no_duplication_found() {
    assertThat(underTest.readComponentDuplications(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_duplication_blocks() {
    ScannerReportWriter writer = new ScannerReportWriter(dir);
    writer.writeMetadata(ScannerReport.Metadata.newBuilder()
      .setRootComponentRef(1).build());
    writer.writeComponent(ScannerReport.Component.newBuilder()
      .setRef(1).build());

    ScannerReport.CpdTextBlock duplicationBlock = ScannerReport.CpdTextBlock.newBuilder()
      .setHash("abcdefghijklmnop")
      .setStartLine(1)
      .setEndLine(2)
      .setStartTokenIndex(10)
      .setEndTokenIndex(15)
      .build();
    writer.writeCpdTextBlocks(1, singletonList(duplicationBlock));

    ScannerReportReader sut = new ScannerReportReader(dir);
    assertThat(sut.readCpdTextBlocks(1)).hasSize(1);
  }

  @Test
  public void empty_list_if_no_duplication_block_found() {
    assertThat(underTest.readComponentDuplications(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_syntax_highlighting() throws Exception {
    ScannerReportWriter writer = new ScannerReportWriter(dir);
    writer.writeMetadata(ScannerReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    writer.writeComponent(ScannerReport.Component.newBuilder()
      .setRef(1).build());

    writer.writeComponentSyntaxHighlighting(1, asList(
      ScannerReport.SyntaxHighlightingRule.newBuilder()
        .setRange(ScannerReport.TextRange.newBuilder()
          .setStartLine(1)
          .setEndLine(10)
          .build())
        .setType(HighlightingType.ANNOTATION)
        .build()));

    try (CloseableIterator<ScannerReport.SyntaxHighlightingRule> it = underTest.readComponentSyntaxHighlighting(1)) {
      ScannerReport.SyntaxHighlightingRule syntaxHighlighting = it.next();
      assertThat(syntaxHighlighting.getRange()).isNotNull();
      assertThat(syntaxHighlighting.getRange().getStartLine()).isEqualTo(1);
      assertThat(syntaxHighlighting.getRange().getEndLine()).isEqualTo(10);
      assertThat(syntaxHighlighting.getType()).isEqualTo(HighlightingType.ANNOTATION);
    }
  }

  @Test
  public void return_empty_if_no_highlighting_found() {
    assertThat(underTest.readComponentSyntaxHighlighting(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_symbols() {
    ScannerReportWriter writer = new ScannerReportWriter(dir);
    writer.writeMetadata(ScannerReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    writer.writeComponent(ScannerReport.Component.newBuilder()
      .setRef(1).build());

    writer.writeComponentSymbols(1, asList(ScannerReport.Symbol.newBuilder()
      .setDeclaration(ScannerReport.TextRange.newBuilder()
        .setStartLine(1)
        .setStartOffset(3)
        .setEndLine(1)
        .setEndOffset(5)
        .build())
      .addReference(ScannerReport.TextRange.newBuilder()
        .setStartLine(10)
        .setStartOffset(15)
        .setEndLine(11)
        .setEndOffset(2)
        .build())
      .build()));

    underTest = new ScannerReportReader(dir);
    assertThat(underTest.readComponentSymbols(1)).hasSize(1);
  }

  @Test
  public void empty_list_if_no_symbol_found() {
    assertThat(underTest.readComponentSymbols(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_coverage() throws Exception {
    ScannerReportWriter writer = new ScannerReportWriter(dir);
    writer.writeMetadata(ScannerReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    writer.writeComponent(ScannerReport.Component.newBuilder()
      .setRef(1).build());

    writer.writeComponentCoverage(1, asList(
      ScannerReport.LineCoverage.newBuilder()
        .setLine(1)
        .setConditions(1)
        .setHits(true)
        .setCoveredConditions(1)
        .build(),
      ScannerReport.LineCoverage.newBuilder()
        .setLine(2)
        .setConditions(5)
        .setHits(false)
        .setCoveredConditions(4)
        .build()));

    underTest = new ScannerReportReader(dir);
    try (CloseableIterator<ScannerReport.LineCoverage> it = new ScannerReportReader(dir).readComponentCoverage(1)) {
      ScannerReport.LineCoverage coverage = it.next();
      assertThat(coverage.getLine()).isEqualTo(1);
      assertThat(coverage.getConditions()).isEqualTo(1);
      assertThat(coverage.getHits()).isTrue();
      assertThat(coverage.getCoveredConditions()).isEqualTo(1);
    }
  }

  @Test
  public void return_empty_iterator_if_no_coverage_found() {
    assertThat(underTest.readComponentCoverage(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_source_lines() throws Exception {
    ScannerReportWriter writer = new ScannerReportWriter(dir);
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.SOURCE, 1);
    FileUtils.writeLines(file, Lists.newArrayList("line1", "line2"));

    File sourceFile = new ScannerReportReader(dir).readFileSource(1);
    assertThat(sourceFile).isEqualTo(file);
  }

  @Test
  public void read_tests() throws Exception {
    ScannerReportWriter writer = new ScannerReportWriter(dir);
    writer.writeTests(1, asList(
      ScannerReport.Test.newBuilder()
        .setDurationInMs(60_000)
        .setStacktrace("stacktrace")
        .setMsg("message")
        .setStatus(TestStatus.OK)
        .build()));

    try (InputStream inputStream = FileUtils.openInputStream(underTest.readTests(1))) {
      ScannerReport.Test testResult = ScannerReport.Test.parser().parseDelimitedFrom(inputStream);
      assertThat(testResult.getDurationInMs()).isEqualTo(60_000);
      assertThat(testResult.getStacktrace()).isEqualTo("stacktrace");
      assertThat(testResult.getMsg()).isEqualTo("message");
      assertThat(testResult.getStatus()).isEqualTo(TestStatus.OK);
    }
  }

  @Test
  public void null_if_no_test_found() {
    assertThat(underTest.readTests(UNKNOWN_COMPONENT_REF)).isNull();
  }

  @Test
  public void read_coverage_details() throws Exception {
    ScannerReportWriter writer = new ScannerReportWriter(dir);
    writer.writeCoverageDetails(1, asList(
      ScannerReport.CoverageDetail.newBuilder()
        .setTestName("test-name")
        .addCoveredFile(ScannerReport.CoverageDetail.CoveredFile.newBuilder()
          .addAllCoveredLine(asList(1, 2, 3, 5, 7))
          .setFileRef(2))
        .build()));

    try (InputStream inputStream = FileUtils.openInputStream(underTest.readCoverageDetails(1))) {
      ScannerReport.CoverageDetail coverageDetail = ScannerReport.CoverageDetail.parser().parseDelimitedFrom(inputStream);
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
    ScannerReportWriter writer = new ScannerReportWriter(dir);
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
