/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.plugins.core.issue.ignore;

import org.sonar.plugins.core.issue.ignore.pattern.IssuePattern;

import org.sonar.plugins.core.issue.ignore.pattern.PatternMatcher;
import org.sonar.plugins.core.issue.ignore.pattern.AbstractPatternInitializer;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFilter;

public abstract class IssuesFilterBase implements IssueFilter {

  private PatternMatcher patternMatcher;

  protected IssuesFilterBase(AbstractPatternInitializer patternInitializer) {
    this.patternMatcher = patternInitializer.getPatternMatcher();
  }

  @Override
  public boolean accept(Issue issue) {
    IssuePattern pattern = patternMatcher.getMatchingPattern(issue);
    if (pattern != null) {
      logExclusion(issue, pattern);
      return false;
    }
    return true;
  }

  protected abstract void logExclusion(Issue issue, IssuePattern pattern);
}
