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

import org.sonar.batch.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.batch.issue.ignore.pattern.IssuePattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilter;
import org.sonar.api.issue.batch.IssueFilterChain;

public class EnforceIssuesFilter implements IssueFilter {

  private IssueInclusionPatternInitializer patternInitializer;

  private static final Logger LOG = LoggerFactory.getLogger(EnforceIssuesFilter.class);

  public EnforceIssuesFilter(IssueInclusionPatternInitializer patternInitializer) {
    this.patternInitializer = patternInitializer;
  }

  @Override
  public boolean accept(Issue issue, IssueFilterChain chain) {
    boolean atLeastOneRuleMatched = false;
    boolean atLeastOnePatternFullyMatched = false;
    IssuePattern matchingPattern = null;

    for (IssuePattern pattern : patternInitializer.getMulticriteriaPatterns()) {
      if (pattern.getRulePattern().match(issue.ruleKey().toString())) {
        atLeastOneRuleMatched = true;
        String pathForComponent = patternInitializer.getPathForComponent(issue.componentKey());
        if (pathForComponent != null && pattern.getResourcePattern().match(pathForComponent)) {
          atLeastOnePatternFullyMatched = true;
          matchingPattern = pattern;
        }
      }
    }

    if (atLeastOneRuleMatched) {
      if (atLeastOnePatternFullyMatched) {
        LOG.debug("Issue {} enforced by pattern {}", issue, matchingPattern);
      }
      return atLeastOnePatternFullyMatched;
    } else {
      return chain.accept(issue);
    }
  }
}
