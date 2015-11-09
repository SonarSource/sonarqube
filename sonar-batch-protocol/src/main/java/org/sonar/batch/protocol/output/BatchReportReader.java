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
import javax.annotation.CheckForNull;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.Protobuf;

import static org.sonar.core.util.CloseableIterator.emptyCloseableIterator;

public class BatchReportReader {

  private final FileStructure fileStructure;

  public BatchReportReader(File dir) {
    this.fileStructure = new FileStructure(dir);
  }

  public BatchReport.Metadata readMetadata() {
    File file = fileStructure.metadataFile();
    if (!fileExists(file)) {
      throw new IllegalStateException("Metadata file is missing in analysis report: " + file);
    }
    return Protobuf.read(file, BatchReport.Metadata.PARSER);
  }

  public CloseableIterator<BatchReport.ActiveRule> readActiveRules() {
    File file = fileStructure.activeRules();
    if (!fileExists(file)) {
      return emptyCloseableIterator();
    }
    return Protobuf.readStream(file, BatchReport.ActiveRule.PARSER);
  }

  public CloseableIterator<BatchReport.Measure> readComponentMeasures(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.MEASURES, componentRef);
    if (fileExists(file)) {
      return Protobuf.readStream(file, BatchReport.Measure.PARSER);
    }
    return emptyCloseableIterator();
  }

  @CheckForNull
  public BatchReport.Changesets readChangesets(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.CHANGESETS, componentRef);
    if (fileExists(file)) {
      return Protobuf.read(file, BatchReport.Changesets.PARSER);
    }
    return null;
  }

  public BatchReport.Component readComponent(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.COMPONENT, componentRef);
    if (!fileExists(file)) {
      throw new IllegalStateException("Unable to find report for component #" + componentRef + ". File does not exist: " + file);
    }
    return Protobuf.read(file, BatchReport.Component.PARSER);
  }

  public CloseableIterator<BatchReport.Issue> readComponentIssues(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES, componentRef);
    if (fileExists(file)) {
      return Protobuf.readStream(file, BatchReport.Issue.PARSER);
    }
    return emptyCloseableIterator();
  }

  public CloseableIterator<BatchReport.Duplication> readComponentDuplications(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.DUPLICATIONS, componentRef);
    if (fileExists(file)) {
      return Protobuf.readStream(file, BatchReport.Duplication.PARSER);
    }
    return emptyCloseableIterator();
  }

  public CloseableIterator<BatchReport.CpdTextBlock> readCpdTextBlocks(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.CPD_TEXT_BLOCKS, componentRef);
    if (fileExists(file)) {
      return Protobuf.readStream(file, BatchReport.CpdTextBlock.parser());
    }
    return emptyCloseableIterator();
  }

  public CloseableIterator<BatchReport.Symbol> readComponentSymbols(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYMBOLS, componentRef);
    if (fileExists(file)) {
      return Protobuf.readStream(file, BatchReport.Symbol.PARSER);
    }
    return emptyCloseableIterator();
  }

  public boolean hasSyntaxHighlighting(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, componentRef);
    return file.exists();
  }

  public CloseableIterator<BatchReport.SyntaxHighlighting> readComponentSyntaxHighlighting(int fileRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, fileRef);
    if (fileExists(file)) {
      return Protobuf.readStream(file, BatchReport.SyntaxHighlighting.PARSER);
    }
    return emptyCloseableIterator();
  }

  public boolean hasCoverage(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.COVERAGES, componentRef);
    return file.exists();
  }

  public CloseableIterator<BatchReport.Coverage> readComponentCoverage(int fileRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.COVERAGES, fileRef);
    if (fileExists(file)) {
      return Protobuf.readStream(file, BatchReport.Coverage.PARSER);
    }
    return emptyCloseableIterator();
  }

  @CheckForNull
  public File readFileSource(int fileRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.SOURCE, fileRef);
    if (fileExists(file)) {
      return file;
    }
    return null;
  }

  @CheckForNull
  public File readTests(int testFileRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.TESTS, testFileRef);
    if (fileExists(file)) {
      return file;
    }

    return null;
  }

  @CheckForNull
  public File readCoverageDetails(int testFileRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.COVERAGE_DETAILS, testFileRef);
    if (fileExists(file)) {
      return file;
    }

    return null;
  }

  private static boolean fileExists(File file) {
    return file.exists() && file.isFile();
  }

  public FileStructure getFileStructure() {
    return fileStructure;
  }
}
