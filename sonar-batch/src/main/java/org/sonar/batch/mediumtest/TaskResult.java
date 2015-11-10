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
package org.sonar.batch.mediumtest;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputDir;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.issue.tracking.TrackedIssue;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.Component;
import org.sonar.batch.protocol.output.BatchReport.Metadata;
import org.sonar.batch.protocol.output.BatchReport.Symbol;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.report.BatchReportUtils;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.batch.scan.ProjectScanContainer;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.core.util.CloseableIterator;

public class TaskResult implements org.sonar.batch.mediumtest.ScanTaskObserver {

  private static final Logger LOG = LoggerFactory.getLogger(TaskResult.class);

  private List<TrackedIssue> issues = new ArrayList<>();
  private Map<String, InputFile> inputFiles = new HashMap<>();
  private Map<String, Component> reportComponents = new HashMap<>();
  private Map<String, InputDir> inputDirs = new HashMap<>();
  private BatchReportReader reader;

  @Override
  public void scanTaskCompleted(ProjectScanContainer container) {
    LOG.info("Store analysis results in memory for later assertions in medium test");
    for (TrackedIssue issue : container.getComponentByType(IssueCache.class).all()) {
      issues.add(issue);
    }

    ReportPublisher reportPublisher = container.getComponentByType(ReportPublisher.class);
    reader = new BatchReportReader(reportPublisher.getReportDir());
    if (!container.getComponentByType(AnalysisMode.class).isIssues()) {
      Metadata readMetadata = getReportReader().readMetadata();
      int rootComponentRef = readMetadata.getRootComponentRef();
      storeReportComponents(rootComponentRef, null, readMetadata.hasBranch() ? readMetadata.getBranch() : null);
    }

    storeFs(container);

  }

  private void storeReportComponents(int componentRef, String parentModuleKey, @Nullable String branch) {
    Component component = getReportReader().readComponent(componentRef);
    if (component.hasKey()) {
      reportComponents.put(component.getKey() + (branch != null ? ":" + branch : ""), component);
    } else {
      reportComponents.put(parentModuleKey + (branch != null ? ":" + branch : "") + ":" + component.getPath(), component);
    }
    for (int childId : component.getChildRefList()) {
      storeReportComponents(childId, component.hasKey() ? component.getKey() : parentModuleKey, branch);
    }

  }

  public BatchReportReader getReportReader() {
    return reader;
  }

  private void storeFs(ProjectScanContainer container) {
    InputPathCache inputFileCache = container.getComponentByType(InputPathCache.class);
    for (InputFile inputPath : inputFileCache.allFiles()) {
      inputFiles.put(inputPath.relativePath(), inputPath);
    }
    for (InputDir inputPath : inputFileCache.allDirs()) {
      inputDirs.put(inputPath.relativePath(), inputPath);
    }
  }

  public List<TrackedIssue> trackedIssues() {
    return issues;
  }

  public Component getReportComponent(String key) {
    return reportComponents.get(key);
  }

  public List<BatchReport.Issue> issuesFor(InputPath inputPath) {
    List<BatchReport.Issue> result = Lists.newArrayList();
    int ref = reportComponents.get(key(inputPath)).getRef();
    try (CloseableIterator<BatchReport.Issue> it = reader.readComponentIssues(ref)) {
      while (it.hasNext()) {
        result.add(it.next());
      }
    } catch (Exception e) {
      throw new IllegalStateException("Can't read issues for " + inputPath.absolutePath(), e);
    }
    return result;
  }

  private static String key(InputPath inputPath) {
    return inputPath instanceof InputFile ? ((DefaultInputFile) inputPath).key() : ((DefaultInputDir) inputPath).key();
  }

  public Collection<InputFile> inputFiles() {
    return inputFiles.values();
  }

  @CheckForNull
  public InputFile inputFile(String relativePath) {
    return inputFiles.get(relativePath);
  }

  public Collection<InputDir> inputDirs() {
    return inputDirs.values();
  }

  @CheckForNull
  public InputDir inputDir(String relativePath) {
    return inputDirs.get(relativePath);
  }

  public Map<String, List<BatchReport.Measure>> allMeasures() {
    Map<String, List<BatchReport.Measure>> result = new HashMap<>();
    for (Map.Entry<String, Component> component : reportComponents.entrySet()) {
      List<BatchReport.Measure> measures = new ArrayList<>();
      try (CloseableIterator<BatchReport.Measure> it = reader.readComponentMeasures(component.getValue().getRef())) {
        Iterators.addAll(measures, it);
      }
      result.put(component.getKey(), measures);
    }
    return result;
  }

  /**
   * Get highlighting types at a given position in an inputfile
   * @param lineOffset 0-based offset in file
   */
  public List<TypeOfText> highlightingTypeFor(InputFile file, int line, int lineOffset) {
    int ref = reportComponents.get(((DefaultInputFile) file).key()).getRef();
    if (!reader.hasSyntaxHighlighting(ref)) {
      return Collections.emptyList();
    }
    TextPointer pointer = file.newPointer(line, lineOffset);
    List<TypeOfText> result = new ArrayList<>();
    try (CloseableIterator<BatchReport.SyntaxHighlighting> it = reader.readComponentSyntaxHighlighting(ref)) {
      while (it.hasNext()) {
        BatchReport.SyntaxHighlighting rule = it.next();
        TextRange ruleRange = toRange(file, rule.getRange());
        if (ruleRange.start().compareTo(pointer) <= 0 && ruleRange.end().compareTo(pointer) > 0) {
          result.add(BatchReportUtils.toBatchType(rule.getType()));
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Can't read syntax highlighting for " + file.absolutePath(), e);
    }
    return result;
  }

  private static TextRange toRange(InputFile file, BatchReport.TextRange reportRange) {
    return file.newRange(file.newPointer(reportRange.getStartLine(), reportRange.getStartOffset()), file.newPointer(reportRange.getEndLine(), reportRange.getEndOffset()));
  }

  /**
   * Get list of all start positions of a symbol in an inputfile
   * @param symbolStartLine 0-based start offset for the symbol in file
   * @param symbolStartLineOffset 0-based end offset for the symbol in file
   */
  @CheckForNull
  public List<BatchReport.TextRange> symbolReferencesFor(InputFile file, int symbolStartLine, int symbolStartLineOffset) {
    int ref = reportComponents.get(((DefaultInputFile) file).key()).getRef();
    try (CloseableIterator<Symbol> symbols = getReportReader().readComponentSymbols(ref)) {
      while (symbols.hasNext()) {
        Symbol symbol = symbols.next();
        if (symbol.getDeclaration().getStartLine() == symbolStartLine && symbol.getDeclaration().getStartOffset() == symbolStartLineOffset) {
          return symbol.getReferenceList();
        }
      }
    }
    return Collections.emptyList();
  }

  public List<BatchReport.Duplication> duplicationsFor(InputFile file) {
    List<BatchReport.Duplication> result = new ArrayList<>();
    int ref = reportComponents.get(((DefaultInputFile) file).key()).getRef();
    try (CloseableIterator<BatchReport.Duplication> it = getReportReader().readComponentDuplications(ref)) {
      while (it.hasNext()) {
        result.add(it.next());
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return result;
  }

  public List<BatchReport.CpdTextBlock> duplicationBlocksFor(InputFile file) {
    List<BatchReport.CpdTextBlock> result = new ArrayList<>();
    int ref = reportComponents.get(((DefaultInputFile) file).key()).getRef();
    try (CloseableIterator<BatchReport.CpdTextBlock> it = getReportReader().readCpdTextBlocks(ref)) {
      while (it.hasNext()) {
        result.add(it.next());
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return result;
  }

  @CheckForNull
  public BatchReport.Coverage coverageFor(InputFile file, int line) {
    int ref = reportComponents.get(((DefaultInputFile) file).key()).getRef();
    try (CloseableIterator<BatchReport.Coverage> it = getReportReader().readComponentCoverage(ref)) {
      while (it.hasNext()) {
        BatchReport.Coverage coverage = it.next();
        if (coverage.getLine() == line) {
          return coverage;
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return null;
  }

  public BatchReport.Test testExecutionFor(InputFile testFile, String testName) {
    int ref = reportComponents.get(((DefaultInputFile) testFile).key()).getRef();
    try (InputStream inputStream = FileUtils.openInputStream(getReportReader().readTests(ref))) {
      BatchReport.Test test = BatchReport.Test.PARSER.parseDelimitedFrom(inputStream);
      while (test != null) {
        if (test.getName().equals(testName)) {
          return test;
        }
        test = BatchReport.Test.PARSER.parseDelimitedFrom(inputStream);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return null;
  }

  public BatchReport.CoverageDetail coveragePerTestFor(InputFile testFile, String testName) {
    int ref = reportComponents.get(((DefaultInputFile) testFile).key()).getRef();
    try (InputStream inputStream = FileUtils.openInputStream(getReportReader().readCoverageDetails(ref))) {
      BatchReport.CoverageDetail details = BatchReport.CoverageDetail.PARSER.parseDelimitedFrom(inputStream);
      while (details != null) {
        if (details.getTestName().equals(testName)) {
          return details;
        }
        details = BatchReport.CoverageDetail.PARSER.parseDelimitedFrom(inputStream);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return null;
  }
}
