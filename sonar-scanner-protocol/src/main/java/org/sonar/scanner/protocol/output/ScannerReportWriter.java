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
package org.sonar.scanner.protocol.output;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import javax.annotation.concurrent.Immutable;
import org.sonar.core.util.ContextException;
import org.sonar.core.util.Protobuf;

@Immutable
public class ScannerReportWriter {

  private final FileStructure fileStructure;

  public ScannerReportWriter(File dir) {
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
  public File writeMetadata(ScannerReport.Metadata metadata) {
    Protobuf.write(metadata, fileStructure.metadataFile());
    return fileStructure.metadataFile();
  }

  public File writeActiveRules(Iterable<ScannerReport.ActiveRule> activeRules) {
    Protobuf.writeStream(activeRules, fileStructure.activeRules(), false);
    return fileStructure.metadataFile();
  }

  public File writeComponent(ScannerReport.Component component) {
    File file = fileStructure.fileFor(FileStructure.Domain.COMPONENT, component.getRef());
    Protobuf.write(component, file);
    return file;
  }

  public File writeComponentIssues(int componentRef, Iterable<ScannerReport.Issue> issues) {
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES, componentRef);
    Protobuf.writeStream(issues, file, false);
    return file;
  }

  public File writeComponentSignificantCode(int componentRef, Iterable<ScannerReport.LineSgnificantCode> lineSignificantCode) {
    File file = fileStructure.fileFor(FileStructure.Domain.SGNIFICANT_CODE, componentRef);
    Protobuf.writeStream(lineSignificantCode, file, false);
    return file;
  }

  public void appendComponentIssue(int componentRef, ScannerReport.Issue issue) {
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES, componentRef);
    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file, true))) {
      issue.writeDelimitedTo(out);
    } catch (Exception e) {
      throw ContextException.of("Unable to write issue", e).addContext("file", file);
    }
  }

  public File writeComponentChangedLines(int componentRef, ScannerReport.ChangedLines changedLines) {
    File file = fileStructure.fileFor(FileStructure.Domain.CHANGED_LINES, componentRef);
    Protobuf.write(changedLines, file);
    return file;
  }

  public void appendComponentExternalIssue(int componentRef, ScannerReport.ExternalIssue issue) {
    File file = fileStructure.fileFor(FileStructure.Domain.EXTERNAL_ISSUES, componentRef);
    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file, true))) {
      issue.writeDelimitedTo(out);
    } catch (Exception e) {
      throw ContextException.of("Unable to write external issue", e).addContext("file", file);
    }
  }

  public void appendAdHocRule(ScannerReport.AdHocRule adHocRule) {
    File file = fileStructure.adHocRules();
    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file, true))) {
      adHocRule.writeDelimitedTo(out);
    } catch (Exception e) {
      throw ContextException.of("Unable to write ad hoc rule", e).addContext("file", file);
    }
  }

  public void appendComponentMeasure(int componentRef, ScannerReport.Measure measure) {
    File file = fileStructure.fileFor(FileStructure.Domain.MEASURES, componentRef);
    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(file, true))) {
      measure.writeDelimitedTo(out);
    } catch (Exception e) {
      throw ContextException.of("Unable to write measure", e).addContext("file", file);
    }
  }

  public File writeComponentChangesets(ScannerReport.Changesets changesets) {
    File file = fileStructure.fileFor(FileStructure.Domain.CHANGESETS, changesets.getComponentRef());
    Protobuf.write(changesets, file);
    return file;
  }

  public File writeComponentDuplications(int componentRef, Iterable<ScannerReport.Duplication> duplications) {
    File file = fileStructure.fileFor(FileStructure.Domain.DUPLICATIONS, componentRef);
    Protobuf.writeStream(duplications, file, false);
    return file;
  }

  public File writeCpdTextBlocks(int componentRef, Iterable<ScannerReport.CpdTextBlock> blocks) {
    File file = fileStructure.fileFor(FileStructure.Domain.CPD_TEXT_BLOCKS, componentRef);
    Protobuf.writeStream(blocks, file, false);
    return file;
  }

  public File writeComponentSymbols(int componentRef, Iterable<ScannerReport.Symbol> symbols) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYMBOLS, componentRef);
    Protobuf.writeStream(symbols, file, false);
    return file;
  }

  public File writeComponentSyntaxHighlighting(int componentRef, Iterable<ScannerReport.SyntaxHighlightingRule> syntaxHighlightingRules) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, componentRef);
    Protobuf.writeStream(syntaxHighlightingRules, file, false);
    return file;
  }

  public File writeComponentCoverage(int componentRef, Iterable<ScannerReport.LineCoverage> coverageList) {
    File file = fileStructure.fileFor(FileStructure.Domain.COVERAGES, componentRef);
    Protobuf.writeStream(coverageList, file, false);
    return file;
  }

  public File writeContextProperties(Iterable<ScannerReport.ContextProperty> properties) {
    File file = fileStructure.contextProperties();
    Protobuf.writeStream(properties, file, false);
    return file;
  }

  public File writeAnalysisWarnings(Iterable<ScannerReport.AnalysisWarning> analysisWarnings) {
    File file = fileStructure.analysisWarnings();
    Protobuf.writeStream(analysisWarnings, file, false);
    return file;
  }

  public File getSourceFile(int componentRef) {
    return fileStructure.fileFor(FileStructure.Domain.SOURCE, componentRef);
  }

}
