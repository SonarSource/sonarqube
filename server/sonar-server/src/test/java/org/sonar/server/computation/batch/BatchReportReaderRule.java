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

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.server.util.CloseableIterator;

public class BatchReportReaderRule implements TestRule, BatchReportReader {
  private BatchReport.Metadata metadata;
  private Map<Integer, List<BatchReport.Measure>> measures = new HashMap<>();
  private Map<Integer, BatchReport.Changesets> changesets = new HashMap<>();
  private Map<Integer, BatchReport.Component> components = new HashMap<>();
  private Map<Integer, List<BatchReport.Issue>> issues = new HashMap<>();
  private Map<Integer, List<BatchReport.Duplication>> duplications = new HashMap<>();
  private Map<Integer, List<BatchReport.Symbols.Symbol>> symbols = new HashMap<>();
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
    this.measures.clear();
    this.changesets.clear();
    this.components.clear();
    this.issues.clear();
    this.duplications.clear();
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

  public void setMetadata(BatchReport.Metadata metadata) {
    this.metadata = metadata;
  }

  @Override
  public List<BatchReport.Measure> readComponentMeasures(int componentRef) {
    List<BatchReport.Measure> res = this.measures.get(componentRef);
    if (res == null) {
      return Collections.emptyList();
    }
    return res;
  }

  public void putMeasures(int componentRef, List<BatchReport.Measure> measures) {
    this.measures.put(componentRef, measures);
  }

  @Override
  @CheckForNull
  public BatchReport.Changesets readChangesets(int componentRef) {
    return changesets.get(componentRef);
  }

  public void putChangesets(BatchReport.Changesets changesets) {
    this.changesets.put(changesets.getComponentRef(), changesets);
  }

  @Override
  public BatchReport.Component readComponent(int componentRef) {
    return components.get(componentRef);
  }

  public void putComponent(BatchReport.Component component) {
    this.components.put(component.getRef(), component);
  }

  @Override
  public List<BatchReport.Issue> readComponentIssues(int componentRef) {
    return nonNull(issues.get(componentRef));
  }

  public void putIssues(int componentRef, List<BatchReport.Issue> issue) {
    this.issues.put(componentRef, issue);
  }

  @Override
  public List<BatchReport.Duplication> readComponentDuplications(int componentRef) {
    return nonNull(this.duplications.get(componentRef));
  }

  public void putDuplications(int componentRef, List<BatchReport.Duplication> duplications) {
    this.duplications.put(componentRef, duplications);
  }

  @Override
  public List<BatchReport.Symbols.Symbol> readComponentSymbols(int componentRef) {
    return nonNull(this.symbols.get(componentRef));
  }

  private static <T> List<T> nonNull(@Nullable List<T> symbols) {
    return symbols == null ? Collections.<T>emptyList() : symbols;
  }

  public void putSymbols(int componentRef, List<BatchReport.Symbols.Symbol> symbols) {
    this.symbols.put(componentRef, symbols);
  }

  @Override
  public CloseableIterator<BatchReport.SyntaxHighlighting> readComponentSyntaxHighlighting(int fileRef) {
    List<BatchReport.SyntaxHighlighting> res = this.syntaxHighlightings.get(fileRef);
    if (res == null) {
      return CloseableIterator.emptyCloseableIterator();
    }

    return CloseableIterator.from(res.iterator());
  }

  public void putSyntaxHighlighting(int fileRef, List<BatchReport.SyntaxHighlighting> syntaxHighlightings) {
    this.syntaxHighlightings.put(fileRef, syntaxHighlightings);
  }

  @Override
  public CloseableIterator<BatchReport.Coverage> readComponentCoverage(int fileRef) {
    List<BatchReport.Coverage> res = this.coverages.get(fileRef);
    if (res == null) {
      return CloseableIterator.emptyCloseableIterator();
    }

    return CloseableIterator.from(res.iterator());
  }

  public void putCoverage(int fileRef, List<BatchReport.Coverage> coverages) {
    this.coverages.put(fileRef, coverages);
  }

  @Override
  public CloseableIterator<String> readFileSource(int fileRef) {
    List<String> lines = fileSources.get(fileRef);
    if (lines == null) {
      throw new IllegalStateException("Unable to find source for file #" + fileRef + ". File does not exist: " + lines);
    }

    return CloseableIterator.from(lines.iterator());
  }

  public void putFileSourceLines(int fileRef, @Nullable String... lines) {
    Preconditions.checkNotNull(lines);
    this.fileSources.put(fileRef, Arrays.asList(lines));
  }

  public void putFileSourceLines(int fileRef, List<String> lines) {
    this.fileSources.put(fileRef, lines);
  }

  @Override
  public CloseableIterator<BatchReport.Test> readTests(int testFileRef) {
    List<BatchReport.Test> res = this.tests.get(testFileRef);
    if (res == null) {
      return CloseableIterator.emptyCloseableIterator();
    }

    return CloseableIterator.from(res.iterator());
  }

  public void putTests(int testFileRed, List<BatchReport.Test> tests) {
    this.tests.put(testFileRed, tests);
  }

  @Override
  public CloseableIterator<BatchReport.CoverageDetail> readCoverageDetails(int testFileRef) {
    List<BatchReport.CoverageDetail> res = this.coverageDetails.get(testFileRef);
    if (res == null) {
      return CloseableIterator.emptyCloseableIterator();
    }

    return CloseableIterator.from(res.iterator());
  }

  public void putCoverageDetails(int testFileRef, List<BatchReport.CoverageDetail> coverageDetails) {
    this.coverageDetails.put(testFileRef, coverageDetails);
  }
}
