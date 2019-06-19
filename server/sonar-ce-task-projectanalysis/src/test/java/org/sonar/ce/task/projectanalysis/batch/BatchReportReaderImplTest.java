/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.batch;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.JUnitTempFolder;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReportWriter;

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;

public class BatchReportReaderImplTest {
  private static final int COMPONENT_REF = 1;
  private static final ScannerReport.Changesets CHANGESETS = ScannerReport.Changesets.newBuilder().setComponentRef(COMPONENT_REF).build();
  private static final ScannerReport.Measure MEASURE = ScannerReport.Measure.newBuilder().build();
  private static final ScannerReport.Component COMPONENT = ScannerReport.Component.newBuilder().setRef(COMPONENT_REF).build();
  private static final ScannerReport.Issue ISSUE = ScannerReport.Issue.newBuilder().build();
  private static final ScannerReport.Duplication DUPLICATION = ScannerReport.Duplication.newBuilder().build();
  private static final ScannerReport.CpdTextBlock DUPLICATION_BLOCK = ScannerReport.CpdTextBlock.newBuilder().build();
  private static final ScannerReport.Symbol SYMBOL = ScannerReport.Symbol.newBuilder().build();
  private static final ScannerReport.SyntaxHighlightingRule SYNTAX_HIGHLIGHTING_1 = ScannerReport.SyntaxHighlightingRule.newBuilder().build();
  private static final ScannerReport.SyntaxHighlightingRule SYNTAX_HIGHLIGHTING_2 = ScannerReport.SyntaxHighlightingRule.newBuilder().build();
  private static final ScannerReport.LineCoverage COVERAGE_1 = ScannerReport.LineCoverage.newBuilder().build();
  private static final ScannerReport.LineCoverage COVERAGE_2 = ScannerReport.LineCoverage.newBuilder().build();

  @Rule
  public JUnitTempFolder tempFolder = new JUnitTempFolder();

  private ScannerReportWriter writer;
  private BatchReportReaderImpl underTest;

  @Before
  public void setUp() {
    BatchReportDirectoryHolder holder = new ImmutableBatchReportDirectoryHolder(tempFolder.newDir());
    underTest = new BatchReportReaderImpl(holder);
    writer = new ScannerReportWriter(holder.getDirectory());
  }

  @Test(expected = IllegalStateException.class)
  public void readMetadata_throws_ISE_if_no_metadata() {
    underTest.readMetadata();
  }

  @Test
  public void readMetadata_result_is_cached() {
    ScannerReport.Metadata metadata = ScannerReport.Metadata.newBuilder().build();

    writer.writeMetadata(metadata);

    ScannerReport.Metadata res = underTest.readMetadata();
    assertThat(res).isEqualTo(metadata);
    assertThat(underTest.readMetadata()).isSameAs(res);
  }

  @Test
  public void readScannerLogs() throws IOException {
    File scannerLogFile = writer.getFileStructure().analysisLog();
    FileUtils.write(scannerLogFile, "log1\nlog2");

    CloseableIterator<String> logs = underTest.readScannerLogs();
    assertThat(logs).toIterable().containsExactly("log1", "log2");
  }

  @Test
  public void readScannerLogs_no_logs() {
    CloseableIterator<String> logs = underTest.readScannerLogs();
    assertThat(logs.hasNext()).isFalse();
  }

  @Test
  public void readComponentMeasures_returns_empty_list_if_there_is_no_measure() {
    assertThat(underTest.readComponentMeasures(COMPONENT_REF)).isExhausted();
  }

  @Test
  public void verify_readComponentMeasures_returns_measures() {
    writer.appendComponentMeasure(COMPONENT_REF, MEASURE);

    try (CloseableIterator<ScannerReport.Measure> measures = underTest.readComponentMeasures(COMPONENT_REF)) {
      assertThat(measures.next()).isEqualTo(MEASURE);
      assertThat(measures.hasNext()).isFalse();
    }
  }

  @Test
  public void readComponentMeasures_is_not_cached() {
    writer.appendComponentMeasure(COMPONENT_REF, MEASURE);

    assertThat(underTest.readComponentMeasures(COMPONENT_REF)).isNotSameAs(underTest.readComponentMeasures(COMPONENT_REF));
  }

  @Test
  public void readChangesets_returns_null_if_no_changeset() {
    assertThat(underTest.readChangesets(COMPONENT_REF)).isNull();
  }

  @Test
  public void verify_readChangesets_returns_changesets() {
    writer.writeComponentChangesets(CHANGESETS);

    ScannerReport.Changesets res = underTest.readChangesets(COMPONENT_REF);
    assertThat(res).isEqualTo(CHANGESETS);
  }

  @Test
  public void readChangesets_is_not_cached() {
    writer.writeComponentChangesets(CHANGESETS);

    assertThat(underTest.readChangesets(COMPONENT_REF)).isNotSameAs(underTest.readChangesets(COMPONENT_REF));
  }

  @Test(expected = IllegalStateException.class)
  public void readComponent_throws_ISE_if_file_does_not_exist() {
    underTest.readComponent(COMPONENT_REF);
  }

  @Test
  public void verify_readComponent_returns_Component() {
    writer.writeComponent(COMPONENT);

    assertThat(underTest.readComponent(COMPONENT_REF)).isEqualTo(COMPONENT);
  }

  @Test
  public void readComponent_is_not_cached() {
    writer.writeComponent(COMPONENT);

    assertThat(underTest.readComponent(COMPONENT_REF)).isNotSameAs(underTest.readComponent(COMPONENT_REF));
  }

  @Test
  public void readComponentIssues_returns_empty_list_if_file_does_not_exist() {
    assertThat(underTest.readComponentIssues(COMPONENT_REF)).isExhausted();
  }

  @Test
  public void verify_readComponentIssues_returns_Issues() {
    writer.writeComponentIssues(COMPONENT_REF, of(ISSUE));

    try (CloseableIterator<ScannerReport.Issue> res = underTest.readComponentIssues(COMPONENT_REF)) {
      assertThat(res.next()).isEqualTo(ISSUE);
      assertThat(res.hasNext()).isFalse();
    }
  }

  @Test
  public void readComponentIssues_it_not_cached() {
    writer.writeComponentIssues(COMPONENT_REF, of(ISSUE));

    assertThat(underTest.readComponentIssues(COMPONENT_REF)).isNotSameAs(underTest.readComponentIssues(COMPONENT_REF));
  }

  @Test
  public void readComponentDuplications_returns_empty_list_if_file_does_not_exist() {
    assertThat(underTest.readComponentDuplications(COMPONENT_REF)).isExhausted();
  }

  @Test
  public void verify_readComponentDuplications_returns_Issues() {
    writer.writeComponentDuplications(COMPONENT_REF, of(DUPLICATION));

    try (CloseableIterator<ScannerReport.Duplication> res = underTest.readComponentDuplications(COMPONENT_REF)) {
      assertThat(res.next()).isEqualTo(DUPLICATION);
      assertThat(res.hasNext()).isFalse();
    }
  }

  @Test
  public void readComponentDuplications_it_not_cached() {
    writer.writeComponentDuplications(COMPONENT_REF, of(DUPLICATION));

    assertThat(underTest.readComponentDuplications(COMPONENT_REF)).isNotSameAs(underTest.readComponentDuplications(COMPONENT_REF));
  }

  @Test
  public void readComponentDuplicationBlocks_returns_empty_list_if_file_does_not_exist() {
    assertThat(underTest.readCpdTextBlocks(COMPONENT_REF)).isExhausted();
  }

  @Test
  public void verify_readComponentDuplicationBlocks_returns_Issues() {
    writer.writeCpdTextBlocks(COMPONENT_REF, of(DUPLICATION_BLOCK));

    try (CloseableIterator<ScannerReport.CpdTextBlock> res = underTest.readCpdTextBlocks(COMPONENT_REF)) {
      assertThat(res.next()).isEqualTo(DUPLICATION_BLOCK);
      assertThat(res.hasNext()).isFalse();
    }
  }

  @Test
  public void readComponentDuplicationBlocks_is_not_cached() {
    writer.writeCpdTextBlocks(COMPONENT_REF, of(DUPLICATION_BLOCK));

    assertThat(underTest.readCpdTextBlocks(COMPONENT_REF)).isNotSameAs(underTest.readCpdTextBlocks(COMPONENT_REF));
  }

  @Test
  public void readComponentSymbols_returns_empty_list_if_file_does_not_exist() {
    assertThat(underTest.readComponentSymbols(COMPONENT_REF)).isExhausted();
  }

  @Test
  public void verify_readComponentSymbols_returns_Issues() {
    writer.writeComponentSymbols(COMPONENT_REF, of(SYMBOL));

    try (CloseableIterator<ScannerReport.Symbol> res = underTest.readComponentSymbols(COMPONENT_REF)) {
      assertThat(res.next()).isEqualTo(SYMBOL);
      assertThat(res.hasNext()).isFalse();
    }
  }

  @Test
  public void readComponentSymbols_it_not_cached() {
    writer.writeComponentSymbols(COMPONENT_REF, of(SYMBOL));

    assertThat(underTest.readComponentSymbols(COMPONENT_REF)).isNotSameAs(underTest.readComponentSymbols(COMPONENT_REF));
  }

  @Test
  public void readComponentSyntaxHighlighting_returns_empty_CloseableIterator_when_file_does_not_exist() {
    assertThat(underTest.readComponentSyntaxHighlighting(COMPONENT_REF)).isExhausted();
  }

  @Test
  public void verify_readComponentSyntaxHighlighting() {
    writer.writeComponentSyntaxHighlighting(COMPONENT_REF, of(SYNTAX_HIGHLIGHTING_1, SYNTAX_HIGHLIGHTING_2));

    CloseableIterator<ScannerReport.SyntaxHighlightingRule> res = underTest.readComponentSyntaxHighlighting(COMPONENT_REF);
    assertThat(res).toIterable().containsExactly(SYNTAX_HIGHLIGHTING_1, SYNTAX_HIGHLIGHTING_2);
    res.close();
  }

  @Test
  public void readComponentCoverage_returns_empty_CloseableIterator_when_file_does_not_exist() {
    assertThat(underTest.readComponentCoverage(COMPONENT_REF)).isExhausted();
  }

  @Test
  public void verify_readComponentCoverage() {
    writer.writeComponentCoverage(COMPONENT_REF, of(COVERAGE_1, COVERAGE_2));

    CloseableIterator<ScannerReport.LineCoverage> res = underTest.readComponentCoverage(COMPONENT_REF);
    assertThat(res).toIterable().containsExactly(COVERAGE_1, COVERAGE_2);
    res.close();
  }

  @Test
  public void readFileSource_returns_absent_optional_when_file_does_not_exist() {
    assertThat(underTest.readFileSource(COMPONENT_REF)).isEmpty();
  }

  @Test
  public void verify_readFileSource() throws IOException {
    File file = writer.getSourceFile(COMPONENT_REF);
    FileUtils.writeLines(file, of("1", "2", "3"));

    CloseableIterator<String> res = underTest.readFileSource(COMPONENT_REF).get();
    assertThat(res).toIterable().containsExactly("1", "2", "3");
    res.close();
  }

  @Test
  public void verify_readAnalysisWarnings() {
    ScannerReport.AnalysisWarning warning1 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 1").build();
    ScannerReport.AnalysisWarning warning2 = ScannerReport.AnalysisWarning.newBuilder().setText("warning 2").build();
    ImmutableList<ScannerReport.AnalysisWarning> warnings = of(warning1, warning2);
    writer.writeAnalysisWarnings(warnings);

    CloseableIterator<ScannerReport.AnalysisWarning> res = underTest.readAnalysisWarnings();
    assertThat(res).toIterable().containsExactlyElementsOf(warnings);
    res.close();
  }
}
