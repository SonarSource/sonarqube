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
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.Constants;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchReportReaderTest {

  private static int UNKNOWN_COMPONENT_REF = 123;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  File dir;

  BatchReportReader sut;

  @Before
  public void setUp() throws Exception {
    dir = temp.newFolder();
    sut = new BatchReportReader(dir);
  }

  @Test
  public void read_metadata() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    BatchReport.Metadata.Builder metadata = BatchReport.Metadata.newBuilder()
      .setAnalysisDate(15000000L)
      .setProjectKey("PROJECT_A")
      .setRootComponentRef(1)
      .setDeletedComponentsCount(10);
    writer.writeMetadata(metadata.build());

    BatchReport.Metadata readMetadata = sut.readMetadata();
    assertThat(readMetadata.getAnalysisDate()).isEqualTo(15000000L);
    assertThat(readMetadata.getProjectKey()).isEqualTo("PROJECT_A");
    assertThat(readMetadata.getDeletedComponentsCount()).isEqualTo(10);
    assertThat(readMetadata.getRootComponentRef()).isEqualTo(1);
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_metadata_file() throws Exception {
    sut.readMetadata();
  }

  @Test
  public void read_components() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    BatchReport.Component.Builder component = BatchReport.Component.newBuilder()
      .setRef(1)
      .setPath("src/main/java/Foo.java");
    writer.writeComponent(component.build());

    assertThat(sut.readComponent(1).getPath()).isEqualTo("src/main/java/Foo.java");
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_file_on_component() throws Exception {
    sut.readComponent(UNKNOWN_COMPONENT_REF);
  }

  @Test
  public void read_issues() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    BatchReport.Issue issue = BatchReport.Issue.newBuilder()
      .setUuid("ISSUE_A")
      .setLine(50)
      .build();
    writer.writeComponentIssues(1, Arrays.asList(issue));
    writer.writeDeletedComponentIssues(1, "compUuid", Arrays.asList(issue));

    assertThat(sut.readComponentIssues(1)).hasSize(1);
    assertThat(sut.readComponentIssues(200)).isEmpty();

    BatchReport.Issues deletedComponentIssues = sut.readDeletedComponentIssues(1);
    assertThat(deletedComponentIssues.getComponentUuid()).isEqualTo("compUuid");
    assertThat(deletedComponentIssues.getIssueList()).hasSize(1);
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_file_on_deleted_component() throws Exception {
    sut.readDeletedComponentIssues(UNKNOWN_COMPONENT_REF);
  }

  @Test
  public void empty_list_if_no_issue_found() throws Exception {
    assertThat(sut.readComponentIssues(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_measures() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    BatchReport.Measure.Builder measure = BatchReport.Measure.newBuilder()
      .setStringValue("value_a");
    writer.writeComponentMeasures(1, Arrays.asList(measure.build()));

    assertThat(sut.readComponentMeasures(1)).hasSize(1);
    assertThat(sut.readComponentMeasures(1).get(0).getStringValue()).isEqualTo("value_a");
  }

  @Test
  public void empty_list_if_no_measure_found() throws Exception {
    assertThat(sut.readComponentMeasures(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_changesets() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    BatchReport.Changesets.Builder scm = BatchReport.Changesets.newBuilder()
      .setComponentRef(1)
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder().setDate(123_456_789).setAuthor("jack.daniels").setRevision("123-456-789"));
    writer.writeComponentChangesets(scm.build());

    assertThat(sut.readChangesets(1).getChangesetList()).hasSize(1);
    assertThat(sut.readChangesets(1).getChangeset(0).getDate()).isEqualTo(123_456_789L);
  }

  @Test
  public void null_if_no_changeset_found() throws Exception {
    assertThat(sut.readChangesets(UNKNOWN_COMPONENT_REF)).isNull();
  }

  @Test
  public void read_duplications() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1).build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1).build());

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
      .setOriginPosition(BatchReport.Range.newBuilder()
        .setStartLine(1)
        .setEndLine(5)
        .build())
      .addDuplicate(BatchReport.Duplicate.newBuilder()
        .setOtherFileKey("COMPONENT_A")
        .setOtherFileRef(2)
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(6)
          .setEndLine(10)
          .build())
        .build())
      .build();
    writer.writeComponentDuplications(1, Arrays.asList(duplication));

    BatchReportReader sut = new BatchReportReader(dir);
    assertThat(sut.readComponentDuplications(1)).hasSize(1);
    assertThat(sut.readComponentDuplications(1).get(0).getOriginPosition()).isNotNull();
    assertThat(sut.readComponentDuplications(1).get(0).getDuplicateList()).hasSize(1);
  }

  @Test
  public void empty_list_if_no_duplication_found() throws Exception {
    assertThat(sut.readComponentDuplications(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_syntax_highlighting() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1).build());

    writer.writeComponentSyntaxHighlighting(1, Arrays.asList(
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(1)
          .setEndLine(10)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build()
      ));

    try (InputStream inputStream = FileUtils.openInputStream(sut.readComponentSyntaxHighlighting(1))) {
      BatchReport.SyntaxHighlighting syntaxHighlighting = BatchReport.SyntaxHighlighting.PARSER.parseDelimitedFrom(inputStream);
      assertThat(syntaxHighlighting.getRange()).isNotNull();
      assertThat(syntaxHighlighting.getRange().getStartLine()).isEqualTo(1);
      assertThat(syntaxHighlighting.getRange().getEndLine()).isEqualTo(10);
      assertThat(syntaxHighlighting.getType()).isEqualTo(Constants.HighlightingType.ANNOTATION);
    }
  }

  @Test
  public void return_null_if_no_highlighting_found() throws Exception {
    assertThat(sut.readComponentSyntaxHighlighting(UNKNOWN_COMPONENT_REF)).isNull();
  }

  @Test
  public void read_symbols() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1).build());

    writer.writeComponentSymbols(1, Arrays.asList(BatchReport.Symbols.Symbol.newBuilder()
      .setDeclaration(BatchReport.Range.newBuilder()
        .setStartLine(1)
        .setStartOffset(3)
        .setEndLine(1)
        .setEndOffset(5)
        .build())
      .addReference(BatchReport.Range.newBuilder()
        .setStartLine(10)
        .setStartOffset(15)
        .setEndLine(11)
        .setEndOffset(2)
        .build())
      .build()));

    sut = new BatchReportReader(dir);
    assertThat(sut.readComponentSymbols(1)).hasSize(1);
    assertThat(sut.readComponentSymbols(1).get(0).getDeclaration().getStartLine()).isEqualTo(1);
    assertThat(sut.readComponentSymbols(1).get(0).getReference(0).getStartLine()).isEqualTo(10);
  }

  @Test
  public void empty_list_if_no_symbol_found() throws Exception {
    assertThat(sut.readComponentSymbols(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

  @Test
  public void read_coverage() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeMetadata(BatchReport.Metadata.newBuilder()
      .setRootComponentRef(1)
      .build());
    writer.writeComponent(BatchReport.Component.newBuilder()
      .setRef(1).build());

    writer.writeComponentCoverage(1, Arrays.asList(
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

    sut = new BatchReportReader(dir);

    try (InputStream inputStream = FileUtils.openInputStream(new BatchReportReader(dir).readComponentCoverage(1))) {
      BatchReport.Coverage coverage = BatchReport.Coverage.PARSER.parseDelimitedFrom(inputStream);
      assertThat(coverage.getLine()).isEqualTo(1);
      assertThat(coverage.getConditions()).isEqualTo(1);
      assertThat(coverage.getUtHits()).isTrue();
      assertThat(coverage.getItHits()).isFalse();
      assertThat(coverage.getUtCoveredConditions()).isEqualTo(1);
      assertThat(coverage.getItCoveredConditions()).isEqualTo(1);
      assertThat(coverage.getOverallCoveredConditions()).isEqualTo(1);

      coverage = BatchReport.Coverage.PARSER.parseDelimitedFrom(inputStream);
      assertThat(coverage.getLine()).isEqualTo(2);
      assertThat(coverage.getConditions()).isEqualTo(5);
      assertThat(coverage.getUtHits()).isFalse();
      assertThat(coverage.getItHits()).isFalse();
      assertThat(coverage.getUtCoveredConditions()).isEqualTo(4);
      assertThat(coverage.getItCoveredConditions()).isEqualTo(5);
      assertThat(coverage.getOverallCoveredConditions()).isEqualTo(5);
    }
  }

  @Test
  public void return_null_if_no_coverage_found() throws Exception {
    assertThat(sut.readComponentCoverage(UNKNOWN_COMPONENT_REF)).isNull();
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
    writer.writeTests(1, Arrays.asList(
      BatchReport.Test.newBuilder()
        .setDurationInMs(60_000)
        .setStacktrace("stacktrace")
        .setMsg("message")
        .setStatus(Constants.TestStatus.OK)
        .build()));

    try (InputStream inputStream = FileUtils.openInputStream(sut.readTests(1))) {
      BatchReport.Test testResult = BatchReport.Test.PARSER.parseDelimitedFrom(inputStream);
      assertThat(testResult.getDurationInMs()).isEqualTo(60_000);
      assertThat(testResult.getStacktrace()).isEqualTo("stacktrace");
      assertThat(testResult.getMsg()).isEqualTo("message");
      assertThat(testResult.getStatus()).isEqualTo(Constants.TestStatus.OK);
    }
  }

  @Test
  public void null_if_no_test_found() throws Exception {
    assertThat(sut.readTests(UNKNOWN_COMPONENT_REF)).isNull();
  }

  @Test
  public void read_coverage_details() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeCoverageDetails(1, Arrays.asList(
      BatchReport.CoverageDetail.newBuilder()
        .setTestName("test-name")
        .addCoveredFile(BatchReport.CoverageDetail.CoveredFile.newBuilder()
          .addAllCoveredLine(Arrays.asList(1, 2, 3, 5, 7))
          .setFileRef(2)
        )
        .build()
      ));

    try (InputStream inputStream = FileUtils.openInputStream(sut.readCoverageDetails(1))) {
      BatchReport.CoverageDetail coverageDetail = BatchReport.CoverageDetail.PARSER.parseDelimitedFrom(inputStream);
      assertThat(coverageDetail.getTestName()).isEqualTo("test-name");
      assertThat(coverageDetail.getCoveredFile(0).getFileRef()).isEqualTo(2);
      assertThat(coverageDetail.getCoveredFile(0).getCoveredLineList()).containsExactly(1, 2, 3, 5, 7);
    }
  }

  @Test
  public void null_if_no_coverage_detail_found() throws Exception {
    assertThat(sut.readCoverageDetails(UNKNOWN_COMPONENT_REF)).isNull();
  }

  @Test
  public void read_file_dependencies() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeFileDependencies(1, Arrays.asList(
      BatchReport.FileDependency.newBuilder()
        .setToFileRef(5)
        .setWeight(20)
        .build()
      ));

    try (InputStream inputStream = FileUtils.openInputStream(sut.readFileDependencies(1))) {
      BatchReport.FileDependency fileDependency = BatchReport.FileDependency.PARSER.parseDelimitedFrom(inputStream);
      assertThat(fileDependency.getToFileRef()).isEqualTo(5);
      assertThat(fileDependency.getWeight()).isEqualTo(20);
    }
  }

  @Test
  public void null_if_no_file_dependencies_found() throws Exception {
    assertThat(sut.readFileDependencies(UNKNOWN_COMPONENT_REF)).isNull();
  }

  @Test
  public void read_module_dependencies() throws Exception {
    BatchReportWriter writer = new BatchReportWriter(dir);
    writer.writeModuleDependencies(1, Arrays.asList(BatchReport.ModuleDependencies.ModuleDependency.newBuilder()
      .setKey("PROJECT_1")
      .setScope("PRJ")
      .setVersion("1.1")
      .addChild(BatchReport.ModuleDependencies.ModuleDependency.newBuilder()
        .setKey("PROJECT_2")
        .setScope("PRJ")
        .setVersion("2.2")
        .build())
      .build()));

    assertThat(sut.readModuleDependencies(1)).hasSize(1);
    BatchReport.ModuleDependencies.ModuleDependency dependency = sut.readModuleDependencies(1).get(0);
    assertThat(dependency.getKey()).isEqualTo("PROJECT_1");
    assertThat(dependency.getScope()).isEqualTo("PRJ");
    assertThat(dependency.getVersion()).isEqualTo("1.1");
    assertThat(dependency.getChildList()).hasSize(1);
  }

  @Test
  public void empty_list_if_no_module_dependencies_found() throws Exception {
    assertThat(sut.readModuleDependencies(UNKNOWN_COMPONENT_REF)).isEmpty();
  }

}
