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

import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Violation;
import org.sonar.core.issue.DefaultIssueBuilder;
import org.sonar.core.technicaldebt.TechnicalDebtCalculator;

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

  public ModuleIssues(RulesProfile qProfile, IssueCache cache, Project project, IssueFilters filters, TechnicalDebtCalculator technicalDebtCalculator) {
    this.qProfile = qProfile;
    this.cache = cache;
    this.project = project;
    this.filters = filters;
    this.technicalDebtCalculator = technicalDebtCalculator;
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
    // TODO fail fast : if rule does not exist

    ActiveRule activeRule = qProfile.getActiveRule(issue.ruleKey().repository(), issue.ruleKey().rule());
    if (activeRule == null || activeRule.getRule() == null) {
      // rule does not exist or is not enabled -> ignore the issue
      return false;
    }
    issue.setCreationDate(project.getAnalysisDate());
    issue.setUpdateDate(project.getAnalysisDate());
    if (issue.severity() == null) {
      issue.setSeverity(activeRule.getSeverity().name());
    }
    issue.setRemediationCost(technicalDebtCalculator.cost(issue));

    if (filters.accept(issue, violation)) {
      cache.put(issue);
      return true;
    }
    return false;
  }
}
