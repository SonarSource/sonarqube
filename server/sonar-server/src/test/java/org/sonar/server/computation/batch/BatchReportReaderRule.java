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

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.sonar.batch.protocol.output.BatchReport;

public class BatchReportReaderRule implements TestRule, BatchReportReader {
  private BatchReport.Metadata metadata;
  private Map<Integer, List<BatchReport.Measure>> measures = new HashMap<>();
  private Map<Integer, BatchReport.Changesets> changesets = new HashMap<>();
  private Map<Integer, BatchReport.Component> components = new HashMap<>();
  private Map<Integer, List<BatchReport.Issue>> issues = new HashMap<>();
  private Map<Integer, BatchReport.Issues> deletedIssues = new HashMap<>();
  private Map<Integer, List<BatchReport.Duplication>> duplications = new HashMap<>();
  private Map<Integer, List<BatchReport.Symbols.Symbol>> symbols = new HashMap<>();
  private Map<Integer, File> syntaxHighlightings = new HashMap<>();
  private Map<Integer, File> coverages = new HashMap<>();
  private Map<Integer, File> fileSources = new HashMap<>();
  private Map<Integer, File> tests = new HashMap<>();
  private Map<Integer, File> coverageDetails = new HashMap<>();

  @Override
  public Statement apply(final Statement statement, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          statement.evaluate();
        }
        finally {
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
    this.deletedIssues.clear();
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
    return measures.get(componentRef);
  }

  public void putMeasures(int componentRef, List<BatchReport.Measure> measures) {
    this.measures.put(componentRef, measures);
  }

  @Override
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
  public BatchReport.Issues readDeletedComponentIssues(int deletedComponentRef) {
    BatchReport.Issues issues = this.deletedIssues.get(deletedComponentRef);
    if (issues == null) {
      throw new IllegalStateException("Unable to issues for deleted component #" + deletedComponentRef);
    }
    return issues;
  }

  public void putDeletedIssues(int componentRef, BatchReport.Issues issues) {
    this.deletedIssues.put(componentRef, issues);
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
  public boolean hasSyntaxHighlighting(int componentRef) {
    File file = syntaxHighlightings.get(componentRef);
    return file != null && file.exists();
  }

  @Override
  public File readComponentSyntaxHighlighting(int fileRef) {
    return syntaxHighlightings.get(fileRef);
  }

  public void putSyntaxHighlighting(int fileRef, File file) {
    this.syntaxHighlightings.put(fileRef, file);
  }

  @Override
  public File readComponentCoverage(int fileRef) {
    return coverages.get(fileRef);
  }

  public void putCoverage(int fileRef, File file) {
    this.coverages.put(fileRef, file);
  }

  @Override
  public File readFileSource(int fileRef) {
    File file = fileSources.get(fileRef);
    if (file == null) {
      throw new IllegalStateException("Unable to find source for file #" + fileRef + ". File does not exist: " + file);
    }
    return file;
  }

  public void putFileSoure(int fileRef, File file) {
    this.fileSources.put(fileRef, file);
  }

  @Override
  public File readTests(int testFileRef) {
    return tests.get(testFileRef);
  }

  public void putTests(int testFileRed, File file) {
    this.tests.put(testFileRed, file);
  }

  @Override
  public File readCoverageDetails(int testFileRef) {
    return coverageDetails.get(testFileRef);
  }

  public void putCoverageDetails(int testFileRef, File file) {
    this.coverageDetails.put(testFileRef, file);
  }
}
