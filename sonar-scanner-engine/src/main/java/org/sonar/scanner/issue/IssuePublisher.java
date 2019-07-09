/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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

import java.util.Collection;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.Issue.Flow;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.IssueLocation;
import org.sonar.scanner.protocol.output.ScannerReport.IssueType;
import org.sonar.scanner.report.ReportPublisher;

/**
 * Initialize the issues raised during scan.
 */
@ThreadSafe
public class IssuePublisher {

  private final ActiveRules activeRules;
  private final IssueFilters filters;
  private final ReportPublisher reportPublisher;

  public IssuePublisher(ActiveRules activeRules, IssueFilters filters, ReportPublisher reportPublisher) {
    this.activeRules = activeRules;
    this.filters = filters;
    this.reportPublisher = reportPublisher;
  }

  public boolean initAndAddIssue(Issue issue) {
    DefaultInputComponent inputComponent = (DefaultInputComponent) issue.primaryLocation().inputComponent();

    if (noSonar(inputComponent, issue)) {
      return false;
    }

    ActiveRule activeRule = activeRules.find(issue.ruleKey());
    if (activeRule == null) {
      // rule does not exist or is not enabled -> ignore the issue
      return false;
    }

    ScannerReport.Issue rawIssue = createReportIssue(issue, inputComponent.scannerId(), activeRule.severity());

    if (filters.accept(inputComponent, rawIssue)) {
      write(inputComponent.scannerId(), rawIssue);
      return true;
    }
    return false;
  }

  private static boolean noSonar(DefaultInputComponent inputComponent, Issue issue) {
    TextRange textRange = issue.primaryLocation().textRange();
    return inputComponent.isFile()
      && textRange != null
      && ((DefaultInputFile) inputComponent).hasNoSonarAt(textRange.start().line())
      && !StringUtils.containsIgnoreCase(issue.ruleKey().rule(), "nosonar");
  }

  public void initAndAddExternalIssue(ExternalIssue issue) {
    DefaultInputComponent inputComponent = (DefaultInputComponent) issue.primaryLocation().inputComponent();
    ScannerReport.ExternalIssue rawExternalIssue = createReportExternalIssue(issue, inputComponent.scannerId());
    write(inputComponent.scannerId(), rawExternalIssue);
  }

  private static String nullToEmpty(@Nullable String str) {
    if (str == null) {
      return "";
    }
    return str;
  }

  private static ScannerReport.Issue createReportIssue(Issue issue, int componentRef, String activeRuleSeverity) {
    String primaryMessage = nullToEmpty(issue.primaryLocation().message());
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
    applyFlows(builder::addFlow, locationBuilder, textRangeBuilder, issue.flows());
    return builder.build();
  }

  private static ScannerReport.ExternalIssue createReportExternalIssue(ExternalIssue issue, int componentRef) {
    // primary location of an external issue must have a message
    String primaryMessage = issue.primaryLocation().message();
    Severity severity = Severity.valueOf(issue.severity().name());
    IssueType issueType = IssueType.valueOf(issue.type().name());

    ScannerReport.ExternalIssue.Builder builder = ScannerReport.ExternalIssue.newBuilder();
    ScannerReport.IssueLocation.Builder locationBuilder = IssueLocation.newBuilder();
    ScannerReport.TextRange.Builder textRangeBuilder = ScannerReport.TextRange.newBuilder();
    // non-null fields
    builder.setSeverity(severity);
    builder.setType(issueType);
    builder.setEngineId(issue.engineId());
    builder.setRuleId(issue.ruleId());
    builder.setMsg(primaryMessage);
    locationBuilder.setMsg(primaryMessage);

    locationBuilder.setComponentRef(componentRef);
    TextRange primaryTextRange = issue.primaryLocation().textRange();
    if (primaryTextRange != null) {
      builder.setTextRange(toProtobufTextRange(textRangeBuilder, primaryTextRange));
    }
    Long effort = issue.remediationEffort();
    if (effort != null) {
      builder.setEffort(effort);
    }
    applyFlows(builder::addFlow, locationBuilder, textRangeBuilder, issue.flows());
    return builder.build();
  }

  private static void applyFlows(Consumer<ScannerReport.Flow> consumer, ScannerReport.IssueLocation.Builder locationBuilder,
    ScannerReport.TextRange.Builder textRangeBuilder, Collection<Flow> flows) {
    ScannerReport.Flow.Builder flowBuilder = ScannerReport.Flow.newBuilder();
    for (Flow flow : flows) {
      if (flow.locations().isEmpty()) {
        return;
      }
      flowBuilder.clear();
      for (org.sonar.api.batch.sensor.issue.IssueLocation location : flow.locations()) {
        int locationComponentRef = ((DefaultInputComponent) location.inputComponent()).scannerId();
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
      consumer.accept(flowBuilder.build());
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

  public void write(int batchId, ScannerReport.Issue rawIssue) {
    reportPublisher.getWriter().appendComponentIssue(batchId, rawIssue);
  }

  public void write(int batchId, ScannerReport.ExternalIssue rawIssue) {
    reportPublisher.getWriter().appendComponentExternalIssue(batchId, rawIssue);
  }
}
