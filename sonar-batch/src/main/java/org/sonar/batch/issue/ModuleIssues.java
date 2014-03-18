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
package org.sonar.batch.issue;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.sonar.api.batch.rule.*;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.MessageException;
import org.sonar.core.issue.DefaultIssueBuilder;

import javax.annotation.Nullable;

/**
 * Initialize the issues raised during scan.
 */
public class ModuleIssues {

  private final ActiveRules activeRules;
  private final Rules rules;
  private final IssueCache cache;
  private final Project project;
  private final IssueFilters filters;

  public ModuleIssues(ActiveRules activeRules, Rules rules, IssueCache cache, Project project, IssueFilters filters) {
    this.activeRules = activeRules;
    this.rules = rules;
    this.cache = cache;
    this.project = project;
    this.filters = filters;
  }

  public boolean initAndAddIssue(DefaultIssue issue) {
    return initAndAddIssue(issue, null);
  }

  public boolean initAndAddViolation(Violation violation) {
    DefaultIssue issue = newIssue(violation);
    return initAndAddIssue(issue, violation);
  }

  private DefaultIssue newIssue(Violation violation) {
    return (DefaultIssue) new DefaultIssueBuilder()
      .componentKey(violation.getResource().getEffectiveKey())
      .ruleKey(RuleKey.of(violation.getRule().getRepositoryKey(), violation.getRule().getKey()))
      .effortToFix(violation.getCost())
      .line(violation.getLineId())
      .message(violation.getMessage())
      .severity(violation.getSeverity() != null ? violation.getSeverity().name() : null)
      .build();
  }

  private boolean initAndAddIssue(DefaultIssue issue, @Nullable Violation violation) {
    RuleKey ruleKey = issue.ruleKey();
    Rule rule = rules.find(ruleKey);
    validateRule(issue, rule);
    ActiveRule activeRule = activeRules.find(ruleKey);
    if (activeRule == null) {
      // rule does not exist or is not enabled -> ignore the issue
      return false;
    }
    updateIssue(issue, rule, activeRule);
    if (filters.accept(issue, violation)) {
      cache.put(issue);
      return true;
    }
    return false;
  }

  private void validateRule(DefaultIssue issue, Rule rule) {
    RuleKey ruleKey = issue.ruleKey();
    if (rule == null) {
      throw MessageException.of(String.format("The rule '%s' does not exist.", ruleKey));
    }
    if (Strings.isNullOrEmpty(rule.name()) && Strings.isNullOrEmpty(issue.message())) {
      throw MessageException.of(String.format("The rule '%s' has no name and the related issue has no message.", ruleKey));
    }
  }

  private void updateIssue(DefaultIssue issue, Rule rule, ActiveRule activeRule) {
    if (Strings.isNullOrEmpty(issue.message())) {
      issue.setMessage(rule.name());
    }
    issue.setCreationDate(project.getAnalysisDate());
    issue.setUpdateDate(project.getAnalysisDate());
    if (issue.severity() == null) {
      issue.setSeverity(activeRule.severity());
    }
    DebtRemediationFunction function = rule.debtRemediationFunction();
    if (rule.debtCharacteristic() != null && function != null) {
      issue.setDebt(calculateDebt(function, issue.effortToFix(), rule.key()));
    }
  }

  private Duration calculateDebt(DebtRemediationFunction function, @Nullable Double effortToFix, RuleKey ruleKey) {
    if (DebtRemediationFunction.Type.CONSTANT_ISSUE.equals(function.type()) && effortToFix != null) {
      throw new IllegalArgumentException("Rule '" + ruleKey + "' can not use 'Constant/issue' remediation function " +
        "because this rule does not have a fixed remediation cost.");
    }
    Duration result = Duration.create(0);
    Duration factor = function.factor();
    Duration offset = function.offset();

    if (factor != null) {
      int effortToFixValue = Objects.firstNonNull(effortToFix, 1).intValue();
      result = factor.multiply(effortToFixValue);
    }
    if (offset != null) {
      result = result.add(offset);
    }
    return result;
  }

}
