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
package org.sonar.ce.task.projectanalysis.batch;

import java.util.Optional;
import javax.annotation.CheckForNull;
import org.sonar.core.util.CloseableIterator;
import org.sonar.scanner.protocol.output.ScannerReport;

public interface BatchReportReader {
  ScannerReport.Metadata readMetadata();

  CloseableIterator<String> readScannerLogs();

  CloseableIterator<ScannerReport.ActiveRule> readActiveRules();

  CloseableIterator<ScannerReport.AdHocRule> readAdHocRules();

  CloseableIterator<ScannerReport.Measure> readComponentMeasures(int componentRef);

  @CheckForNull
  ScannerReport.Changesets readChangesets(int componentRef);

  ScannerReport.Component readComponent(int componentRef);

  CloseableIterator<ScannerReport.Issue> readComponentIssues(int componentRef);

  CloseableIterator<ScannerReport.ExternalIssue> readComponentExternalIssues(int componentRef);

  CloseableIterator<ScannerReport.Duplication> readComponentDuplications(int componentRef);

  CloseableIterator<ScannerReport.CpdTextBlock> readCpdTextBlocks(int componentRef);

  CloseableIterator<ScannerReport.Symbol> readComponentSymbols(int componentRef);

  CloseableIterator<ScannerReport.SyntaxHighlightingRule> readComponentSyntaxHighlighting(int fileRef);

  CloseableIterator<ScannerReport.LineCoverage> readComponentCoverage(int fileRef);

  /**
   * Reads a file's source code, line by line. Returns an absent optional if the file does not exist
   */
  Optional<CloseableIterator<String>> readFileSource(int fileRef);

  CloseableIterator<ScannerReport.ContextProperty> readContextProperties();

  Optional<CloseableIterator<ScannerReport.LineSgnificantCode>> readComponentSignificantCode(int fileRef);

  Optional<ScannerReport.ChangedLines> readComponentChangedLines(int fileRef);

  CloseableIterator<ScannerReport.AnalysisWarning> readAnalysisWarnings();
}
