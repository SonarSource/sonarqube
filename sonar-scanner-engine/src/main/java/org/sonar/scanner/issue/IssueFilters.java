/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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

import java.util.List;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputProject;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @deprecated since 7.6, {@link IssueFilter} is deprecated
 */
@Deprecated
public class IssueFilters {
  private IssueFilterChain filterChain;
  private final DefaultInputProject project;

  @Autowired
  public IssueFilters(DefaultInputProject project) {
    this.project = project;
  }

  public boolean accept(InputComponent component, ScannerReport.Issue rawIssue, String severity) {
    if (filterChain == null) {
      throw new IllegalStateException("Issue filters must be registered before this class can be used");
    }
    FilterableIssue fIssue = new DefaultFilterableIssue(project, rawIssue, severity, component);
    return filterChain.accept(fIssue);
  }

  public void registerFilters(List<IssueFilter> exclusionFilters) {
    this.filterChain = new DefaultIssueFilterChain(exclusionFilters);
  }

}
