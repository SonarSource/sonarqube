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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.concurrent.ThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.scanner.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.IssuePattern;
import org.sonar.scanner.scan.filesystem.InputComponentStore;

@ThreadSafe
public class EnforceIssuesFilter implements IssueFilter {
  private static final Logger LOG = LoggerFactory.getLogger(EnforceIssuesFilter.class);

  private final List<IssuePattern> multicriteriaPatterns;
  private final InputComponentStore componentStore;

  public EnforceIssuesFilter(IssueInclusionPatternInitializer patternInitializer, InputComponentStore componentStore) {
    this.multicriteriaPatterns = Collections.unmodifiableList(new ArrayList<>(patternInitializer.getMulticriteriaPatterns()));
    this.componentStore = componentStore;
  }

  @Override
  public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
    boolean atLeastOneRuleMatched = false;
    boolean atLeastOnePatternFullyMatched = false;
    IssuePattern matchingPattern = null;

    for (IssuePattern pattern : multicriteriaPatterns) {
      if (pattern.getRulePattern().match(issue.ruleKey().toString())) {
        atLeastOneRuleMatched = true;
        String relativePath = getRelativePath(issue.componentKey());
        if (relativePath != null && pattern.getResourcePattern().match(relativePath)) {
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

  @CheckForNull
  private String getRelativePath(String componentKey) {
    InputComponent component = componentStore.getByKey(componentKey);
    if (component == null || !component.isFile()) {
      return null;
    }
    InputFile inputPath = (InputFile) component;
    return inputPath.relativePath();
  }
}
