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
package org.sonar.batch.issue;

import com.google.common.base.Strings;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.technicaldebt.TechnicalDebtCalculator;
import org.sonar.core.issue.DefaultIssueBuilder;

import javax.annotation.Nullable;

/**
 * Initialize the issues raised during scan.
 */
public class ModuleIssues {

  private final RulesProfile qProfile;
  private final IssueCache cache;
  private final Project project;
  private final IssueFilters filters;
  private final TechnicalDebtCalculator technicalDebtCalculator;
  private final RuleFinder ruleFinder;

  public ModuleIssues(RulesProfile qProfile, IssueCache cache, Project project, IssueFilters filters, TechnicalDebtCalculator technicalDebtCalculator, RuleFinder ruleFinder) {
    this.qProfile = qProfile;
    this.cache = cache;
    this.project = project;
    this.filters = filters;
    this.technicalDebtCalculator = technicalDebtCalculator;
    this.ruleFinder = ruleFinder;
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
    Rule rule = ruleFinder.findByKey(ruleKey);
    if (rule == null) {
      throw MessageException.of(String.format("The rule '%s' does not exists.", ruleKey));
    }
    ActiveRule activeRule = qProfile.getActiveRule(ruleKey.repository(), ruleKey.rule());
    if (activeRule == null || activeRule.getRule() == null) {
      // rule does not exist or is not enabled -> ignore the issue
      return false;
    }

    if (Strings.isNullOrEmpty(issue.message())) {
      issue.setMessage(rule.getName());
    }
    issue.setCreationDate(project.getAnalysisDate());
    issue.setUpdateDate(project.getAnalysisDate());
    if (issue.severity() == null) {
      issue.setSeverity(activeRule.getSeverity().name());
    }
    issue.setTechnicalDebt(technicalDebtCalculator.calculTechnicalDebt(issue));

    if (filters.accept(issue, violation)) {
      cache.put(issue);
      return true;
    }
    return false;
  }

}
