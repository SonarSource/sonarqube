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
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.Issue.ExecutionFlow;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.Constants.Severity;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReport.IssueLocation;
import org.sonar.batch.protocol.output.BatchReport.IssueLocation.Builder;
import org.sonar.batch.report.ReportPublisher;

/**
 * Initialize the issues raised during scan.
 */
public class ModuleIssues {

  private final ActiveRules activeRules;
  private final Rules rules;
  private final IssueFilters filters;
  private final ReportPublisher reportPublisher;
  private final BatchComponentCache componentCache;
  private final BatchReport.Issue.Builder builder = BatchReport.Issue.newBuilder();
  private final Builder locationBuilder = IssueLocation.newBuilder();
  private final org.sonar.batch.protocol.output.BatchReport.TextRange.Builder textRangeBuilder = org.sonar.batch.protocol.output.BatchReport.TextRange.newBuilder();
  private final BatchReport.ExecutionFlow.Builder flowBuilder = BatchReport.ExecutionFlow.newBuilder();

  public ModuleIssues(ActiveRules activeRules, Rules rules, IssueFilters filters, ReportPublisher reportPublisher, BatchComponentCache componentCache) {
    this.activeRules = activeRules;
    this.rules = rules;
    this.filters = filters;
    this.reportPublisher = reportPublisher;
    this.componentCache = componentCache;
  }

  public boolean initAndAddIssue(Issue issue) {
    InputComponent inputComponent = issue.primaryLocation().inputComponent();
    BatchComponent component = componentCache.get(inputComponent);

    Rule rule = validateRule(issue);
    ActiveRule activeRule = activeRules.find(issue.ruleKey());
    if (activeRule == null) {
      // rule does not exist or is not enabled -> ignore the issue
      return false;
    }

    String primaryMessage = Strings.isNullOrEmpty(issue.primaryLocation().message()) ? rule.name() : issue.primaryLocation().message();
    org.sonar.api.batch.rule.Severity overriddenSeverity = issue.overriddenSeverity();
    Severity severity = overriddenSeverity != null ? Severity.valueOf(overriddenSeverity.name()) : Severity.valueOf(activeRule.severity());

    builder.clear();
    locationBuilder.clear();
    // non-null fields
    builder.setSeverity(severity);
    builder.setRuleRepository(issue.ruleKey().repository());
    builder.setRuleKey(issue.ruleKey().rule());
    builder.setAttributes(KeyValueFormat.format(issue.attributes()));
    builder.setMsg(primaryMessage);
    locationBuilder.setMsg(primaryMessage);

    locationBuilder.setComponentRef(component.batchId());
    TextRange primaryTextRange = issue.primaryLocation().textRange();
    applyTextRange(primaryTextRange);
    if (primaryTextRange != null) {
      builder.setLine(primaryTextRange.start().line());
    }
    builder.setPrimaryLocation(locationBuilder.build());
    Double effortToFix = issue.effortToFix();
    if (effortToFix != null) {
      builder.setEffortToFix(effortToFix);
    }
    applyAdditionalLocations(issue);
    applyExecutionFlows(issue);
    BatchReport.Issue rawIssue = builder.build();

    if (filters.accept(inputComponent.key(), rawIssue)) {
      write(component, rawIssue);
      return true;
    }
    return false;
  }

  private void applyAdditionalLocations(Issue issue) {
    for (org.sonar.api.batch.sensor.issue.IssueLocation additionalLocation : issue.locations()) {
      locationBuilder.clear();
      locationBuilder.setComponentRef(componentCache.get(additionalLocation.inputComponent()).batchId());
      String message = additionalLocation.message();
      if (message != null) {
        locationBuilder.setMsg(message);
      }
      applyTextRange(additionalLocation.textRange());
      builder.addAdditionalLocation(locationBuilder.build());
    }
  }

  private void applyExecutionFlows(Issue issue) {
    for (ExecutionFlow executionFlow : issue.executionFlows()) {
      flowBuilder.clear();
      for (org.sonar.api.batch.sensor.issue.IssueLocation location : executionFlow.locations()) {
        locationBuilder.clear();
        locationBuilder.setComponentRef(componentCache.get(location.inputComponent()).batchId());
        String message = location.message();
        if (message != null) {
          locationBuilder.setMsg(message);
        }
        applyTextRange(location.textRange());
        flowBuilder.addLocation(locationBuilder.build());
      }
      builder.addExecutionFlow(flowBuilder.build());
    }
  }

  private void applyTextRange(TextRange primaryTextRange) {
    if (primaryTextRange != null) {
      textRangeBuilder.clear();
      textRangeBuilder.setStartLine(primaryTextRange.start().line());
      textRangeBuilder.setStartOffset(primaryTextRange.start().lineOffset());
      textRangeBuilder.setEndLine(primaryTextRange.end().line());
      textRangeBuilder.setEndOffset(primaryTextRange.end().lineOffset());
      locationBuilder.setTextRange(textRangeBuilder.build());
    }
  }

  private Rule validateRule(Issue issue) {
    RuleKey ruleKey = issue.ruleKey();
    Rule rule = rules.find(ruleKey);
    if (rule == null) {
      throw MessageException.of(String.format("The rule '%s' does not exist.", ruleKey));
    }
    if (Strings.isNullOrEmpty(rule.name()) && Strings.isNullOrEmpty(issue.primaryLocation().message())) {
      throw MessageException.of(String.format("The rule '%s' has no name and the related issue has no message.", ruleKey));
    }
    return rule;
  }

  public void write(BatchComponent component, BatchReport.Issue rawIssue) {
    reportPublisher.getWriter().appendComponentIssue(component.batchId(), rawIssue);
  }

}
