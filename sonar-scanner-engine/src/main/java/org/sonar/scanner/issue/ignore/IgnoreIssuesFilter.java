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
package org.sonar.scanner.issue.ignore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.scanner.issue.ignore.pattern.IssuePattern;
import org.sonar.scanner.issue.ignore.pattern.PatternMatcher;

public class IgnoreIssuesFilter implements IssueFilter {

  private PatternMatcher patternMatcher;

  private static final Logger LOG = LoggerFactory.getLogger(IgnoreIssuesFilter.class);

  public IgnoreIssuesFilter(PatternMatcher patternMatcher) {
    this.patternMatcher = patternMatcher;
  }

  @Override
  public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
    if (hasMatchFor(issue)) {
      return false;
    }
    return chain.accept(issue);
  }

  private boolean hasMatchFor(FilterableIssue issue) {
    IssuePattern pattern = patternMatcher.getMatchingPattern(issue.componentKey(), issue.ruleKey(), issue.line());
    if (pattern != null) {
      logExclusion(issue, pattern);
      return true;
    }
    return false;
  }

  private static void logExclusion(FilterableIssue issue, IssuePattern pattern) {
    LOG.debug("Issue {} ignored by exclusion pattern {}", issue, pattern);
  }
}
