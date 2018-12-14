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
package org.sonar.scanner.issue;

import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.scanner.ProjectAnalysisInfo;
import org.sonar.scanner.protocol.output.ScannerReport;

/**
 * @deprecated since 7.6, {@link IssueFilter} is deprecated
 */
@Deprecated
public class IssueFilters {
  private final IssueFilterChain filterChain;
  private final DefaultInputProject project;
  private final ProjectAnalysisInfo projectAnalysisInfo;

  public IssueFilters(DefaultInputProject project, ProjectAnalysisInfo projectAnalysisInfo, IssueFilter[] exclusionFilters) {
    this.project = project;
    this.filterChain = new DefaultIssueFilterChain(exclusionFilters);
    this.projectAnalysisInfo = projectAnalysisInfo;
  }

  public IssueFilters(DefaultInputProject project, ProjectAnalysisInfo projectAnalysisInfo) {
    this(project, projectAnalysisInfo, new IssueFilter[0]);
  }

  public boolean accept(InputComponent component, ScannerReport.Issue rawIssue) {
    FilterableIssue fIssue = new DefaultFilterableIssue(project, projectAnalysisInfo, rawIssue, component);
    return filterChain.accept(fIssue);
  }

}
