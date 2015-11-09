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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import org.sonar.core.util.ContextException;
import org.sonar.core.util.Protobuf;

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
    Protobuf.write(metadata, fileStructure.metadataFile());
    return fileStructure.metadataFile();
  }

  public File writeActiveRules(Iterable<BatchReport.ActiveRule> activeRules) {
    Protobuf.writeStream(activeRules, fileStructure.activeRules(), false);
    return fileStructure.metadataFile();
  }

  public File writeComponent(BatchReport.Component component) {
    File file = fileStructure.fileFor(FileStructure.Domain.COMPONENT, component.getRef());
    Protobuf.write(component, file);
    return file;
  }

  public File writeComponentIssues(int componentRef, Iterable<BatchReport.Issue> issues) {
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES, componentRef);
    Protobuf.writeStream(issues, file, false);
    return file;
  }

  public void appendComponentIssue(int componentRef, BatchReport.Issue issue) {
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES, componentRef);
    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file, true))) {
      issue.writeDelimitedTo(out);
    } catch (Exception e) {
      throw ContextException.of("Unable to write issue", e).addContext("file", file);
    }
  }

  public File writeComponentMeasures(int componentRef, Iterable<BatchReport.Measure> measures) {
    File file = fileStructure.fileFor(FileStructure.Domain.MEASURES, componentRef);
    Protobuf.writeStream(measures, file, false);
    return file;
  }

  public File writeComponentChangesets(BatchReport.Changesets changesets) {
    File file = fileStructure.fileFor(FileStructure.Domain.CHANGESETS, changesets.getComponentRef());
    Protobuf.write(changesets, file);
    return file;
  }

  public File writeComponentDuplications(int componentRef, Iterable<BatchReport.Duplication> duplications) {
    File file = fileStructure.fileFor(FileStructure.Domain.DUPLICATIONS, componentRef);
    Protobuf.writeStream(duplications, file, false);
    return file;
  }

  public File writeCpdTextBlocks(int componentRef, Iterable<BatchReport.CpdTextBlock> blocks) {
    File file = fileStructure.fileFor(FileStructure.Domain.CPD_TEXT_BLOCKS, componentRef);
    Protobuf.writeStream(blocks, file, false);
    return file;
  }

  public File writeComponentSymbols(int componentRef, Iterable<BatchReport.Symbol> symbols) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYMBOLS, componentRef);
    Protobuf.writeStream(symbols, file, false);
    return file;
  }

  public File writeComponentSyntaxHighlighting(int componentRef, Iterable<BatchReport.SyntaxHighlighting> syntaxHighlightingRules) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, componentRef);
    Protobuf.writeStream(syntaxHighlightingRules, file, false);
    return file;
  }

  public File writeComponentCoverage(int componentRef, Iterable<BatchReport.Coverage> coverageList) {
    File file = fileStructure.fileFor(FileStructure.Domain.COVERAGES, componentRef);
    Protobuf.writeStream(coverageList, file, false);
    return file;
  }

  public File writeTests(int componentRef, Iterable<BatchReport.Test> tests) {
    File file = fileStructure.fileFor(FileStructure.Domain.TESTS, componentRef);
    Protobuf.writeStream(tests, file, false);
    return file;
  }

  public File writeCoverageDetails(int componentRef, Iterable<BatchReport.CoverageDetail> tests) {
    File file = fileStructure.fileFor(FileStructure.Domain.COVERAGE_DETAILS, componentRef);
    Protobuf.writeStream(tests, file, false);
    return file;
  }

  public File getSourceFile(int componentRef) {
    return fileStructure.fileFor(FileStructure.Domain.SOURCE, componentRef);
  }

}
