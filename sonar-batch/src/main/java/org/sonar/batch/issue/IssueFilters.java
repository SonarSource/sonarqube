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
package org.sonar.batch.issue;

import org.sonar.api.scan.issue.filter.FilterableIssue;

import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.api.batch.BatchSide;
import org.sonar.api.issue.Issue;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.resources.Project;
import org.sonar.batch.protocol.output.BatchReport;

@BatchSide
public class IssueFilters {
  private final IssueFilter[] filters;
  private final org.sonar.api.issue.batch.IssueFilter[] deprecatedFilters;
  private final Project project;

  public IssueFilters(Project project, IssueFilter[] exclusionFilters, org.sonar.api.issue.batch.IssueFilter[] filters) {
    this.project = project;
    this.filters = exclusionFilters;
    this.deprecatedFilters = filters;
  }

  public IssueFilters(Project project, IssueFilter[] filters) {
    this(project, filters, new org.sonar.api.issue.batch.IssueFilter[0]);
  }

  public IssueFilters(Project project, org.sonar.api.issue.batch.IssueFilter[] deprecatedFilters) {
    this(project, new IssueFilter[0], deprecatedFilters);
  }

  public IssueFilters(Project project) {
    this(project, new IssueFilter[0], new org.sonar.api.issue.batch.IssueFilter[0]);
  }

  public boolean accept(String componentKey, BatchReport.Issue rawIssue) {
    IssueFilterChain filterChain = new DefaultIssueFilterChain(filters);
    FilterableIssue fIssue = new DefaultFilterableIssue(project, rawIssue, componentKey);
    if (filterChain.accept(fIssue)) {
      return acceptDeprecated(componentKey, rawIssue);
    }

    return false;
  }

  public boolean acceptDeprecated(String componentKey, BatchReport.Issue rawIssue) {
    Issue issue = new DeprecatedIssueAdapterForFilter(project, rawIssue, componentKey);
    return new DeprecatedIssueFilterChain(deprecatedFilters).accept(issue);
  }
}
