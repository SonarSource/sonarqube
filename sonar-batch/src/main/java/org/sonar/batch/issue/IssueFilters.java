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

import org.sonar.api.BatchSide;
import org.sonar.api.issue.batch.IssueFilter;
import org.sonar.api.issue.internal.DefaultIssue;

@BatchSide
public class IssueFilters {

  private final org.sonar.api.issue.IssueFilter[] exclusionFilters;
  private final IssueFilter[] filters;

  public IssueFilters(org.sonar.api.issue.IssueFilter[] exclusionFilters, IssueFilter[] filters) {
    this.exclusionFilters = exclusionFilters;
    this.filters = filters;
  }

  public IssueFilters(org.sonar.api.issue.IssueFilter[] exclusionFilters) {
    this(exclusionFilters, new IssueFilter[0]);
  }

  public IssueFilters(IssueFilter[] filters) {
    this(new org.sonar.api.issue.IssueFilter[0], filters);
  }

  public IssueFilters() {
    this(new org.sonar.api.issue.IssueFilter[0], new IssueFilter[0]);
  }

  public boolean accept(DefaultIssue issue) {
    if (new DefaultIssueFilterChain(filters).accept(issue)) {
      // Apply deprecated rules only if filter chain accepts the current issue
      for (org.sonar.api.issue.IssueFilter filter : exclusionFilters) {
        if (!filter.accept(issue)) {
          return false;
        }
      }
      return true;
    } else {
      return false;
    }
  }
}
