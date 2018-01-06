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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilter;
import org.sonar.api.issue.batch.IssueFilterChain;

/**
 * @deprecated since 5.3
 */
@Deprecated
public class DeprecatedIssueFilterChain implements IssueFilterChain {

  private final List<IssueFilter> filters;

  public DeprecatedIssueFilterChain(IssueFilter... filters) {
    this.filters = Arrays.asList(filters);
  }

  public DeprecatedIssueFilterChain() {
    this.filters = Collections.emptyList();
  }

  private DeprecatedIssueFilterChain(List<IssueFilter> filters) {
    this.filters = filters;
  }

  @Override
  public boolean accept(Issue issue) {
    if (filters.isEmpty()) {
      return true;
    } else {
      return filters.get(0).accept(issue, new DeprecatedIssueFilterChain(filters.subList(1, filters.size())));
    }
  }
}
