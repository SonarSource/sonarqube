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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFilter;
import org.sonar.plugins.core.issue.ignore.pattern.Pattern;
import org.sonar.plugins.core.issue.ignore.pattern.PatternsInitializer;

import java.util.List;

public final class IgnoreIssuesFilter implements IssueFilter {

  private static final Logger LOG = LoggerFactory.getLogger(IgnoreIssuesFilter.class);

  private PatternsInitializer patternsInitializer;

  public IgnoreIssuesFilter(PatternsInitializer patternsInitializer) {
    this.patternsInitializer = patternsInitializer;
  }

  public boolean accept(Issue issue) {
    Pattern extraPattern = patternsInitializer.getExtraPattern(issue.componentKey());
    LOG.debug("Extra pattern for resource {}: {}", issue.componentKey(), extraPattern);
    if (extraPattern != null && extraPattern.match(issue)) {
      logExclusion(issue, extraPattern);
      return false;
    }

    List<Pattern> patterns = patternsInitializer.getMulticriteriaPatterns();
    for (Pattern pattern : patterns) {
      if (pattern.match(issue)) {
        logExclusion(issue, pattern);
        return false;
      }
    }
    return true;
  }

  private void logExclusion(Issue issue, Pattern pattern) {
    LOG.debug("Issue {} ignored by {}", issue, pattern);
  }

}
