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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.ThreadSafe;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.internal.DefaultActiveRules;
import org.sonar.api.notifications.AnalysisWarnings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.scan.issue.filter.FilterableIssue;
import org.sonar.api.scan.issue.filter.IssueFilter;
import org.sonar.api.scan.issue.filter.IssueFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.scanner.issue.DefaultFilterableIssue;
import org.sonar.scanner.issue.ignore.pattern.IssueInclusionPatternInitializer;
import org.sonar.scanner.issue.ignore.pattern.IssuePattern;

@ThreadSafe
public class EnforceIssuesFilter implements IssueFilter {
  private static final Logger LOG = LoggerFactory.getLogger(EnforceIssuesFilter.class);

  private final List<IssuePattern> multicriteriaPatterns;
  private final AnalysisWarnings analysisWarnings;
  private final DefaultActiveRules activeRules;
  private final Set<RuleKey> warnedDeprecatedRuleKeys = new LinkedHashSet<>();

  private boolean warnDeprecatedIssuePatternAlreadyLogged;

  public EnforceIssuesFilter(IssueInclusionPatternInitializer patternInitializer, AnalysisWarnings analysisWarnings, DefaultActiveRules activeRules) {
    this.multicriteriaPatterns = List.copyOf(patternInitializer.getMulticriteriaPatterns());
    this.analysisWarnings = analysisWarnings;
    this.activeRules = activeRules;
  }

  @Override
  public boolean accept(FilterableIssue issue, IssueFilterChain chain) {
    boolean atLeastOneRuleMatched = false;
    boolean atLeastOnePatternFullyMatched = false;
    IssuePattern matchingPattern = null;

    for (IssuePattern pattern : multicriteriaPatterns) {
      if (ruleMatches(pattern, issue.ruleKey())) {
        atLeastOneRuleMatched = true;
        InputComponent component = ((DefaultFilterableIssue) issue).getComponent();
        if (component.isFile()) {
          DefaultInputFile file = (DefaultInputFile) component;
          if (pattern.matchFile(file.getProjectRelativePath())) {
            atLeastOnePatternFullyMatched = true;
            matchingPattern = pattern;
          } else if (pattern.matchFile(file.getModuleRelativePath())) {
            warnOnceDeprecatedIssuePattern(
              "Specifying module-relative paths at project level in property '" + IssueInclusionPatternInitializer.CONFIG_KEY + "' is deprecated. " +
                "To continue matching files like '" + file.getProjectRelativePath() + "', update this property so that patterns refer to project-relative paths.");

            atLeastOnePatternFullyMatched = true;
            matchingPattern = pattern;
          }
        }
      }
    }

    if (atLeastOneRuleMatched) {
      if (atLeastOnePatternFullyMatched) {
        LOG.debug("Issue '{}' enforced by pattern '{}'", issue, matchingPattern);
      }
      return atLeastOnePatternFullyMatched;
    } else {
      return chain.accept(issue);
    }
  }

  private boolean ruleMatches(IssuePattern pattern, RuleKey ruleKey) {
    if (activeRules.matchesDeprecatedKeys(ruleKey, pattern.getRulePattern())) {
      String msg = String.format("A multicriteria issue enforce uses the rule key '%s' that has been changed. The pattern should be updated to '%s'",
        pattern.getRulePattern(), ruleKey);
      analysisWarnings.addUnique(msg);
      if (warnedDeprecatedRuleKeys.add(ruleKey)) {
        LOG.warn(msg);
      }
      return true;
    }
    return pattern.matchRule(ruleKey);
  }

  private void warnOnceDeprecatedIssuePattern(String msg) {
    if (!warnDeprecatedIssuePatternAlreadyLogged) {
      LOG.warn(msg);
      analysisWarnings.addUnique(msg);
      warnDeprecatedIssuePatternAlreadyLogged = true;
    }
  }

}
