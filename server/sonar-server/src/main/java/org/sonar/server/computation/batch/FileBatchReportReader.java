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

public class FileBatchReportReader implements BatchReportReader {
  private final org.sonar.batch.protocol.output.BatchReportReader delegate;

  public FileBatchReportReader(org.sonar.batch.protocol.output.BatchReportReader delegate) {
    this.delegate = delegate;
  }

  @Override
  public BatchReport.Metadata readMetadata() {
    return delegate.readMetadata();
  }

  @Override
  public List<BatchReport.Measure> readComponentMeasures(int componentRef) {
    return delegate.readComponentMeasures(componentRef);
  }

  @Override
  @CheckForNull
  public BatchReport.Changesets readChangesets(int componentRef) {
    return delegate.readChangesets(componentRef);
  }

  @Override
  public BatchReport.Component readComponent(int componentRef) {
    return delegate.readComponent(componentRef);
  }

  @Override
  public List<BatchReport.Issue> readComponentIssues(int componentRef) {
    return delegate.readComponentIssues(componentRef);
  }

  @Override
  public BatchReport.Issues readDeletedComponentIssues(int deletedComponentRef) {
    return delegate.readDeletedComponentIssues(deletedComponentRef);
  }

  @Override
  public List<BatchReport.Duplication> readComponentDuplications(int componentRef) {
    return delegate.readComponentDuplications(componentRef);
  }

  @Override
  public List<BatchReport.Symbols.Symbol> readComponentSymbols(int componentRef) {
    return delegate.readComponentSymbols(componentRef);
  }

  @Override
  public boolean hasSyntaxHighlighting(int componentRef) {
    return delegate.hasSyntaxHighlighting(componentRef);
  }

  @Override
  @CheckForNull
  public File readComponentSyntaxHighlighting(int fileRef) {
    return delegate.readComponentSyntaxHighlighting(fileRef);
  }

  @Override
  @CheckForNull
  public File readComponentCoverage(int fileRef) {
    return delegate.readComponentCoverage(fileRef);
  }

  @Override
  public File readFileSource(int fileRef) {
    return delegate.readFileSource(fileRef);
  }

  @Override
  @CheckForNull
  public File readTests(int testFileRef) {
    return delegate.readTests(testFileRef);
  }

  @Override
  @CheckForNull
  public File readCoverageDetails(int testFileRef) {
    return delegate.readCoverageDetails(testFileRef);
  }
}
