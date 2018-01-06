/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.issue;

import com.google.common.base.Strings;
import javax.annotation.concurrent.ThreadSafe;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.Issue.Flow;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.MessageException;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.IssueLocation;
import org.sonar.scanner.report.ReportPublisher;

/**
 * Initialize the issues raised during scan.
 */
@ThreadSafe
public class ModuleIssues {

  private final ActiveRules activeRules;
  private final Rules rules;
  private final IssueFilters filters;
  private final ReportPublisher reportPublisher;

  public ModuleIssues(ActiveRules activeRules, Rules rules, IssueFilters filters, ReportPublisher reportPublisher) {
    this.activeRules = activeRules;
    this.rules = rules;
    this.filters = filters;
    this.reportPublisher = reportPublisher;
  }

  public boolean initAndAddIssue(Issue issue) {
    DefaultInputComponent inputComponent = (DefaultInputComponent) issue.primaryLocation().inputComponent();

    Rule rule = validateRule(issue);
    ActiveRule activeRule = activeRules.find(issue.ruleKey());
    if (activeRule == null) {
      // rule does not exist or is not enabled -> ignore the issue
      return false;
    }

    ScannerReport.Issue rawIssue = createReportIssue(issue, inputComponent.batchId(), rule.name(), activeRule.severity());

    if (filters.accept(inputComponent.key(), rawIssue)) {
      write(inputComponent.batchId(), rawIssue);
      return true;
    }
    return false;
  }

  private static ScannerReport.Issue createReportIssue(Issue issue, int componentRef, String ruleName, String activeRuleSeverity) {
    String primaryMessage = Strings.isNullOrEmpty(issue.primaryLocation().message()) ? ruleName : issue.primaryLocation().message();
    org.sonar.api.batch.rule.Severity overriddenSeverity = issue.overriddenSeverity();
    Severity severity = overriddenSeverity != null ? Severity.valueOf(overriddenSeverity.name()) : Severity.valueOf(activeRuleSeverity);

    ScannerReport.Issue.Builder builder = ScannerReport.Issue.newBuilder();
    ScannerReport.IssueLocation.Builder locationBuilder = IssueLocation.newBuilder();
    ScannerReport.TextRange.Builder textRangeBuilder = ScannerReport.TextRange.newBuilder();
    // non-null fields
    builder.setSeverity(severity);
    builder.setRuleRepository(issue.ruleKey().repository());
    builder.setRuleKey(issue.ruleKey().rule());
    builder.setMsg(primaryMessage);
    locationBuilder.setMsg(primaryMessage);

    locationBuilder.setComponentRef(componentRef);
    TextRange primaryTextRange = issue.primaryLocation().textRange();
    if (primaryTextRange != null) {
      builder.setTextRange(toProtobufTextRange(textRangeBuilder, primaryTextRange));
    }
    Double gap = issue.gap();
    if (gap != null) {
      builder.setGap(gap);
    }
    applyFlows(componentRef, builder, locationBuilder, textRangeBuilder, issue);
    return builder.build();
  }

  private static void applyFlows(int componentRef, ScannerReport.Issue.Builder builder, ScannerReport.IssueLocation.Builder locationBuilder,
    ScannerReport.TextRange.Builder textRangeBuilder, Issue issue) {
    ScannerReport.Flow.Builder flowBuilder = ScannerReport.Flow.newBuilder();
    for (Flow flow : issue.flows()) {
      if (flow.locations().isEmpty()) {
        return;
      }
      flowBuilder.clear();
      for (org.sonar.api.batch.sensor.issue.IssueLocation location : flow.locations()) {
        int locationComponentRef = ((DefaultInputComponent) location.inputComponent()).batchId();
        if (locationComponentRef != componentRef) {
          // Some analyzers are trying to report cross file secondary locations. The API was designed to support it, but server side is not
          // ready to handle it (especially the UI)
          // So let's skip them for now (SONAR-9929)
          continue;
        }
        locationBuilder.clear();
        locationBuilder.setComponentRef(locationComponentRef);
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

  public void write(int batchId, ScannerReport.Issue rawIssue) {
    reportPublisher.getWriter().appendComponentIssue(batchId, rawIssue);
  }

}
