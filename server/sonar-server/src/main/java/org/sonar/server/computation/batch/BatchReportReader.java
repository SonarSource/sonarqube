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
package org.sonar.server.computation.batch;

import javax.annotation.CheckForNull;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.util.CloseableIterator;

public interface BatchReportReader {
  BatchReport.Metadata readMetadata();

  CloseableIterator<BatchReport.ActiveRule> readActiveRules();

  CloseableIterator<BatchReport.Measure> readComponentMeasures(int componentRef);

  @CheckForNull
  BatchReport.Changesets readChangesets(int componentRef);

  BatchReport.Component readComponent(int componentRef);

  CloseableIterator<BatchReport.Issue> readComponentIssues(int componentRef);

  CloseableIterator<BatchReport.Duplication> readComponentDuplications(int componentRef);

  CloseableIterator<BatchReport.Symbol> readComponentSymbols(int componentRef);

  CloseableIterator<BatchReport.SyntaxHighlighting> readComponentSyntaxHighlighting(int fileRef);

  CloseableIterator<BatchReport.Coverage> readComponentCoverage(int fileRef);

  /**
   * Reads file source line by line. Throws an exception if the ref does not relate
   * to a file
   */
  CloseableIterator<String> readFileSource(int fileRef);

  CloseableIterator<BatchReport.Test> readTests(int testFileRef);

  CloseableIterator<BatchReport.CoverageDetail> readCoverageDetails(int testFileRef);
}
