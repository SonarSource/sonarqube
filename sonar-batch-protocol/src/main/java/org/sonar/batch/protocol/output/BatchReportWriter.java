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

import org.sonar.batch.protocol.ProtobufUtil;

import java.io.File;

public class BatchReportWriter {

  private final FileStructure fileStructure;

  public BatchReportWriter(File dir) {
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IllegalStateException("Unable to create directory: " + dir);
    }
    this.fileStructure = new FileStructure(dir);
  }

  public FileStructure getFileStructure() {
    return fileStructure;
  }

  public boolean hasComponentData(FileStructure.Domain domain, int componentRef) {
    File file = fileStructure.fileFor(domain, componentRef);
    return file.exists() && file.isFile();
  }

  /**
   * Metadata is mandatory
   */
  public void writeMetadata(BatchReport.Metadata metadata) {
    ProtobufUtil.writeToFile(metadata, fileStructure.metadataFile());
  }

  public void writeComponent(BatchReport.Component component) {
    File file = fileStructure.fileFor(FileStructure.Domain.COMPONENT, component.getRef());
    ProtobufUtil.writeToFile(component, file);
  }

  public void writeComponentIssues(int componentRef, Iterable<BatchReport.Issue> issues) {
    BatchReport.Issues.Builder issuesBuilder = BatchReport.Issues.newBuilder();
    issuesBuilder.setComponentRef(componentRef);
    issuesBuilder.addAllIssue(issues);
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES, componentRef);
    ProtobufUtil.writeToFile(issuesBuilder.build(), file);
  }

  public void writeComponentMeasures(int componentRef, Iterable<BatchReport.Measure> measures) {
    BatchReport.Measures.Builder measuresBuilder = BatchReport.Measures.newBuilder();
    measuresBuilder.setComponentRef(componentRef);
    measuresBuilder.addAllMeasure(measures);
    File file = fileStructure.fileFor(FileStructure.Domain.MEASURES, componentRef);
    ProtobufUtil.writeToFile(measuresBuilder.build(), file);
  }

  public void writeComponentChangesets(BatchReport.Changesets changesets) {
    File file = fileStructure.fileFor(FileStructure.Domain.CHANGESETS, changesets.getComponentRef());
    ProtobufUtil.writeToFile(changesets, file);
  }

  /**
   * Issues on components which have been deleted are stored in another location.
   * Temporary hack, waiting for computation stack
   */
  public void writeDeletedComponentIssues(int componentRef, String componentUuid, Iterable<BatchReport.Issue> issues) {
    BatchReport.Issues.Builder issuesBuilder = BatchReport.Issues.newBuilder();
    issuesBuilder.setComponentRef(componentRef);
    issuesBuilder.setComponentUuid(componentUuid);
    issuesBuilder.addAllIssue(issues);
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES_ON_DELETED, componentRef);
    ProtobufUtil.writeToFile(issuesBuilder.build(), file);
  }

  public void writeComponentDuplications(int componentRef, Iterable<BatchReport.Duplication> duplications) {
    BatchReport.Duplications.Builder builder = BatchReport.Duplications.newBuilder();
    builder.setComponentRef(componentRef);
    builder.addAllDuplication(duplications);
    File file = fileStructure.fileFor(FileStructure.Domain.DUPLICATIONS, componentRef);
    ProtobufUtil.writeToFile(builder.build(), file);
  }

  public void writeComponentSymbols(int componentRef, Iterable<BatchReport.Symbols.Symbol> symbols) {
    BatchReport.Symbols.Builder builder = BatchReport.Symbols.newBuilder();
    builder.setFileRef(componentRef);
    builder.addAllSymbol(symbols);
    File file = fileStructure.fileFor(FileStructure.Domain.SYMBOLS, componentRef);
    ProtobufUtil.writeToFile(builder.build(), file);
  }

  public void writeComponentSyntaxHighlighting(int componentRef, Iterable<BatchReport.SyntaxHighlighting> syntaxHighlightingRules) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, componentRef);
    ProtobufUtil.writeMessagesToFile(syntaxHighlightingRules, file);
  }

  public void writeComponentCoverage(int componentRef, Iterable<BatchReport.Coverage> coverageList) {
    File file = fileStructure.fileFor(FileStructure.Domain.COVERAGES, componentRef);
    ProtobufUtil.writeMessagesToFile(coverageList, file);
  }

  public void writeTests(int componentRef, Iterable<BatchReport.Test> tests) {
    File file = fileStructure.fileFor(FileStructure.Domain.TESTS, componentRef);
    ProtobufUtil.writeMessagesToFile(tests, file);
  }

  public void writeCoverageDetails(int componentRef, Iterable<BatchReport.CoverageDetail> tests) {
    File file = fileStructure.fileFor(FileStructure.Domain.COVERAGE_DETAILS, componentRef);
    ProtobufUtil.writeMessagesToFile(tests, file);
  }

  public void writeFileDependencies(int componentRef, Iterable<BatchReport.FileDependency> fileDependencies) {
    File file = fileStructure.fileFor(FileStructure.Domain.FILE_DEPENDENCIES, componentRef);
    ProtobufUtil.writeMessagesToFile(fileDependencies, file);
  }

  public void writeModuleDependencies(int componentRef, Iterable<BatchReport.ModuleDependencies.ModuleDependency> dependencies) {
    BatchReport.ModuleDependencies.Builder builder = BatchReport.ModuleDependencies.newBuilder();
    builder.addAllDep(dependencies);
    File file = fileStructure.fileFor(FileStructure.Domain.MODULE_DEPENDENCIES, componentRef);
    ProtobufUtil.writeToFile(builder.build(), file);
  }

  public File getSourceFile(int componentRef) {
    return fileStructure.fileFor(FileStructure.Domain.SOURCE, componentRef);
  }

}
