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

import java.io.File;
import java.util.List;
import javax.annotation.CheckForNull;
import org.sonar.batch.protocol.output.BatchReport;

public interface BatchReportReader {
  BatchReport.Metadata readMetadata();

  List<BatchReport.Measure> readComponentMeasures(int componentRef);

  @CheckForNull
  BatchReport.Changesets readChangesets(int componentRef);

  BatchReport.Component readComponent(int componentRef);

  List<BatchReport.Issue> readComponentIssues(int componentRef);

  BatchReport.Issues readDeletedComponentIssues(int deletedComponentRef);

  List<BatchReport.Duplication> readComponentDuplications(int componentRef);

  List<BatchReport.Symbols.Symbol> readComponentSymbols(int componentRef);

  boolean hasSyntaxHighlighting(int componentRef);

  @CheckForNull
  File readComponentSyntaxHighlighting(int fileRef);

  @CheckForNull
  File readComponentCoverage(int fileRef);

  File readFileSource(int fileRef);

  @CheckForNull
  File readTests(int testFileRef);

  @CheckForNull
  File readCoverageDetails(int testFileRef);
}
