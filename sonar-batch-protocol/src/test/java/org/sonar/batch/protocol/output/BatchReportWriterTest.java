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

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.ProtobufUtil;
import org.sonar.batch.protocol.output.BatchReport.Range;

import java.io.File;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class BatchReportWriterTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();
  File dir;
  BatchReportWriter sut;

  @Before
  public void setUp() throws Exception {
    dir = temp.newFolder();
    sut = new BatchReportWriter(dir);
  }

  @Test
  public void create_dir_if_does_not_exist() throws Exception {
    FileUtils.deleteQuietly(dir);
    sut = new BatchReportWriter(dir);

    assertThat(dir).isDirectory().exists();
  }

  @Test
  public void write_metadata() throws Exception {
    BatchReport.Metadata.Builder metadata = BatchReport.Metadata.newBuilder()
      .setAnalysisDate(15000000L)
      .setProjectKey("PROJECT_A")
      .setRootComponentRef(1);
    sut.writeMetadata(metadata.build());

    BatchReport.Metadata read = ProtobufUtil.readFile(sut.getFileStructure().metadataFile(), BatchReport.Metadata.PARSER);
    assertThat(read.getAnalysisDate()).isEqualTo(15000000L);
    assertThat(read.getProjectKey()).isEqualTo("PROJECT_A");
    assertThat(read.getRootComponentRef()).isEqualTo(1);
  }

  @Test
  public void write_component() throws Exception {
    // no data yet
    assertThat(sut.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isFalse();

    // write data
    BatchReport.Component.Builder component = BatchReport.Component.newBuilder()
      .setRef(1)
      .setLanguage("java")
      .setPath("src/Foo.java")
      .setUuid("UUID_A")
      .setType(Constants.ComponentType.FILE)
      .setIsTest(false)
      .addChildRef(5)
      .addChildRef(42);
    sut.writeComponent(component.build());

    assertThat(sut.hasComponentData(FileStructure.Domain.COMPONENT, 1)).isTrue();
    File file = sut.getFileStructure().fileFor(FileStructure.Domain.COMPONENT, 1);
    assertThat(file).exists().isFile();
    BatchReport.Component read = ProtobufUtil.readFile(file, BatchReport.Component.PARSER);
    assertThat(read.getRef()).isEqualTo(1);
    assertThat(read.getChildRefList()).containsOnly(5, 42);
    assertThat(read.hasName()).isFalse();
    assertThat(read.getIsTest()).isFalse();
    assertThat(read.getUuid()).isEqualTo("UUID_A");
  }

  @Test
  public void write_issues() throws Exception {
    // no data yet
    assertThat(sut.hasComponentData(FileStructure.Domain.ISSUES, 1)).isFalse();

    // write data
    BatchReport.Issue issue = BatchReport.Issue.newBuilder()
      .setUuid("ISSUE_A")
      .setLine(50)
      .setMsg("the message")
      .build();

    sut.writeComponentIssues(1, Arrays.asList(issue));

    assertThat(sut.hasComponentData(FileStructure.Domain.ISSUES, 1)).isTrue();
    File file = sut.getFileStructure().fileFor(FileStructure.Domain.ISSUES, 1);
    assertThat(file).exists().isFile();
    BatchReport.Issues read = ProtobufUtil.readFile(file, BatchReport.Issues.PARSER);
    assertThat(read.getComponentRef()).isEqualTo(1);
    assertThat(read.hasComponentUuid()).isFalse();
    assertThat(read.getIssueCount()).isEqualTo(1);
  }

  @Test
  public void write_issues_of_deleted_component() throws Exception {
    // no data yet
    assertThat(sut.hasComponentData(FileStructure.Domain.ISSUES_ON_DELETED, 1)).isFalse();

    // write data
    BatchReport.Issue issue = BatchReport.Issue.newBuilder()
      .setUuid("ISSUE_A")
      .setLine(50)
      .setMsg("the message")
      .build();

    sut.writeDeletedComponentIssues(1, "componentUuid", Arrays.asList(issue));

    assertThat(sut.hasComponentData(FileStructure.Domain.ISSUES_ON_DELETED, 1)).isTrue();
    File file = sut.getFileStructure().fileFor(FileStructure.Domain.ISSUES_ON_DELETED, 1);
    assertThat(file).exists().isFile();
    BatchReport.Issues read = ProtobufUtil.readFile(file, BatchReport.Issues.PARSER);
    assertThat(read.getComponentRef()).isEqualTo(1);
    assertThat(read.getComponentUuid()).isEqualTo("componentUuid");
    assertThat(read.getIssueCount()).isEqualTo(1);
  }

  @Test
  public void write_measures() throws Exception {
    assertThat(sut.hasComponentData(FileStructure.Domain.MEASURES, 1)).isFalse();

    BatchReport.Measure measure = BatchReport.Measure.newBuilder()
      .setStringValue("text-value")
      .setDoubleValue(2.5d)
      .setValueType(Constants.MeasureValueType.DOUBLE)
      .setDescription("description")
      .build();

    sut.writeComponentMeasures(1, Arrays.asList(measure));

    assertThat(sut.hasComponentData(FileStructure.Domain.MEASURES, 1)).isTrue();
    File file = sut.getFileStructure().fileFor(FileStructure.Domain.MEASURES, 1);
    assertThat(file).exists().isFile();
    BatchReport.Measures measures = ProtobufUtil.readFile(file, BatchReport.Measures.PARSER);
    assertThat(measures.getComponentRef()).isEqualTo(1);
    assertThat(measures.getMeasureCount()).isEqualTo(1);
    assertThat(measures.getMeasure(0).getStringValue()).isEqualTo("text-value");
    assertThat(measures.getMeasure(0).getDoubleValue()).isEqualTo(2.5d);
    assertThat(measures.getMeasure(0).getValueType()).isEqualTo(Constants.MeasureValueType.DOUBLE);
    assertThat(measures.getMeasure(0).getDescription()).isEqualTo("description");
  }

  @Test
  public void write_scm() throws Exception {
    assertThat(sut.hasComponentData(FileStructure.Domain.CHANGESETS, 1)).isFalse();

    BatchReport.Changesets scm = BatchReport.Changesets.newBuilder()
      .setComponentRef(1)
      .addChangesetIndexByLine(0)
      .addChangeset(BatchReport.Changesets.Changeset.newBuilder()
        .setRevision("123-456-789")
        .setAuthor("author")
        .setDate(123_456_789L))
      .build();

    sut.writeComponentChangesets(scm);

    assertThat(sut.hasComponentData(FileStructure.Domain.CHANGESETS, 1)).isTrue();
    File file = sut.getFileStructure().fileFor(FileStructure.Domain.CHANGESETS, 1);
    assertThat(file).exists().isFile();
    BatchReport.Changesets read = ProtobufUtil.readFile(file, BatchReport.Changesets.PARSER);
    assertThat(read.getComponentRef()).isEqualTo(1);
    assertThat(read.getChangesetCount()).isEqualTo(1);
    assertThat(read.getChangesetList()).hasSize(1);
    assertThat(read.getChangeset(0).getDate()).isEqualTo(123_456_789L);
  }

  @Test
  public void write_duplications() throws Exception {
    assertThat(sut.hasComponentData(FileStructure.Domain.DUPLICATIONS, 1)).isFalse();

    BatchReport.Duplication duplication = BatchReport.Duplication.newBuilder()
      .setOriginPosition(Range.newBuilder()
        .setStartLine(1)
        .setEndLine(5)
        .build())
      .addDuplicate(BatchReport.Duplicate.newBuilder()
        .setOtherFileKey("COMPONENT_A")
        .setOtherFileRef(2)
        .setRange(Range.newBuilder()
          .setStartLine(6)
          .setEndLine(10)
          .build())
        .build())
      .build();
    sut.writeComponentDuplications(1, Arrays.asList(duplication));

    assertThat(sut.hasComponentData(FileStructure.Domain.DUPLICATIONS, 1)).isTrue();
    File file = sut.getFileStructure().fileFor(FileStructure.Domain.DUPLICATIONS, 1);
    assertThat(file).exists().isFile();
    BatchReport.Duplications duplications = ProtobufUtil.readFile(file, BatchReport.Duplications.PARSER);
    assertThat(duplications.getComponentRef()).isEqualTo(1);
    assertThat(duplications.getDuplicationList()).hasSize(1);
    assertThat(duplications.getDuplication(0).getOriginPosition()).isNotNull();
    assertThat(duplications.getDuplication(0).getDuplicateList()).hasSize(1);
  }

  @Test
  public void write_symbols() throws Exception {
    // no data yet
    assertThat(sut.hasComponentData(FileStructure.Domain.SYMBOLS, 1)).isFalse();

    // write data
    BatchReport.Symbols.Symbol symbol = BatchReport.Symbols.Symbol.newBuilder()
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
      .build();

    sut.writeComponentSymbols(1, Arrays.asList(symbol));

    assertThat(sut.hasComponentData(FileStructure.Domain.SYMBOLS, 1)).isTrue();

    File file = sut.getFileStructure().fileFor(FileStructure.Domain.SYMBOLS, 1);
    assertThat(file).exists().isFile();
    BatchReport.Symbols read = ProtobufUtil.readFile(file, BatchReport.Symbols.PARSER);
    assertThat(read.getFileRef()).isEqualTo(1);
    assertThat(read.getSymbolList()).hasSize(1);
    assertThat(read.getSymbol(0).getDeclaration().getStartLine()).isEqualTo(1);
    assertThat(read.getSymbol(0).getReference(0).getStartLine()).isEqualTo(10);
  }

  @Test
  public void write_syntax_highlighting() throws Exception {
    // no data yet
    assertThat(sut.hasComponentData(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, 1)).isFalse();

    sut.writeComponentSyntaxHighlighting(1, Arrays.asList(
      BatchReport.SyntaxHighlighting.newBuilder()
        .setRange(BatchReport.Range.newBuilder()
          .setStartLine(1)
          .setEndLine(1)
          .build())
        .setType(Constants.HighlightingType.ANNOTATION)
        .build()
    ));

    assertThat(sut.hasComponentData(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, 1)).isTrue();
  }

  @Test
  public void write_coverage() throws Exception {
    // no data yet
    assertThat(sut.hasComponentData(FileStructure.Domain.COVERAGES, 1)).isFalse();

    sut.writeComponentCoverage(1, Arrays.asList(
      BatchReport.Coverage.newBuilder()
        .setLine(1)
        .setConditions(1)
        .setUtHits(true)
        .setItHits(false)
        .setUtCoveredConditions(1)
        .setItCoveredConditions(1)
        .setOverallCoveredConditions(1)
        .build()
    ));

    assertThat(sut.hasComponentData(FileStructure.Domain.COVERAGES, 1)).isTrue();
  }

  @Test
  public void write_tests() throws Exception {
    assertThat(sut.hasComponentData(FileStructure.Domain.TESTS, 1)).isFalse();

    sut.writeTests(1, Arrays.asList(
      BatchReport.Test.getDefaultInstance()
    ));

    assertThat(sut.hasComponentData(FileStructure.Domain.TESTS, 1)).isTrue();

  }

  @Test
  public void write_coverage_details() throws Exception {
    assertThat(sut.hasComponentData(FileStructure.Domain.COVERAGE_DETAILS, 1)).isFalse();

    sut.writeCoverageDetails(1, Arrays.asList(
      BatchReport.CoverageDetail.getDefaultInstance()
    ));

    assertThat(sut.hasComponentData(FileStructure.Domain.COVERAGE_DETAILS, 1)).isTrue();
  }

  @Test
  public void write_file_dependencies() throws Exception {
    assertThat(sut.hasComponentData(FileStructure.Domain.FILE_DEPENDENCIES, 1)).isFalse();

    sut.writeFileDependencies(1, Arrays.asList(BatchReport.FileDependency.getDefaultInstance()));

    assertThat(sut.hasComponentData(FileStructure.Domain.FILE_DEPENDENCIES, 1)).isTrue();
  }

  @Test
  public void write_module_dependencies() throws Exception {
    assertThat(sut.hasComponentData(FileStructure.Domain.MODULE_DEPENDENCIES, 1)).isFalse();

    sut.writeModuleDependencies(1, Arrays.asList(BatchReport.ModuleDependencies.ModuleDependency.getDefaultInstance()));

    assertThat(sut.hasComponentData(FileStructure.Domain.MODULE_DEPENDENCIES, 1)).isTrue();
  }
}
