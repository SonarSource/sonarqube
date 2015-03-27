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
    if (isNotAnExistingFile(file)) {
      throw new IllegalStateException("Metadata file is missing in analysis report: " + file);
    }
    return ProtobufUtil.readFile(file, BatchReport.Metadata.PARSER);
  }

  public List<BatchReport.Measure> readComponentMeasures(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.MEASURES, componentRef);
    if (file.exists() && file.isFile()) {
      // all the measures are loaded in memory
      BatchReport.Measures measures = ProtobufUtil.readFile(file, BatchReport.Measures.PARSER);
      return measures.getMeasureList();
    }
    return Collections.emptyList();
  }

  @CheckForNull
  public BatchReport.Scm readComponentScm(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.SCM, componentRef);
    if (file.exists() && file.isFile()) {
      return ProtobufUtil.readFile(file, BatchReport.Scm.PARSER);
    }
    return null;
  }

  public BatchReport.Component readComponent(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.COMPONENT, componentRef);
    if (isNotAnExistingFile(file)) {
      throw new IllegalStateException("Unable to find report for component #" + componentRef + ". File does not exist: " + file);
    }
    return ProtobufUtil.readFile(file, BatchReport.Component.PARSER);
  }

  public List<BatchReport.Issue> readComponentIssues(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES, componentRef);
    if (file.exists() && file.isFile()) {
      // all the issues are loaded in memory
      BatchReport.Issues issues = ProtobufUtil.readFile(file, BatchReport.Issues.PARSER);
      return issues.getIssueList();
    }
    return Collections.emptyList();
  }

  public Issues readDeletedComponentIssues(int deletedComponentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.ISSUES_ON_DELETED, deletedComponentRef);
    if (isNotAnExistingFile(file)) {
      throw new IllegalStateException("Unable to find report for deleted component #" + deletedComponentRef);
    }
    // all the issues are loaded in memory
    return ProtobufUtil.readFile(file, Issues.PARSER);
  }

  public List<BatchReport.Duplication> readComponentDuplications(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.DUPLICATIONS, componentRef);
    if (file.exists() && file.isFile()) {
      // all the duplications are loaded in memory
      BatchReport.Duplications duplications = ProtobufUtil.readFile(file, BatchReport.Duplications.PARSER);
      return duplications.getDuplicationList();
    }
    return Collections.emptyList();
  }

  public List<BatchReport.Symbols.Symbol> readComponentSymbols(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYMBOLS, componentRef);
    if (file.exists() && file.isFile()) {
      // all the symbols are loaded in memory
      BatchReport.Symbols symbols = ProtobufUtil.readFile(file, BatchReport.Symbols.PARSER);
      return symbols.getSymbolList();
    }
    return Collections.emptyList();
  }

  public List<BatchReport.SyntaxHighlighting.HighlightingRule> readComponentSyntaxHighlighting(int componentRef) {
    File file = fileStructure.fileFor(FileStructure.Domain.SYNTAX_HIGHLIGHTING, componentRef);
    if (file.exists() && file.isFile()) {
      // all the highlighting are loaded in memory
      BatchReport.SyntaxHighlighting syntaxHighlighting = ProtobufUtil.readFile(file, BatchReport.SyntaxHighlighting.PARSER);
      return syntaxHighlighting.getHighlightingRuleList();
    }
    return Collections.emptyList();
  }

  private boolean isNotAnExistingFile(File file) {
    return !file.exists() || !file.isFile();
  }
}
