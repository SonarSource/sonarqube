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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.ModuleIssues;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Violation;
import org.sonar.core.issue.DefaultIssueBuilder;

import javax.annotation.Nullable;

/**
 * Initialize the issues raised during scan.
 */
public class DefaultModuleIssues implements ModuleIssues {

  private final RulesProfile qProfile;
  private final IssueCache cache;
  private final Project project;
  private final IssueFilters filters;

  public DefaultModuleIssues(RulesProfile qProfile, IssueCache cache, Project project, IssueFilters filters) {
    this.qProfile = qProfile;
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

    if (filters.accept(issue, violation)) {
      cache.put(issue);
      return true;
    }
    return false;
  }

  @Override
  public Iterable<Issue> issues() {
    return (Iterable) Iterables.filter(cache.all(), new ModulePredicate(false));
  }

  @Override
  public Iterable<Issue> resolvedIssues() {
    return (Iterable) Iterables.filter(cache.all(), new ModulePredicate(true));
  }

  private class ModulePredicate implements Predicate<DefaultIssue> {
    private final boolean resolved;

    private ModulePredicate(boolean resolved) {
      this.resolved = resolved;
    }

    @Override
    public boolean apply(@Nullable DefaultIssue issue) {
      if (issue != null && (issue.componentKey().equals(project.getEffectiveKey()) || issue.componentKey().startsWith(project.getEffectiveKey() + ":"))) {
        return resolved ? issue.resolution() != null : issue.resolution()==null;
      }
      return false;
    }
  }
}
