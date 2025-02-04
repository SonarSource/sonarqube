/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import com.google.protobuf.ByteString;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.Protobuf;
import org.sonar.scanner.protocol.internal.ScannerInternal.SensorCacheEntry;
import org.sonar.scanner.protocol.output.ScannerReport.Measure.StringValue;
import org.sonar.scanner.protocol.output.ScannerReport.SyntaxHighlightingRule.HighlightingType;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class ScannerReportReaderIT {

  private static final int UNKNOWN_COMPONENT_REF = 123;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private FileStructure fileStructure;
  private ScannerReportReader underTest;

  @Before
  public void setUp() throws Exception {
    File dir = temp.newFolder();
    fileStructure = new FileStructure(dir);
    underTest = new ScannerReportReader(fileStructure);
  }

  @Test
  public void read_metadata() {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
    ScannerReport.Metadata.Builder metadata = ScannerReport.Metadata.newBuilder()
      .setAnalysisDate(15000000L)
      .setProjectKey("PROJECT_A")
      .setRootComponentRef(1)
      .setCrossProjectDuplicationActivated(true);
    writer.writeMetadata(metadata.build());

    ScannerReport.Metadata readMetadata = underTest.readMetadata();
    assertThat(readMetadata.getAnalysisDate()).isEqualTo(15000000L);
    assertThat(readMetadata.getProjectKey()).isEqualTo("PROJECT_A");
    assertThat(readMetadata.getRootComponentRef()).isOne();
    assertThat(readMetadata.getCrossProjectDuplicationActivated()).isTrue();
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_metadata_file() {
    underTest.readMetadata();
  }

  @Test
  public void read_components() {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
    ScannerReport.Component.Builder component = ScannerReport.Component.newBuilder()
      .setRef(1)
      .setProjectRelativePath("src/main/java/Foo.java");
    writer.writeComponent(component.build());

    assertThat(underTest.readComponent(1).getProjectRelativePath()).isEqualTo("src/main/java/Foo.java");
  }

  @Test(expected = IllegalStateException.class)
  public void fail_if_missing_file_on_component() {
    underTest.readComponent(UNKNOWN_COMPONENT_REF);
  }

  @Test
  public void read_issues() {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
    ScannerReport.Issue issue = ScannerReport.Issue.newBuilder()
      .build();
    writer.writeComponentIssues(1, asList(issue));

    assertThat(underTest.readComponentIssues(1)).toIterable().hasSize(1);
    assertThat(underTest.readComponentIssues(200)).isExhausted();
  }

  @Test
  public void read_external_issues() {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
    ScannerReport.ExternalIssue issue = ScannerReport.ExternalIssue.newBuilder()
      .build();
    writer.appendComponentExternalIssue(1, issue);

    assertThat(underTest.readComponentExternalIssues(1)).toIterable().hasSize(1);
    assertThat(underTest.readComponentExternalIssues(200)).toIterable().isEmpty();
  }

  @Test
  public void empty_list_if_no_issue_found() {
    assertThat(underTest.readComponentIssues(UNKNOWN_COMPONENT_REF)).toIterable().isEmpty();
  }

  @Test
  public void read_measures() {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
    ScannerReport.Measure.Builder measure = ScannerReport.Measure.newBuilder()
      .setStringValue(StringValue.newBuilder().setValue("value_a"));
    writer.appendComponentMeasure(1, measure.build());

    assertThat(underTest.readComponentMeasures(1)).toIterable().hasSize(1);
  }

  @Test
  public void empty_list_if_no_measure_found() {
    assertThat(underTest.readComponentMeasures(UNKNOWN_COMPONENT_REF)).toIterable().isEmpty();
  }

  @Test
  public void read_changesets() {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
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
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
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

    ScannerReportReader sut = new ScannerReportReader(fileStructure);
    assertThat(sut.readComponentDuplications(1)).toIterable().hasSize(1);
  }

  @Test
  public void empty_list_if_no_duplication_found() {
    assertThat(underTest.readComponentDuplications(UNKNOWN_COMPONENT_REF)).toIterable().isEmpty();
  }

  @Test
  public void read_duplication_blocks() {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
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

    ScannerReportReader sut = new ScannerReportReader(fileStructure);
    assertThat(sut.readCpdTextBlocks(1)).toIterable().hasSize(1);
  }

  @Test
  public void read_analysis_cache() throws IOException {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);

    SensorCacheEntry entry1 = SensorCacheEntry.newBuilder()
      .setKey("key")
      .setData(ByteString.copyFrom("data", UTF_8))
      .build();
    SensorCacheEntry entry2 = SensorCacheEntry.newBuilder()
      .setKey("key")
      .setData(ByteString.copyFrom("data", UTF_8))
      .build();

    Protobuf.writeStream(List.of(entry1, entry2), fileStructure.analysisCache(), false);
    ScannerReportReader reader = new ScannerReportReader(fileStructure);

    CloseableIterator<SensorCacheEntry> it = Protobuf.readStream(reader.getAnalysisCache(), SensorCacheEntry.parser());
    List<SensorCacheEntry> data = new LinkedList<>();
    it.forEachRemaining(data::add);
    assertThat(data).containsExactly(entry1, entry2);
  }

  @Test
  public void read_analysis_cache_returns_null_if_no_file_exists() {
    ScannerReportReader reader = new ScannerReportReader(fileStructure);
    assertThat(reader.getAnalysisCache()).isNull();
  }

  @Test
  public void empty_list_if_no_duplication_block_found() {
    assertThat(underTest.readComponentDuplications(UNKNOWN_COMPONENT_REF)).toIterable().isEmpty();
  }

  @Test
  public void read_syntax_highlighting() {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
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
      assertThat(syntaxHighlighting.getRange().getStartLine()).isOne();
      assertThat(syntaxHighlighting.getRange().getEndLine()).isEqualTo(10);
      assertThat(syntaxHighlighting.getType()).isEqualTo(HighlightingType.ANNOTATION);
    }
  }

  @Test
  public void return_empty_if_no_highlighting_found() {
    assertThat(underTest.readComponentSyntaxHighlighting(UNKNOWN_COMPONENT_REF)).toIterable().isEmpty();
  }

  @Test
  public void read_symbols() {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
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

    underTest = new ScannerReportReader(fileStructure);
    assertThat(underTest.readComponentSymbols(1)).toIterable().hasSize(1);
  }

  @Test
  public void empty_list_if_no_symbol_found() {
    assertThat(underTest.readComponentSymbols(UNKNOWN_COMPONENT_REF)).toIterable().isEmpty();
  }

  @Test
  public void read_coverage() {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
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

    underTest = new ScannerReportReader(fileStructure);
    try (CloseableIterator<ScannerReport.LineCoverage> it = new ScannerReportReader(fileStructure).readComponentCoverage(1)) {
      ScannerReport.LineCoverage coverage = it.next();
      assertThat(coverage.getLine()).isOne();
      assertThat(coverage.getConditions()).isOne();
      assertThat(coverage.getHits()).isTrue();
      assertThat(coverage.getCoveredConditions()).isOne();
    }
  }

  @Test
  public void return_empty_iterator_if_no_coverage_found() {
    assertThat(underTest.readComponentCoverage(UNKNOWN_COMPONENT_REF)).toIterable().isEmpty();
  }

  @Test
  public void read_source_lines() throws Exception {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
    File file = writer.getFileStructure().fileFor(FileStructure.Domain.SOURCE, 1);
    FileUtils.writeLines(file, Lists.newArrayList("line1", "line2"));

    File sourceFile = new ScannerReportReader(fileStructure).readFileSource(1);
    assertThat(sourceFile).isEqualTo(file);
  }

  @Test
  public void read_file_source() throws Exception {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
    try (FileOutputStream outputStream = new FileOutputStream(writer.getSourceFile(1))) {
      IOUtils.write("line1\nline2", outputStream);
    }

    try (InputStream inputStream = FileUtils.openInputStream(underTest.readFileSource(1))) {
      assertThat(IOUtils.readLines(inputStream)).containsOnly("line1", "line2");
    }
  }

  @Test
  public void return_null_when_no_file_source() {
    assertThat(underTest.readFileSource(UNKNOWN_COMPONENT_REF)).isNull();
  }

  @Test
  public void readTelemetryEntries_whenFileExists() {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);
    ScannerReport.TelemetryEntry.Builder telemetry = ScannerReport.TelemetryEntry.newBuilder()
      .setKey("key")
      .setValue("value");
    writer.writeTelemetry(List.of(telemetry.build()));

    assertThat(underTest.readTelemetryEntries()).toIterable().hasSize(1);
  }

  @Test
  public void readTelemetryEntries_whenFileDoesntExists() {
    assertThat(underTest.readTelemetryEntries()).toIterable().isEmpty();
  }

  @Test
  public void readDependencyFilesZip_withNoFile_returnsNull() {
    assertThat(underTest.readDependencyFilesZip()).isNull();
  }

  @Test
  public void readDependencyFilesZip_withFile_returnsFile() throws IOException {
    ScannerReportWriter writer = new ScannerReportWriter(fileStructure);

    temp.create();
    File tempFile = temp.newFile("dependency-files.zip");
    byte[] expectedBytes = "hello world!".getBytes();
    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
      fos.write(expectedBytes);
    }

    writer.writeScaFile(tempFile);

    assertThat(underTest.readDependencyFilesZip()).isNotNull();
    var returnBytes = FileUtils.readFileToByteArray(underTest.readDependencyFilesZip());
    assertThat(returnBytes).isEqualTo(expectedBytes);
  }
}
