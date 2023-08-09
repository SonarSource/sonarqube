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
package org.sonar.scanner.issue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultInputComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.sensor.issue.ExternalIssue;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.Issue.Flow;
import org.sonar.api.batch.sensor.issue.MessageFormatting;
import org.sonar.api.batch.sensor.issue.NewIssue.FlowType;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueFlow;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rules.CleanCodeAttribute;
import org.sonar.api.rules.RuleType;
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
    builder.addAllMsgFormatting(toProtobufMessageFormattings(issue.primaryLocation().messageFormattings()));
    builder.addAllOverridenImpacts(toProtobufImpacts(issue.overridenImpacts()));
    locationBuilder.setMsg(primaryMessage);
    locationBuilder.addAllMsgFormatting(toProtobufMessageFormattings(issue.primaryLocation().messageFormattings()));

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
    builder.setQuickFixAvailable(issue.isQuickFixAvailable());
    issue.ruleDescriptionContextKey().ifPresent(builder::setRuleDescriptionContextKey);
    List<String> codeVariants = issue.codeVariants();
    if (codeVariants != null) {
      builder.addAllCodeVariants(codeVariants);
    }
    return builder.build();
  }

  private static List<ScannerReport.Impact> toProtobufImpacts(Map<SoftwareQuality, org.sonar.api.issue.impact.Severity> softwareQualitySeverityMap) {
    List<ScannerReport.Impact> impacts = new ArrayList<>();
    softwareQualitySeverityMap.forEach((q, s) -> impacts.add(ScannerReport.Impact.newBuilder().setSoftwareQuality(q.name()).setSeverity(s.name()).build()));
    return impacts;
  }

  private static List<ScannerReport.MessageFormatting> toProtobufMessageFormattings(List<MessageFormatting> messageFormattings) {
    return messageFormattings.stream()
      .map(m -> ScannerReport.MessageFormatting.newBuilder()
        .setStart(m.start())
        .setEnd(m.end())
        .setType(ScannerReport.MessageFormattingType.valueOf(m.type().name()))
        .build())
      .collect(Collectors.toList());
  }

  private static ScannerReport.ExternalIssue createReportExternalIssue(ExternalIssue issue, int componentRef) {
    // primary location of an external issue must have a message
    String primaryMessage = issue.primaryLocation().message();
    ScannerReport.ExternalIssue.Builder builder = ScannerReport.ExternalIssue.newBuilder();
    ScannerReport.IssueLocation.Builder locationBuilder = IssueLocation.newBuilder();
    ScannerReport.TextRange.Builder textRangeBuilder = ScannerReport.TextRange.newBuilder();

    // non-null fields
    builder.setEngineId(issue.engineId());
    builder.setRuleId(issue.ruleId());
    builder.setMsg(primaryMessage);
    builder.addAllMsgFormatting(toProtobufMessageFormattings(issue.primaryLocation().messageFormattings()));
    locationBuilder.setMsg(primaryMessage);
    locationBuilder.addAllMsgFormatting(toProtobufMessageFormattings(issue.primaryLocation().messageFormattings()));
    locationBuilder.setComponentRef(componentRef);
    TextRange primaryTextRange = issue.primaryLocation().textRange();

    //nullable fields
    CleanCodeAttribute cleanCodeAttribute = issue.cleanCodeAttribute();
    if (cleanCodeAttribute != null) {
      builder.setCleanCodeAttribute(cleanCodeAttribute.name());
    }
    org.sonar.api.batch.rule.Severity severity = issue.severity();
    if (severity != null) {
      builder.setSeverity(Severity.valueOf(severity.name()));
    }
    RuleType issueType = issue.type();
    if (issueType != null) {
      builder.setType(IssueType.valueOf(issueType.name()));
    }
    if (primaryTextRange != null) {
      builder.setTextRange(toProtobufTextRange(textRangeBuilder, primaryTextRange));
    }
    Long effort = issue.remediationEffort();
    if (effort != null) {
      builder.setEffort(effort);
    }
    applyFlows(builder::addFlow, locationBuilder, textRangeBuilder, issue.flows());
    builder.addAllImpacts(toProtobufImpacts(issue.impacts()));
    return builder.build();
  }

  private static void applyFlows(Consumer<ScannerReport.Flow> consumer, ScannerReport.IssueLocation.Builder locationBuilder,
    ScannerReport.TextRange.Builder textRangeBuilder, Collection<Flow> flows) {
    ScannerReport.Flow.Builder flowBuilder = ScannerReport.Flow.newBuilder();
    for (Flow f : flows) {
      DefaultIssueFlow flow = (DefaultIssueFlow) f;
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
          locationBuilder.addAllMsgFormatting(toProtobufMessageFormattings(location.messageFormattings()));
        }
        TextRange textRange = location.textRange();
        if (textRange != null) {
          locationBuilder.setTextRange(toProtobufTextRange(textRangeBuilder, textRange));
        }
        flowBuilder.addLocation(locationBuilder.build());
      }
      if (flow.description() != null) {
        flowBuilder.setDescription(flow.description());
      }
      flowBuilder.setType(toProtobufFlowType(flow.type()));
      consumer.accept(flowBuilder.build());
    }
  }

  private static ScannerReport.FlowType toProtobufFlowType(FlowType flowType) {
    switch (flowType) {
      case EXECUTION:
        return ScannerReport.FlowType.EXECUTION;
      case DATA:
        return ScannerReport.FlowType.DATA;
      case UNDEFINED:
        return ScannerReport.FlowType.UNDEFINED;
      default:
        throw new IllegalArgumentException("Unrecognized flow type: " + flowType);
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
