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

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.AnalysisMode;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextPointer;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.sensor.dependency.internal.DefaultDependency;
import org.sonar.api.batch.sensor.duplication.Duplication;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.measures.Measure;
import org.sonar.batch.dependency.DependencyCache;
import org.sonar.batch.duplication.DuplicationCache;
import org.sonar.batch.index.Cache.Entry;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.protocol.output.*;
import org.sonar.batch.protocol.output.BatchReport.Component;
import org.sonar.batch.protocol.output.BatchReport.Metadata;
import org.sonar.batch.protocol.output.BatchReport.Range;
import org.sonar.batch.protocol.output.BatchReport.Symbols.Symbol;
import org.sonar.batch.report.BatchReportUtils;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.batch.scan.ProjectScanContainer;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.batch.scan.measure.MeasureCache;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;

public class TaskResult implements org.sonar.batch.mediumtest.ScanTaskObserver {

  private static final Logger LOG = LoggerFactory.getLogger(TaskResult.class);

  private List<Issue> issues = new ArrayList<>();
  private List<org.sonar.api.batch.sensor.measure.Measure> measures = new ArrayList<>();
  private Map<String, List<Duplication>> duplications = new HashMap<>();
  private Map<String, InputFile> inputFiles = new HashMap<>();
  private Map<String, Component> reportComponents = new HashMap<>();
  private Map<String, InputDir> inputDirs = new HashMap<>();
  private Map<String, Map<String, Integer>> dependencies = new HashMap<>();
  private BatchReportReader reader;

  @Override
  public void scanTaskCompleted(ProjectScanContainer container) {
    LOG.info("Store analysis results in memory for later assertions in medium test");
    for (DefaultIssue issue : container.getComponentByType(IssueCache.class).all()) {
      issues.add(issue);
    }

    if (!container.getComponentByType(AnalysisMode.class).isPreview()) {
      ReportPublisher reportPublisher = container.getComponentByType(ReportPublisher.class);
      reader = new BatchReportReader(reportPublisher.getReportDir());
      Metadata readMetadata = getReportReader().readMetadata();
      int rootComponentRef = readMetadata.getRootComponentRef();
      storeReportComponents(rootComponentRef, null, readMetadata.hasBranch() ? readMetadata.getBranch() : null);
    }

    storeFs(container);

    storeMeasures(container);

    storeDuplication(container);
    storeDependencies(container);
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

  private void storeMeasures(ProjectScanContainer container) {
    InputPathCache inputFileCache = container.getComponentByType(InputPathCache.class);
    for (Entry<Measure> measureEntry : container.getComponentByType(MeasureCache.class).entries()) {
      String componentKey = measureEntry.key()[0].toString();
      InputFile file = inputFileCache.getFile(StringUtils.substringBeforeLast(componentKey, ":"), StringUtils.substringAfterLast(componentKey, ":"));
      Measure oldMeasure = measureEntry.value();
      DefaultMeasure<Serializable> newMeasure = new DefaultMeasure<>().forMetric(oldMeasure.getMetric());
      if (file != null) {
        newMeasure.onFile(file);
      } else {
        newMeasure.onProject();
      }
      newMeasure.withValue(oldMeasure.value());
      measures.add(newMeasure);
    }
  }

  private void storeDuplication(ProjectScanContainer container) {
    DuplicationCache duplicationCache = container.getComponentByType(DuplicationCache.class);
    for (String effectiveKey : duplicationCache.componentKeys()) {
      duplications.put(effectiveKey, Lists.<Duplication>newArrayList(duplicationCache.byComponent(effectiveKey)));
    }
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

  private void storeDependencies(ProjectScanContainer container) {
    DependencyCache dependencyCache = container.getComponentByType(DependencyCache.class);
    for (Entry<DefaultDependency> entry : dependencyCache.entries()) {
      String fromKey = entry.key()[1].toString();
      String toKey = entry.key()[2].toString();
      if (!dependencies.containsKey(fromKey)) {
        dependencies.put(fromKey, new HashMap<String, Integer>());
      }
      dependencies.get(fromKey).put(toKey, entry.value().weight());
    }
  }

  public List<Issue> issues() {
    return issues;
  }

  public List<org.sonar.api.batch.sensor.measure.Measure> measures() {
    return measures;
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

  public List<Duplication> duplicationsFor(InputFile inputFile) {
    return duplications.get(((DefaultInputFile) inputFile).key());
  }

  /**
   * Get highlighting types at a given position in an inputfile
   * @param lineOffset 0-based offset in file
   */
  public List<TypeOfText> highlightingTypeFor(InputFile file, int line, int lineOffset) {
    int ref = reportComponents.get(((DefaultInputFile) file).key()).getRef();
    File highlightingFile = reader.readComponentSyntaxHighlighting(ref);
    if (highlightingFile == null) {
      return Collections.emptyList();
    }
    TextPointer pointer = file.newPointer(line, lineOffset);
    List<TypeOfText> result = new ArrayList<TypeOfText>();
    InputStream inputStream = null;
    try {
      inputStream = FileUtils.openInputStream(highlightingFile);
      BatchReport.SyntaxHighlighting rule = BatchReport.SyntaxHighlighting.PARSER.parseDelimitedFrom(inputStream);
      while (rule != null) {
        TextRange ruleRange = toRange(file, rule.getRange());
        if (ruleRange.start().compareTo(pointer) <= 0 && ruleRange.end().compareTo(pointer) > 0) {
          result.add(BatchReportUtils.toBatchType(rule.getType()));
        }
        // Get next element
        rule = BatchReport.SyntaxHighlighting.PARSER.parseDelimitedFrom(inputStream);
      }

    } catch (Exception e) {
      throw new IllegalStateException("Can't read syntax highlighting for " + file.absolutePath(), e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
    return result;
  }

  private static TextRange toRange(InputFile file, Range reportRange) {
    return file.newRange(file.newPointer(reportRange.getStartLine(), reportRange.getStartOffset()), file.newPointer(reportRange.getEndLine(), reportRange.getEndOffset()));
  }

  /**
   * Get list of all start positions of a symbol in an inputfile
   * @param symbolStartLine 0-based start offset for the symbol in file
   * @param symbolStartLineOffset 0-based end offset for the symbol in file
   */
  @CheckForNull
  public List<Range> symbolReferencesFor(InputFile file, int symbolStartLine, int symbolStartLineOffset) {
    int ref = reportComponents.get(((DefaultInputFile) file).key()).getRef();
    List<Symbol> symbols = getReportReader().readComponentSymbols(ref);
    if (symbols.isEmpty()) {
      return Collections.emptyList();
    }
    for (Symbol symbol : symbols) {
      if (symbol.getDeclaration().getStartLine() == symbolStartLine && symbol.getDeclaration().getStartOffset() == symbolStartLineOffset) {
        return symbol.getReferenceList();
      }
    }
    return Collections.emptyList();
  }

  @CheckForNull
  public BatchReport.Coverage coverageFor(InputFile file, int line) {
    int ref = reportComponents.get(((DefaultInputFile) file).key()).getRef();
    try (InputStream inputStream = FileUtils.openInputStream(getReportReader().readComponentCoverage(ref))) {
      BatchReport.Coverage coverage = BatchReport.Coverage.PARSER.parseDelimitedFrom(inputStream);
      while (coverage != null) {
        if (coverage.getLine() == line) {
          return coverage;
        }
        coverage = BatchReport.Coverage.PARSER.parseDelimitedFrom(inputStream);
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

  /**
   * @return null if no dependency else return dependency weight.
   */
  @CheckForNull
  public Integer dependencyWeight(InputFile from, InputFile to) {
    String fromKey = ((DefaultInputFile) from).key();
    String toKey = ((DefaultInputFile) to).key();
    return dependencies.containsKey(fromKey) ? dependencies.get(fromKey).get(toKey) : null;
  }
}
