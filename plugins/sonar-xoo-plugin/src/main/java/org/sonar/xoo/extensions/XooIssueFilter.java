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
package org.sonar.xoo.extensions;

import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.config.Configuration;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.scan.issue.filter.IssueFilterChain;

public class XooIssueFilter implements IssueFilter {

  private final Configuration config;

  public XooIssueFilter(Configuration config) {
    this.config = config;
  }


  @Override
  public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
    if (config.getBoolean("sonar.xoo.excludeAllIssuesOnOddLines").orElse(false) && isOdd(issue)) {
      return false;
    }
    return chain.accept(issue);
  }

  private static boolean isOdd(FilterableIssue issue) {
    TextRange textRange = issue.textRange();
    return textRange != null && textRange.start().line() % 2 == 1;
  }
}
