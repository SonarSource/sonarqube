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

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.LineSgnificantCode;

public class BatchReportReaderRule implements TestRule, BatchReportReader {
  private ScannerReport.Metadata metadata;
  private List<String> scannerLogs;
  private List<ScannerReport.ActiveRule> activeRules = new ArrayList<>();
  private List<ScannerReport.ContextProperty> contextProperties = new ArrayList<>();
  private Map<Integer, List<ScannerReport.Measure>> measures = new HashMap<>();
  private Map<Integer, ScannerReport.Changesets> changesets = new HashMap<>();
  private Map<Integer, ScannerReport.Component> components = new HashMap<>();
  private Map<Integer, List<ScannerReport.Issue>> issues = new HashMap<>();
  private Map<Integer, List<ScannerReport.ExternalIssue>> externalIssues = new HashMap<>();
  private List<ScannerReport.AdHocRule> adHocRules = new ArrayList<>();
  private Map<Integer, List<ScannerReport.Duplication>> duplications = new HashMap<>();
  private Map<Integer, List<ScannerReport.CpdTextBlock>> duplicationBlocks = new HashMap<>();
  private Map<Integer, List<ScannerReport.Symbol>> symbols = new HashMap<>();
  private Map<Integer, List<ScannerReport.SyntaxHighlightingRule>> syntaxHighlightings = new HashMap<>();
  private Map<Integer, List<ScannerReport.LineCoverage>> coverages = new HashMap<>();
  private Map<Integer, List<String>> fileSources = new HashMap<>();
  private Map<Integer, List<ScannerReport.LineSgnificantCode>> significantCode = new HashMap<>();
  private Map<Integer, ScannerReport.ChangedLines> changedLines = new HashMap<>();
  private List<ScannerReport.AnalysisWarning> analysisWarnings = Collections.emptyList();

  @Override
  public Statement apply(final Statement statement, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          statement.evaluate();
        } finally {
          clear();
        }
      }
    };
  }

  private void clear() {
    this.metadata = null;
    this.scannerLogs = null;
    this.measures.clear();
    this.changesets.clear();
    this.components.clear();
    this.issues.clear();
    this.duplications.clear();
    this.duplicationBlocks.clear();
    this.symbols.clear();
    this.syntaxHighlightings.clear();
    this.coverages.clear();
    this.fileSources.clear();
    this.significantCode.clear();
  }

  @Override
  public CloseableIterator<ScannerReport.ContextProperty> readContextProperties() {
    return CloseableIterator.from(contextProperties.iterator());
  }

  public BatchReportReaderRule putContextProperties(List<ScannerReport.ContextProperty> contextProperties) {
    this.contextProperties = Objects.requireNonNull(contextProperties);
    return this;
  }

  @Override
  public ScannerReport.Metadata readMetadata() {
    if (metadata == null) {
      throw new IllegalStateException("Metadata is missing");
    }
    return metadata;
  }

  public BatchReportReaderRule setMetadata(ScannerReport.Metadata metadata) {
    this.metadata = metadata;
    return this;
  }

  @Override
  public CloseableIterator<String> readScannerLogs() {
    if (scannerLogs == null) {
      throw new IllegalStateException("Scanner logs are missing");
    }
    return CloseableIterator.from(scannerLogs.iterator());
  }

  public BatchReportReaderRule setScannerLogs(@Nullable List<String> logs) {
    this.scannerLogs = logs;
    return this;
  }

  @Override
  public CloseableIterator<ScannerReport.ActiveRule> readActiveRules() {
    if (activeRules == null) {
      throw new IllegalStateException("Active rules are not set");
    }
    return CloseableIterator.from(activeRules.iterator());
  }

  public BatchReportReaderRule putActiveRules(List<ScannerReport.ActiveRule> activeRules) {
    this.activeRules = activeRules;
    return this;
  }

  @Override
  public CloseableIterator<ScannerReport.Measure> readComponentMeasures(int componentRef) {
    return closeableIterator(this.measures.get(componentRef));
  }

  public BatchReportReaderRule putMeasures(int componentRef, List<ScannerReport.Measure> measures) {
    this.measures.put(componentRef, measures);
    return this;
  }

  @Override
  @CheckForNull
  public ScannerReport.Changesets readChangesets(int componentRef) {
    return changesets.get(componentRef);
  }

  public BatchReportReaderRule putChangesets(ScannerReport.Changesets changesets) {
    this.changesets.put(changesets.getComponentRef(), changesets);
    return this;
  }

  @Override
  public ScannerReport.Component readComponent(int componentRef) {
    return components.get(componentRef);
  }

  public BatchReportReaderRule putComponent(ScannerReport.Component component) {
    this.components.put(component.getRef(), component);
    return this;
  }

  @Override
  public CloseableIterator<ScannerReport.Issue> readComponentIssues(int componentRef) {
    return closeableIterator(issues.get(componentRef));
  }

  @Override
  public CloseableIterator<ScannerReport.ExternalIssue> readComponentExternalIssues(int componentRef) {
    return closeableIterator(externalIssues.get(componentRef));
  }

  @Override
  public CloseableIterator<ScannerReport.AdHocRule> readAdHocRules() {
    return closeableIterator(adHocRules);
  }

  public BatchReportReaderRule putAdHocRules(List<ScannerReport.AdHocRule> adHocRules) {
    this.adHocRules = adHocRules;
    return this;
  }

  public BatchReportReaderRule putIssues(int componentRef, List<ScannerReport.Issue> issues) {
    this.issues.put(componentRef, issues);
    return this;
  }

  public BatchReportReaderRule putExternalIssues(int componentRef, List<ScannerReport.ExternalIssue> externalIssues) {
    this.externalIssues.put(componentRef, externalIssues);
    return this;
  }

  @Override
  public CloseableIterator<ScannerReport.Duplication> readComponentDuplications(int componentRef) {
    return closeableIterator(this.duplications.get(componentRef));
  }

  public BatchReportReaderRule putDuplications(int componentRef, ScannerReport.Duplication... duplications) {
    this.duplications.put(componentRef, Arrays.asList(duplications));
    return this;
  }

  @Override
  public CloseableIterator<ScannerReport.CpdTextBlock> readCpdTextBlocks(int componentRef) {
    return closeableIterator(this.duplicationBlocks.get(componentRef));
  }

  public BatchReportReaderRule putDuplicationBlocks(int componentRef, List<ScannerReport.CpdTextBlock> duplicationBlocks) {
    this.duplicationBlocks.put(componentRef, duplicationBlocks);
    return this;
  }

  @Override
  public CloseableIterator<ScannerReport.Symbol> readComponentSymbols(int componentRef) {
    return closeableIterator(this.symbols.get(componentRef));
  }

  private static <T> CloseableIterator<T> closeableIterator(@Nullable List<T> list) {
    return list == null ? CloseableIterator.emptyCloseableIterator() : CloseableIterator.from(list.iterator());
  }

  public BatchReportReaderRule putSymbols(int componentRef, List<ScannerReport.Symbol> symbols) {
    this.symbols.put(componentRef, symbols);
    return this;
  }

  public BatchReportReaderRule putSignificantCode(int fileRef, List<ScannerReport.LineSgnificantCode> significantCode) {
    this.significantCode.put(fileRef, significantCode);
    return this;
  }

  @Override
  public Optional<CloseableIterator<LineSgnificantCode>> readComponentSignificantCode(int fileRef) {
    List<LineSgnificantCode> list = significantCode.get(fileRef);
    return list == null ? Optional.empty() : Optional.of(CloseableIterator.from(list.iterator()));
  }

  public BatchReportReaderRule putChangedLines(int fileRef, ScannerReport.ChangedLines fileChangedLines) {
    changedLines.put(fileRef, fileChangedLines);
    return this;
  }

  @Override
  public Optional<ScannerReport.ChangedLines> readComponentChangedLines(int fileRef) {
    return Optional.ofNullable(changedLines.get(fileRef));
  }

  @Override
  public CloseableIterator<ScannerReport.AnalysisWarning> readAnalysisWarnings() {
    return closeableIterator(analysisWarnings);
  }

  public BatchReportReaderRule setAnalysisWarnings(List<ScannerReport.AnalysisWarning> analysisWarnings) {
    this.analysisWarnings = new ArrayList<>(analysisWarnings);
    return this;
  }

  @Override
  public CloseableIterator<ScannerReport.SyntaxHighlightingRule> readComponentSyntaxHighlighting(int fileRef) {
    return closeableIterator(this.syntaxHighlightings.get(fileRef));
  }

  public BatchReportReaderRule putSyntaxHighlighting(int fileRef, List<ScannerReport.SyntaxHighlightingRule> syntaxHighlightings) {
    this.syntaxHighlightings.put(fileRef, syntaxHighlightings);
    return this;
  }

  @Override
  public CloseableIterator<ScannerReport.LineCoverage> readComponentCoverage(int fileRef) {
    return closeableIterator(this.coverages.get(fileRef));
  }

  public BatchReportReaderRule putCoverage(int fileRef, List<ScannerReport.LineCoverage> coverages) {
    this.coverages.put(fileRef, coverages);
    return this;
  }

  @Override
  public Optional<CloseableIterator<String>> readFileSource(int fileRef) {
    List<String> lines = fileSources.get(fileRef);
    if (lines == null) {
      return Optional.empty();
    }

    return Optional.of(CloseableIterator.from(lines.iterator()));
  }

  public BatchReportReaderRule putFileSourceLines(int fileRef, @Nullable String... lines) {
    Preconditions.checkNotNull(lines);
    this.fileSources.put(fileRef, Arrays.asList(lines));
    return this;
  }

  public BatchReportReaderRule putFileSourceLines(int fileRef, List<String> lines) {
    this.fileSources.put(fileRef, lines);
    return this;
  }

}
