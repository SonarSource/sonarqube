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

import com.google.common.collect.ImmutableList;
import org.sonar.api.scan.issue.filter.IssueFilter;

import java.util.List;

import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilterChain;

public class DefaultIssueFilterChain implements IssueFilterChain {
  private final List<IssueFilter> filters;

  public DefaultIssueFilterChain(IssueFilter... filters) {
    this.filters = ImmutableList.copyOf(filters);
  }

  public DefaultIssueFilterChain() {
    this.filters = ImmutableList.of();
  }

  private DefaultIssueFilterChain(List<IssueFilter> filters) {
    this.filters = filters;
  }

  @Override
  public boolean accept(FilterableIssue issue) {
    if (filters.isEmpty()) {
      return true;
    } else {
      return filters.get(0).accept(issue, new DefaultIssueFilterChain(filters.subList(1, filters.size())));
    }
  }

}
