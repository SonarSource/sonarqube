/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.sonar.api.utils.WildcardPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.issue.DefaultFilterableIssue;

public class IgnoreIssuesFilter implements IssueFilter {

  private static final Logger LOG = LoggerFactory.getLogger(IgnoreIssuesFilter.class);
  private final DefaultActiveRules activeRules;
  private final AnalysisWarnings analysisWarnings;
  private final Map<InputComponent, List<WildcardPattern>> rulePatternByComponent = new HashMap<>();
  private final Set<RuleKey> warnedDeprecatedRuleKeys = new LinkedHashSet<>();

  public IgnoreIssuesFilter(DefaultActiveRules activeRules, AnalysisWarnings analysisWarnings) {
    this.activeRules = activeRules;
    this.analysisWarnings = analysisWarnings;
  }

  @Override
  public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
    InputComponent component = ((DefaultFilterableIssue) issue).getComponent();

    if (isIgnoreIssue(component, issue)) {
      return false;
    }

    if (hasRuleMatchFor(component, issue)) {
      return false;
    }
    return chain.accept(issue);
  }

  private static boolean isIgnoreIssue(InputComponent component, FilterableIssue issue) {
    if (component.isFile()) {
      DefaultInputFile inputFile = (DefaultInputFile) component;
      return inputFile.isIgnoreAllIssues() || inputFile.isIgnoreAllIssuesOnLine(issue.line());
    }
    return false;
  }

  public void addRuleExclusionPatternForComponent(DefaultInputFile inputFile, WildcardPattern rulePattern) {
    if ("*".equals(rulePattern.toString())) {
      inputFile.setIgnoreAllIssues(true);
    } else {
      rulePatternByComponent.computeIfAbsent(inputFile, x -> new LinkedList<>()).add(rulePattern);
    }
  }

  private boolean hasRuleMatchFor(InputComponent component, FilterableIssue issue) {
    for (WildcardPattern pattern : rulePatternByComponent.getOrDefault(component, Collections.emptyList())) {
      if (pattern.match(issue.ruleKey().toString())) {
        LOG.debug("Issue '{}' ignored by exclusion pattern '{}'", issue, pattern);
        return true;
      }

      RuleKey ruleKey = issue.ruleKey();
      if (activeRules.matchesDeprecatedKeys(ruleKey, pattern)) {
        String msg = String.format("A multicriteria issue exclusion uses the rule key '%s' that has been changed. The pattern should be updated to '%s'", pattern, ruleKey);
        analysisWarnings.addUnique(msg);
        if (warnedDeprecatedRuleKeys.add(ruleKey)) {
          LOG.warn(msg);
        }
        LOG.debug("Issue '{}' ignored by exclusion pattern '{}' matching a deprecated rule key", issue, pattern);
        return true;
      }
    }
    return false;

  }
}
