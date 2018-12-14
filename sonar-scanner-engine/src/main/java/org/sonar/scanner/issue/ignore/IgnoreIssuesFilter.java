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
package org.sonar.scanner.issue.ignore;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.scanner.issue.DefaultFilterableIssue;

public class IgnoreIssuesFilter implements IssueFilter {

  private Multimap<InputComponent, WildcardPattern> rulePatternByComponent = LinkedHashMultimap.create();

  private static final Logger LOG = Loggers.get(IgnoreIssuesFilter.class);

  @Override
  public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
    InputComponent component = ((DefaultFilterableIssue) issue).getComponent();
    if (component.isFile() && ((DefaultInputFile) component).isIgnoreAllIssues()) {
      return false;
    }
    if (component.isFile() && ((DefaultInputFile) component).isIgnoreAllIssuesOnLine(issue.line())) {
      return false;
    }
    if (hasRuleMatchFor(component, issue)) {
      return false;
    }
    return chain.accept(issue);
  }

  public void addRuleExclusionPatternForComponent(DefaultInputFile inputFile, WildcardPattern rulePattern) {
    if ("*".equals(rulePattern.toString())) {
      inputFile.setIgnoreAllIssues(true);
    } else {
      rulePatternByComponent.put(inputFile, rulePattern);
    }
  }

  private boolean hasRuleMatchFor(InputComponent component, FilterableIssue issue) {
    for (WildcardPattern pattern : rulePatternByComponent.get(component)) {
      if (pattern.match(issue.ruleKey().toString())) {
        LOG.debug("Issue {} ignored by exclusion pattern {}", issue, pattern);
        return true;
      }
    }
    return false;

  }
}
