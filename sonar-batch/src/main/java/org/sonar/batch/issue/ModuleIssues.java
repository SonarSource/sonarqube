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

import com.google.common.base.Strings;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.batch.rule.Severity;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.Constants;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.core.issue.DefaultIssue;

/**
 * Initialize the issues raised during scan.
 */
public class ModuleIssues {

  private final ActiveRules activeRules;
  private final Rules rules;
  private final Project project;
  private final IssueFilters filters;
  private final ReportPublisher reportPublisher;
  private final BatchComponentCache componentCache;
  private final BatchReport.Issue.Builder builder = BatchReport.Issue.newBuilder();

  public ModuleIssues(ActiveRules activeRules, Rules rules, Project project, IssueFilters filters, ReportPublisher reportPublisher, BatchComponentCache componentCache) {
    this.activeRules = activeRules;
    this.rules = rules;
    this.project = project;
    this.filters = filters;
    this.reportPublisher = reportPublisher;
    this.componentCache = componentCache;
  }

  public boolean initAndAddIssue(Issue issue) {
    BatchComponent component;
    InputPath inputPath = issue.locations().get(0).inputPath();
    if (inputPath != null) {
      component = componentCache.get(inputPath);
    } else {
      component = componentCache.get(project);
    }
    DefaultIssue defaultIssue = toDefaultIssue(project.getKey(), component.key(), issue);
    RuleKey ruleKey = defaultIssue.ruleKey();
    Rule rule = rules.find(ruleKey);
    validateRule(defaultIssue, rule);
    ActiveRule activeRule = activeRules.find(ruleKey);
    if (activeRule == null) {
      // rule does not exist or is not enabled -> ignore the issue
      return false;
    }
    updateIssue(defaultIssue, rule, activeRule);
    if (filters.accept(defaultIssue)) {
      write(component, defaultIssue);
      return true;
    }
    return false;
  }

  public void write(BatchComponent component, DefaultIssue issue) {
    reportPublisher.getWriter().appendComponentIssue(component.batchId(), toReportIssue(builder, issue));
  }

  private static BatchReport.Issue toReportIssue(BatchReport.Issue.Builder builder, DefaultIssue issue) {
    builder.clear();
    // non-null fields
    builder.setSeverity(Constants.Severity.valueOf(issue.severity()));
    builder.setRuleRepository(issue.ruleKey().repository());
    builder.setRuleKey(issue.ruleKey().rule());
    builder.setAttributes(KeyValueFormat.format(issue.attributes()));

    // nullable fields
    Integer line = issue.line();
    if (line != null) {
      builder.setLine(line);
    }
    String message = issue.message();
    if (message != null) {
      builder.setMsg(message);
    }
    Double effortToFix = issue.effortToFix();
    if (effortToFix != null) {
      builder.setEffortToFix(effortToFix);
    }
    return builder.build();
  }

  public static DefaultIssue toDefaultIssue(String projectKey, String componentKey, Issue issue) {
    Severity overriddenSeverity = issue.overriddenSeverity();
    TextRange textRange = issue.locations().get(0).textRange();
    return new org.sonar.core.issue.DefaultIssueBuilder()
      .componentKey(componentKey)
      .projectKey(projectKey)
      .ruleKey(RuleKey.of(issue.ruleKey().repository(), issue.ruleKey().rule()))
      .effortToFix(issue.effortToFix())
      .line(textRange != null ? textRange.start().line() : null)
      .message(issue.locations().get(0).message())
      .severity(overriddenSeverity != null ? overriddenSeverity.name() : null)
      .build();
  }

  private static void validateRule(DefaultIssue issue, @Nullable Rule rule) {
    RuleKey ruleKey = issue.ruleKey();
    if (rule == null) {
      throw MessageException.of(String.format("The rule '%s' does not exist.", ruleKey));
    }
    if (Strings.isNullOrEmpty(rule.name()) && Strings.isNullOrEmpty(issue.message())) {
      throw MessageException.of(String.format("The rule '%s' has no name and the related issue has no message.", ruleKey));
    }
  }

  private void updateIssue(DefaultIssue issue, @Nullable Rule rule, ActiveRule activeRule) {
    if (Strings.isNullOrEmpty(issue.message())) {
      issue.setMessage(rule.name());
    }
    if (project != null) {
      issue.setCreationDate(project.getAnalysisDate());
      issue.setUpdateDate(project.getAnalysisDate());
    }
    if (issue.severity() == null) {
      issue.setSeverity(activeRule.severity());
    }
  }
}
