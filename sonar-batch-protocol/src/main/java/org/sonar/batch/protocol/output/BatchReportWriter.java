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

import java.io.File;
import org.sonar.batch.protocol.ProtobufUtil;

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
  public File writeMetadata(BatchReport.Metadata metadata) {
    ProtobufUtil.writeToFile(metadata, fileStructure.metadataFile());
    return fileStructure.metadataFile();
  }

  public File writeComponent(BatchReport.Component component) {
    File file = fileStructure.fileFor(FileStructure.Domain.COMPONENT, component.getRef());
    ProtobufUtil.writeToFile(component, file);
    return file;
  }

  public File writeComponentIssues(int componentRef, Iterable<BatchReport.Issue> issues) {
    BatchReport.Issues.Builder issuesBuilder = BatchReport.Issues.newBuilder();
    issuesBuilder.setComponentRef(componentRef);
    issuesBuilder.addAllIssue(issues);
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES, componentRef);
    ProtobufUtil.writeToFile(issuesBuilder.build(), file);
    return file;
  }

  public File writeComponentMeasures(int componentRef, Iterable<BatchReport.Measure> measures) {
    BatchReport.Measures.Builder measuresBuilder = BatchReport.Measures.newBuilder();
    measuresBuilder.setComponentRef(componentRef);
    measuresBuilder.addAllMeasure(measures);
    File file = fileStructure.fileFor(FileStructure.Domain.MEASURES, componentRef);
    ProtobufUtil.writeToFile(measuresBuilder.build(), file);
    return file;
  }

  public File writeComponentChangesets(BatchReport.Changesets changesets) {
    File file = fileStructure.fileFor(FileStructure.Domain.CHANGESETS, changesets.getComponentRef());
    ProtobufUtil.writeToFile(changesets, file);
    return file;
  }

  /**
   * Issues on components which have been deleted are stored in another location.
   * Temporary hack, waiting for computation stack
   */
  public File writeDeletedComponentIssues(int componentRef, String componentUuid, Iterable<BatchReport.Issue> issues) {
    BatchReport.Issues.Builder issuesBuilder = BatchReport.Issues.newBuilder();
    issuesBuilder.setComponentRef(componentRef);
    issuesBuilder.setComponentUuid(componentUuid);
    issuesBuilder.addAllIssue(issues);
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES_ON_DELETED, componentRef);
    ProtobufUtil.writeToFile(issuesBuilder.build(), file);
    return file;
  }

  public File writeComponentDuplications(int componentRef, Iterable<BatchReport.Duplication> duplications) {
    BatchReport.Duplications.Builder builder = BatchReport.Duplications.newBuilder();
    builder.setComponentRef(componentRef);
    builder.addAllDuplication(duplications);
    File file = fileStructure.fileFor(FileStructure.Domain.DUPLICATIONS, componentRef);
    ProtobufUtil.writeToFile(builder.build(), file);
    return file;
  }

  public File writeComponentSymbols(int componentRef, Iterable<BatchReport.Symbols.Symbol> symbols) {
    BatchReport.Symbols.Builder builder = BatchReport.Symbols.newBuilder();
    builder.setFileRef(componentRef);
    builder.addAllSymbol(symbols);
    File file = fileStructure.fileFor(FileStructure.Domain.SYMBOLS, componentRef);
    ProtobufUtil.writeToFile(builder.build(), file);
    return file;
  }

  public File writeComponentSyntaxHighlighting(int componentRef, Iterable<BatchReport.SyntaxHighlighting> syntaxHighlightingRules) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, componentRef);
    ProtobufUtil.writeMessagesToFile(syntaxHighlightingRules, file);
    return file;
  }

  public File writeComponentCoverage(int componentRef, Iterable<BatchReport.Coverage> coverageList) {
    File file = fileStructure.fileFor(FileStructure.Domain.COVERAGES, componentRef);
    ProtobufUtil.writeMessagesToFile(coverageList, file);
    return file;
  }

  public File writeTests(int componentRef, Iterable<BatchReport.Test> tests) {
    File file = fileStructure.fileFor(FileStructure.Domain.TESTS, componentRef);
    ProtobufUtil.writeMessagesToFile(tests, file);
    return file;
  }

  public File writeCoverageDetails(int componentRef, Iterable<BatchReport.CoverageDetail> tests) {
    File file = fileStructure.fileFor(FileStructure.Domain.COVERAGE_DETAILS, componentRef);
    ProtobufUtil.writeMessagesToFile(tests, file);
    return file;
  }

  public File getSourceFile(int componentRef) {
    return fileStructure.fileFor(FileStructure.Domain.SOURCE, componentRef);
  }

}
