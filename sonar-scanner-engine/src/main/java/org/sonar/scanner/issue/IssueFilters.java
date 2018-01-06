/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.issue;

import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.issue.Issue;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.scanner.ProjectAnalysisInfo;
import org.sonar.scanner.protocol.output.ScannerReport;

@ScannerSide
public class IssueFilters {
  private final IssueFilterChain filterChain;
  private final org.sonar.api.issue.batch.IssueFilter[] deprecatedFilters;
  private final DefaultInputModule module;
  private final ProjectAnalysisInfo projectAnalysisInfo;

  public IssueFilters(DefaultInputModule module, ProjectAnalysisInfo projectAnalysisInfo, IssueFilter[] exclusionFilters, org.sonar.api.issue.batch.IssueFilter[] filters) {
    this.module = module;
    this.filterChain = new DefaultIssueFilterChain(exclusionFilters);
    this.deprecatedFilters = filters;
    this.projectAnalysisInfo = projectAnalysisInfo;
  }

  public IssueFilters(DefaultInputModule module, ProjectAnalysisInfo projectAnalysisInfo, IssueFilter[] filters) {
    this(module, projectAnalysisInfo, filters, new org.sonar.api.issue.batch.IssueFilter[0]);
  }

  public IssueFilters(DefaultInputModule module, ProjectAnalysisInfo projectAnalysisInfo, org.sonar.api.issue.batch.IssueFilter[] deprecatedFilters) {
    this(module, projectAnalysisInfo, new IssueFilter[0], deprecatedFilters);
  }

  public IssueFilters(DefaultInputModule module, ProjectAnalysisInfo projectAnalysisInfo) {
    this(module, projectAnalysisInfo, new IssueFilter[0], new org.sonar.api.issue.batch.IssueFilter[0]);
  }

  public boolean accept(String componentKey, ScannerReport.Issue rawIssue) {
    FilterableIssue fIssue = new DefaultFilterableIssue(module, projectAnalysisInfo, rawIssue, componentKey);
    if (filterChain.accept(fIssue)) {
      return acceptDeprecated(componentKey, rawIssue);
    }

    return false;
  }

  public boolean acceptDeprecated(String componentKey, ScannerReport.Issue rawIssue) {
    Issue issue = new DeprecatedIssueAdapterForFilter(module, projectAnalysisInfo, rawIssue, componentKey);
    return new DeprecatedIssueFilterChain(deprecatedFilters).accept(issue);
  }
}
