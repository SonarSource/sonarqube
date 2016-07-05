/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.scanner.issue;

import com.google.common.base.Strings;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.Issue.Flow;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.index.BatchComponent;
import org.sonar.scanner.index.BatchComponentCache;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.IssueLocation;
import org.sonar.scanner.report.ReportPublisher;

/**
 * Initialize the issues raised during scan.
 */
public class ModuleIssues {

  private final ActiveRules activeRules;
  private final Rules rules;
  private final IssueFilters filters;
  private final ReportPublisher reportPublisher;
  private final BatchComponentCache componentCache;

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

    ScannerReport.Issue.Builder builder = ScannerReport.Issue.newBuilder();
    ScannerReport.IssueLocation.Builder locationBuilder = IssueLocation.newBuilder();
    ScannerReport.TextRange.Builder textRangeBuilder = ScannerReport.TextRange.newBuilder();
    // non-null fields
    builder.setSeverity(severity);
    builder.setRuleRepository(issue.ruleKey().repository());
    builder.setRuleKey(issue.ruleKey().rule());
    builder.setMsg(primaryMessage);
    locationBuilder.setMsg(primaryMessage);

    locationBuilder.setComponentRef(component.batchId());
    TextRange primaryTextRange = issue.primaryLocation().textRange();
    if (primaryTextRange != null) {
      builder.setTextRange(toProtobufTextRange(textRangeBuilder, primaryTextRange));
    }
    Double gap = issue.gap();
    if (gap != null) {
      builder.setGap(gap);
    }
    applyFlows(builder, locationBuilder, textRangeBuilder, issue);
    ScannerReport.Issue rawIssue = builder.build();

    if (filters.accept(inputComponent.key(), rawIssue)) {
      write(component, rawIssue);
      return true;
    }
    return false;
  }

  private void applyFlows(ScannerReport.Issue.Builder builder, ScannerReport.IssueLocation.Builder locationBuilder, ScannerReport.TextRange.Builder textRangeBuilder, Issue issue) {
    ScannerReport.Flow.Builder flowBuilder = ScannerReport.Flow.newBuilder();
    for (Flow flow : issue.flows()) {
      if (!flow.locations().isEmpty()) {
        flowBuilder.clear();
        for (org.sonar.api.batch.sensor.issue.IssueLocation location : flow.locations()) {
          locationBuilder.clear();
          locationBuilder.setComponentRef(componentCache.get(location.inputComponent()).batchId());
          String message = location.message();
          if (message != null) {
            locationBuilder.setMsg(message);
          }
          TextRange textRange = location.textRange();
          if (textRange != null) {
            locationBuilder.setTextRange(toProtobufTextRange(textRangeBuilder, textRange));
          }
          flowBuilder.addLocation(locationBuilder.build());
        }
        builder.addFlow(flowBuilder.build());
      }
    }
  }

  private static ScannerReport.TextRange toProtobufTextRange(ScannerReport.TextRange.Builder textRangeBuilder, TextRange primaryTextRange) {
    textRangeBuilder.clear();
    textRangeBuilder.setStartLine(primaryTextRange.start().line());
    textRangeBuilder.setStartOffset(primaryTextRange.start().lineOffset());
    textRangeBuilder.setEndLine(primaryTextRange.end().line());
    textRangeBuilder.setEndOffset(primaryTextRange.end().lineOffset());
    return textRangeBuilder.build();
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

  public void write(BatchComponent component, ScannerReport.Issue rawIssue) {
    reportPublisher.getWriter().appendComponentIssue(component.batchId(), rawIssue);
  }

}
