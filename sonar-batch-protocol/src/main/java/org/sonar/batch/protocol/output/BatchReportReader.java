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
import org.sonar.batch.protocol.output.BatchReport.Issues;

import javax.annotation.CheckForNull;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class BatchReportReader {

  private final FileStructure fileStructure;

  public BatchReportReader(File dir) {
    this.fileStructure = new FileStructure(dir);
  }

  public BatchReport.Metadata readMetadata() {
    File file = fileStructure.metadataFile();
    if (!doesFileExists(file)) {
      throw new IllegalStateException("Metadata file is missing in analysis report: " + file);
    }
    return ProtobufUtil.readFile(file, BatchReport.Metadata.PARSER);
  }

  public List<BatchReport.Measure> readComponentMeasures(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.MEASURES, componentRef);
    if (doesFileExists(file)) {
      // all the measures are loaded in memory
      BatchReport.Measures measures = ProtobufUtil.readFile(file, BatchReport.Measures.PARSER);
      return measures.getMeasureList();
    }
    return Collections.emptyList();
  }

  @CheckForNull
  public BatchReport.Changesets readChangesets(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.CHANGESETS, componentRef);
    if (doesFileExists(file)) {
      return ProtobufUtil.readFile(file, BatchReport.Changesets.PARSER);
    }
    return null;
  }

  public BatchReport.Component readComponent(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.COMPONENT, componentRef);
    if (!doesFileExists(file)) {
      throw new IllegalStateException("Unable to find report for component #" + componentRef + ". File does not exist: " + file);
    }
    return ProtobufUtil.readFile(file, BatchReport.Component.PARSER);
  }

  public List<BatchReport.Issue> readComponentIssues(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES, componentRef);
    if (doesFileExists(file)) {
      // all the issues are loaded in memory
      BatchReport.Issues issues = ProtobufUtil.readFile(file, BatchReport.Issues.PARSER);
      return issues.getIssueList();
    }
    return Collections.emptyList();
  }

  public Issues readDeletedComponentIssues(int deletedComponentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES_ON_DELETED, deletedComponentRef);
    if (!doesFileExists(file)) {
      throw new IllegalStateException("Unable to find report for deleted component #" + deletedComponentRef);
    }
    // all the issues are loaded in memory
    return ProtobufUtil.readFile(file, Issues.PARSER);
  }

  public List<BatchReport.Duplication> readComponentDuplications(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.DUPLICATIONS, componentRef);
    if (doesFileExists(file)) {
      // all the duplications are loaded in memory
      BatchReport.Duplications duplications = ProtobufUtil.readFile(file, BatchReport.Duplications.PARSER);
      return duplications.getDuplicationList();
    }
    return Collections.emptyList();
  }

  public List<BatchReport.Symbols.Symbol> readComponentSymbols(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYMBOLS, componentRef);
    if (doesFileExists(file)) {
      // all the symbols are loaded in memory
      BatchReport.Symbols symbols = ProtobufUtil.readFile(file, BatchReport.Symbols.PARSER);
      return symbols.getSymbolList();
    }
    return Collections.emptyList();
  }

  public boolean hasSyntaxHighlighting(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, componentRef);
    return file.exists();
  }

  @CheckForNull
  public File readComponentSyntaxHighlighting(int fileRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYNTAX_HIGHLIGHTINGS, fileRef);
    if (doesFileExists(file)) {
      return file;
    }
    return null;
  }

  @CheckForNull
  public File readComponentCoverage(int fileRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.COVERAGES, fileRef);
    if (doesFileExists(file)) {
      return file;
    }
    return null;
  }

  public File readFileSource(int fileRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.SOURCE, fileRef);
    if (!doesFileExists(file)) {
      throw new IllegalStateException("Unable to find source for file #" + fileRef + ". File does not exist: " + file);
    }
    return file;
  }

  @CheckForNull
  public File readTests(int testFileRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.TESTS, testFileRef);
    if (doesFileExists(file)) {
      return file;
    }

    return null;
  }

  @CheckForNull
  public File readCoverageDetails(int testFileRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.COVERAGE_DETAILS, testFileRef);
    if (doesFileExists(file)) {
      return file;
    }

    return null;
  }

  @CheckForNull
  public File readFileDependencies(int fileRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.FILE_DEPENDENCIES, fileRef);
    if (doesFileExists(file)) {
      return file;
    }
    return null;
  }

  public List<BatchReport.ModuleDependencies.ModuleDependency> readModuleDependencies(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.MODULE_DEPENDENCIES, componentRef);
    if (doesFileExists(file)) {
      // all the module dependencies are loaded in memory
      BatchReport.ModuleDependencies dependencies = ProtobufUtil.readFile(file, BatchReport.ModuleDependencies.PARSER);
      return dependencies.getDepList();
    }
    return Collections.emptyList();
  }

  private boolean doesFileExists(File file) {
    return file.exists() && file.isFile();
  }
}
