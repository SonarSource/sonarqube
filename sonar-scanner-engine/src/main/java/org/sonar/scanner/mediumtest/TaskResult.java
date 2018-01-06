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
package org.sonar.scanner.mediumtest;

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
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.issue.IssueCache;
import org.sonar.scanner.issue.tracking.TrackedIssue;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Component;
import org.sonar.scanner.protocol.output.ScannerReport.Metadata;
import org.sonar.scanner.protocol.output.ScannerReport.Symbol;
import org.sonar.scanner.protocol.output.ScannerReportReader;
import org.sonar.scanner.report.ReportPublisher;
import org.sonar.scanner.report.ScannerReportUtils;
import org.sonar.scanner.scan.ProjectScanContainer;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

public class TaskResult implements org.sonar.scanner.mediumtest.ScanTaskObserver {

  private static final Logger LOG = LoggerFactory.getLogger(TaskResult.class);

  private List<TrackedIssue> issues = new ArrayList<>();
  private Map<String, InputFile> inputFiles = new HashMap<>();
  private Map<String, Component> reportComponents = new HashMap<>();
  private Map<String, InputDir> inputDirs = new HashMap<>();
  private ScannerReportReader reader;

  @Override
  public void scanTaskCompleted(ProjectScanContainer container) {
    LOG.info("Store analysis results in memory for later assertions in medium test");
    for (TrackedIssue issue : container.getComponentByType(IssueCache.class).all()) {
      issues.add(issue);
    }

    ReportPublisher reportPublisher = container.getComponentByType(ReportPublisher.class);
    reader = new ScannerReportReader(reportPublisher.getReportDir().toFile());
    if (!container.getComponentByType(AnalysisMode.class).isIssues()) {
      Metadata readMetadata = getReportReader().readMetadata();
      int rootComponentRef = readMetadata.getRootComponentRef();
      storeReportComponents(rootComponentRef, null);
    }

    storeFs(container);

  }

  private void storeReportComponents(int componentRef, String parentModuleKey) {
    Component component = getReportReader().readComponent(componentRef);
    if (isNotEmpty(component.getKey())) {
      reportComponents.put(component.getKey(), component);
    } else {
      reportComponents.put(parentModuleKey + ":" + component.getPath(), component);
    }
    for (int childId : component.getChildRefList()) {
      storeReportComponents(childId, isNotEmpty(component.getKey()) ? component.getKey() : parentModuleKey);
    }

  }

  public ScannerReportReader getReportReader() {
    return reader;
  }

  private void storeFs(ProjectScanContainer container) {
    InputComponentStore inputFileCache = container.getComponentByType(InputComponentStore.class);
    for (InputFile inputPath : inputFileCache.allFiles()) {
      inputFiles.put(((DefaultInputFile) inputPath).getProjectRelativePath(), inputPath);
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

  public List<ScannerReport.Issue> issuesFor(InputComponent inputComponent) {
    int ref = reportComponents.get(inputComponent.key()).getRef();
    return issuesFor(ref);
  }

  public List<ScannerReport.Issue> issuesFor(Component reportComponent) {
    int ref = reportComponent.getRef();
    return issuesFor(ref);
  }

  private List<ScannerReport.Issue> issuesFor(int ref) {
    List<ScannerReport.Issue> result = Lists.newArrayList();
    try (CloseableIterator<ScannerReport.Issue> it = reader.readComponentIssues(ref)) {
      while (it.hasNext()) {
        result.add(it.next());
      }
    }
    return result;
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

  public Map<String, List<ScannerReport.Measure>> allMeasures() {
    Map<String, List<ScannerReport.Measure>> result = new HashMap<>();
    for (Map.Entry<String, Component> component : reportComponents.entrySet()) {
      List<ScannerReport.Measure> measures = new ArrayList<>();
      try (CloseableIterator<ScannerReport.Measure> it = reader.readComponentMeasures(component.getValue().getRef())) {
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
    int ref = reportComponents.get(file.key()).getRef();
    if (!reader.hasSyntaxHighlighting(ref)) {
      return Collections.emptyList();
    }
    TextPointer pointer = file.newPointer(line, lineOffset);
    List<TypeOfText> result = new ArrayList<>();
    try (CloseableIterator<ScannerReport.SyntaxHighlightingRule> it = reader.readComponentSyntaxHighlighting(ref)) {
      while (it.hasNext()) {
        ScannerReport.SyntaxHighlightingRule rule = it.next();
        TextRange ruleRange = toRange(file, rule.getRange());
        if (ruleRange.start().compareTo(pointer) <= 0 && ruleRange.end().compareTo(pointer) > 0) {
          result.add(ScannerReportUtils.toBatchType(rule.getType()));
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("Can't read syntax highlighting for " + file, e);
    }
    return result;
  }

  private static TextRange toRange(InputFile file, ScannerReport.TextRange reportRange) {
    return file.newRange(file.newPointer(reportRange.getStartLine(), reportRange.getStartOffset()), file.newPointer(reportRange.getEndLine(), reportRange.getEndOffset()));
  }

  /**
   * Get list of all start positions of a symbol in an inputfile
   * @param symbolStartLine 0-based start offset for the symbol in file
   * @param symbolStartLineOffset 0-based end offset for the symbol in file
   */
  @CheckForNull
  public List<ScannerReport.TextRange> symbolReferencesFor(InputFile file, int symbolStartLine, int symbolStartLineOffset) {
    int ref = reportComponents.get(file.key()).getRef();
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

  public List<ScannerReport.Duplication> duplicationsFor(InputFile file) {
    List<ScannerReport.Duplication> result = new ArrayList<>();
    int ref = reportComponents.get(file.key()).getRef();
    try (CloseableIterator<ScannerReport.Duplication> it = getReportReader().readComponentDuplications(ref)) {
      while (it.hasNext()) {
        result.add(it.next());
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return result;
  }

  public List<ScannerReport.CpdTextBlock> duplicationBlocksFor(InputFile file) {
    List<ScannerReport.CpdTextBlock> result = new ArrayList<>();
    int ref = reportComponents.get(file.key()).getRef();
    try (CloseableIterator<ScannerReport.CpdTextBlock> it = getReportReader().readCpdTextBlocks(ref)) {
      while (it.hasNext()) {
        result.add(it.next());
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return result;
  }

  @CheckForNull
  public ScannerReport.LineCoverage coverageFor(InputFile file, int line) {
    int ref = reportComponents.get(file.key()).getRef();
    try (CloseableIterator<ScannerReport.LineCoverage> it = getReportReader().readComponentCoverage(ref)) {
      while (it.hasNext()) {
        ScannerReport.LineCoverage coverage = it.next();
        if (coverage.getLine() == line) {
          return coverage;
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return null;
  }

  public ScannerReport.Test firstTestExecutionForName(InputFile testFile, String testName) {
    int ref = reportComponents.get(testFile.key()).getRef();
    try (InputStream inputStream = FileUtils.openInputStream(getReportReader().readTests(ref))) {
      ScannerReport.Test test = ScannerReport.Test.parser().parseDelimitedFrom(inputStream);
      while (test != null) {
        if (test.getName().equals(testName)) {
          return test;
        }
        test = ScannerReport.Test.parser().parseDelimitedFrom(inputStream);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return null;
  }

  public ScannerReport.CoverageDetail coveragePerTestFor(InputFile testFile, String testName) {
    int ref = reportComponents.get(testFile.key()).getRef();
    try (InputStream inputStream = FileUtils.openInputStream(getReportReader().readCoverageDetails(ref))) {
      ScannerReport.CoverageDetail details = ScannerReport.CoverageDetail.parser().parseDelimitedFrom(inputStream);
      while (details != null) {
        if (details.getTestName().equals(testName)) {
          return details;
        }
        details = ScannerReport.CoverageDetail.parser().parseDelimitedFrom(inputStream);
      }
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    return null;
  }
}
