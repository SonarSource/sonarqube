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
package org.sonar.server.computation.batch;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.util.CloseableIterator;

public class BatchReportReaderRule implements TestRule, BatchReportReader {
  private BatchReport.Metadata metadata;
  private List<String> scannerLogs;
  private List<BatchReport.ActiveRule> activeRules = new ArrayList<>();
  private Map<Integer, List<BatchReport.Measure>> measures = new HashMap<>();
  private Map<Integer, BatchReport.Changesets> changesets = new HashMap<>();
  private Map<Integer, BatchReport.Component> components = new HashMap<>();
  private Map<Integer, List<BatchReport.Issue>> issues = new HashMap<>();
  private Map<Integer, List<BatchReport.Duplication>> duplications = new HashMap<>();
  private Map<Integer, List<BatchReport.CpdTextBlock>> duplicationBlocks = new HashMap<>();
  private Map<Integer, List<BatchReport.Symbol>> symbols = new HashMap<>();
  private Map<Integer, List<BatchReport.SyntaxHighlighting>> syntaxHighlightings = new HashMap<>();
  private Map<Integer, List<BatchReport.Coverage>> coverages = new HashMap<>();
  private Map<Integer, List<String>> fileSources = new HashMap<>();
  private Map<Integer, List<BatchReport.Test>> tests = new HashMap<>();
  private Map<Integer, List<BatchReport.CoverageDetail>> coverageDetails = new HashMap<>();

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
    this.tests.clear();
    this.coverageDetails.clear();
  }

  @Override
  public BatchReport.Metadata readMetadata() {
    if (metadata == null) {
      throw new IllegalStateException("Metadata is missing");
    }
    return metadata;
  }

  public BatchReportReaderRule setMetadata(BatchReport.Metadata metadata) {
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
  public CloseableIterator<BatchReport.ActiveRule> readActiveRules() {
    if (activeRules == null) {
      throw new IllegalStateException("Active rules are not set");
    }
    return CloseableIterator.from(activeRules.iterator());
  }

  public BatchReportReaderRule putActiveRules(List<BatchReport.ActiveRule> activeRules) {
    this.activeRules = activeRules;
    return this;
  }

  @Override
  public CloseableIterator<BatchReport.Measure> readComponentMeasures(int componentRef) {
    List<BatchReport.Measure> res = this.measures.get(componentRef);
    if (res == null) {
      return CloseableIterator.emptyCloseableIterator();
    }
    return CloseableIterator.from(res.iterator());
  }

  public BatchReportReaderRule putMeasures(int componentRef, List<BatchReport.Measure> measures) {
    this.measures.put(componentRef, measures);
    return this;
  }

  @Override
  @CheckForNull
  public BatchReport.Changesets readChangesets(int componentRef) {
    return changesets.get(componentRef);
  }

  public BatchReportReaderRule putChangesets(BatchReport.Changesets changesets) {
    this.changesets.put(changesets.getComponentRef(), changesets);
    return this;
  }

  @Override
  public BatchReport.Component readComponent(int componentRef) {
    return components.get(componentRef);
  }

  public BatchReportReaderRule putComponent(BatchReport.Component component) {
    this.components.put(component.getRef(), component);
    return this;
  }

  @Override
  public CloseableIterator<BatchReport.Issue> readComponentIssues(int componentRef) {
    return closeableIterator(issues.get(componentRef));
  }

  public BatchReportReaderRule putIssues(int componentRef, List<BatchReport.Issue> issue) {
    this.issues.put(componentRef, issue);
    return this;
  }

  @Override
  public CloseableIterator<BatchReport.Duplication> readComponentDuplications(int componentRef) {
    return closeableIterator(this.duplications.get(componentRef));
  }

  public BatchReportReaderRule putDuplications(int componentRef, BatchReport.Duplication... duplications) {
    this.duplications.put(componentRef, Arrays.asList(duplications));
    return this;
  }

  @Override
  public CloseableIterator<BatchReport.CpdTextBlock> readCpdTextBlocks(int componentRef) {
    return closeableIterator(this.duplicationBlocks.get(componentRef));
  }

  public BatchReportReaderRule putDuplicationBlocks(int componentRef, List<BatchReport.CpdTextBlock> duplicationBlocks) {
    this.duplicationBlocks.put(componentRef, duplicationBlocks);
    return this;
  }

  @Override
  public CloseableIterator<BatchReport.Symbol> readComponentSymbols(int componentRef) {
    return closeableIterator(this.symbols.get(componentRef));
  }

  private static <T> CloseableIterator<T> closeableIterator(@Nullable List<T> list) {
    return list == null ? CloseableIterator.<T>emptyCloseableIterator() : CloseableIterator.from(list.iterator());
  }

  public BatchReportReaderRule putSymbols(int componentRef, List<BatchReport.Symbol> symbols) {
    this.symbols.put(componentRef, symbols);
    return this;
  }

  @Override
  public CloseableIterator<BatchReport.SyntaxHighlighting> readComponentSyntaxHighlighting(int fileRef) {
    List<BatchReport.SyntaxHighlighting> res = this.syntaxHighlightings.get(fileRef);
    if (res == null) {
      return CloseableIterator.emptyCloseableIterator();
    }

    return CloseableIterator.from(res.iterator());
  }

  public BatchReportReaderRule putSyntaxHighlighting(int fileRef, List<BatchReport.SyntaxHighlighting> syntaxHighlightings) {
    this.syntaxHighlightings.put(fileRef, syntaxHighlightings);
    return this;
  }

  @Override
  public CloseableIterator<BatchReport.Coverage> readComponentCoverage(int fileRef) {
    List<BatchReport.Coverage> res = this.coverages.get(fileRef);
    if (res == null) {
      return CloseableIterator.emptyCloseableIterator();
    }

    return CloseableIterator.from(res.iterator());
  }

  public BatchReportReaderRule putCoverage(int fileRef, List<BatchReport.Coverage> coverages) {
    this.coverages.put(fileRef, coverages);
    return this;
  }

  @Override
  public Optional<CloseableIterator<String>> readFileSource(int fileRef) {
    List<String> lines = fileSources.get(fileRef);
    if (lines == null) {
      return Optional.absent();
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

  @Override
  public CloseableIterator<BatchReport.Test> readTests(int testFileRef) {
    List<BatchReport.Test> res = this.tests.get(testFileRef);
    if (res == null) {
      return CloseableIterator.emptyCloseableIterator();
    }

    return CloseableIterator.from(res.iterator());
  }

  public BatchReportReaderRule putTests(int testFileRed, List<BatchReport.Test> tests) {
    this.tests.put(testFileRed, tests);
    return this;
  }

  @Override
  public CloseableIterator<BatchReport.CoverageDetail> readCoverageDetails(int testFileRef) {
    List<BatchReport.CoverageDetail> res = this.coverageDetails.get(testFileRef);
    if (res == null) {
      return CloseableIterator.emptyCloseableIterator();
    }

    return CloseableIterator.from(res.iterator());
  }

  public BatchReportReaderRule putCoverageDetails(int testFileRef, List<BatchReport.CoverageDetail> coverageDetails) {
    this.coverageDetails.put(testFileRef, coverageDetails);
    return this;
  }
}
