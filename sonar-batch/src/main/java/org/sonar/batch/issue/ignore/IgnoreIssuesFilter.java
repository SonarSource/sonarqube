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
package org.sonar.batch.issue.ignore;

import org.sonar.api.scan.issue.filter.FilterableIssue;

import org.sonar.batch.issue.ignore.pattern.IssueExclusionPatternInitializer;
import org.sonar.batch.issue.ignore.pattern.IssuePattern;
import org.sonar.batch.issue.ignore.pattern.PatternMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.scan.issue.filter.IssueFilterChain;

public class IgnoreIssuesFilter implements IssueFilter {

  private PatternMatcher patternMatcher;

  private static final Logger LOG = LoggerFactory.getLogger(IgnoreIssuesFilter.class);

  public IgnoreIssuesFilter(IssueExclusionPatternInitializer patternInitializer) {
    this.patternMatcher = patternInitializer.getPatternMatcher();
  }

  @Override
  public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
    if (hasMatchFor(issue)) {
      return false;
    } else {
      return chain.accept(issue);
    }
  }

  private boolean hasMatchFor(FilterableIssue issue) {
    IssuePattern pattern = patternMatcher.getMatchingPattern(issue);
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
