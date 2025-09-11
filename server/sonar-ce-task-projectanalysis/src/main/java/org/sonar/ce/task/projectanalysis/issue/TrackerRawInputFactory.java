/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.rule.RuleType;
import org.sonar.api.server.rule.internal.ImpactMapper;
import org.sonar.api.utils.Duration;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.filter.IssueFilter;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolder;
import org.sonar.ce.task.projectanalysis.source.SourceLinesHashRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LazyInput;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.core.rule.RuleTypeMapper;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.scanner.protocol.Constants;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.Impact;
import org.sonar.scanner.protocol.output.ScannerReport.IssueType;
import org.sonar.server.rule.CommonRuleKeys;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;

public class TrackerRawInputFactory {
  private static final long DEFAULT_EXTERNAL_ISSUE_EFFORT = 0L;
  private final TreeRootHolder treeRootHolder;
  private final ScannerReportReader reportReader;
  private final IssueFilter issueFilter;
  private final SourceLinesHashRepository sourceLinesHash;
  private final RuleRepository ruleRepository;
  private final ActiveRulesHolder activeRulesHolder;

  public TrackerRawInputFactory(TreeRootHolder treeRootHolder, ScannerReportReader reportReader, SourceLinesHashRepository sourceLinesHash,
    IssueFilter issueFilter, RuleRepository ruleRepository, ActiveRulesHolder activeRulesHolder) {
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.sourceLinesHash = sourceLinesHash;
    this.issueFilter = issueFilter;
    this.ruleRepository = ruleRepository;
    this.activeRulesHolder = activeRulesHolder;
  }

  public Input<DefaultIssue> create(Component component) {
    return new RawLazyInput(component);
  }

  private class RawLazyInput extends LazyInput<DefaultIssue> {
    private final Component component;

    private RawLazyInput(Component component) {
      this.component = component;
    }

    @Override
    protected LineHashSequence loadLineHashSequence() {
      if (component.getType() == Component.Type.FILE) {
        return new LineHashSequence(sourceLinesHash.getLineHashesMatchingDBVersion(component));
      } else {
        return new LineHashSequence(Collections.emptyList());
      }
    }

    @Override
    protected List<DefaultIssue> loadIssues() {
      List<DefaultIssue> result = new ArrayList<>();

      if (component.getReportAttributes().getRef() == null) {
        return result;
      }

      try (CloseableIterator<ScannerReport.Issue> reportIssues = reportReader.readComponentIssues(component.getReportAttributes().getRef())) {
        // optimization - do not load line hashes if there are no issues -> getLineHashSequence() is executed
        // as late as possible
        while (reportIssues.hasNext()) {
          ScannerReport.Issue reportIssue = reportIssues.next();
          if (isOnInactiveRule(reportIssue)) {
            continue;
          }
          if (!isIssueOnUnsupportedCommonRule(reportIssue)) {
            LoggerFactory.getLogger(getClass()).debug("Ignored issue from analysis report on rule {}:{}", reportIssue.getRuleRepository(), reportIssue.getRuleKey());
            continue;
          }
          DefaultIssue issue = toIssue(getLineHashSequence(), reportIssue);
          if (issueFilter.accept(issue, component)) {
            result.add(issue);
          }
        }
      }

      Map<RuleKey, ScannerReport.AdHocRule> adHocRuleMap = new HashMap<>();
      try (CloseableIterator<ScannerReport.AdHocRule> reportAdHocRule = reportReader.readAdHocRules()) {
        while (reportAdHocRule.hasNext()) {
          ScannerReport.AdHocRule adHocRule = reportAdHocRule.next();
          adHocRuleMap.put(RuleKey.of(RuleKey.EXTERNAL_RULE_REPO_PREFIX + adHocRule.getEngineId(), adHocRule.getRuleId()), adHocRule);
        }
      }

      try (CloseableIterator<ScannerReport.ExternalIssue> reportExternalIssues = reportReader.readComponentExternalIssues(component.getReportAttributes().getRef())) {
        // optimization - do not load line hashes if there are no issues -> getLineHashSequence() is executed
        // as late as possible
        while (reportExternalIssues.hasNext()) {
          ScannerReport.ExternalIssue reportExternalIssue = reportExternalIssues.next();
          result.add(toExternalIssue(getLineHashSequence(), reportExternalIssue, adHocRuleMap));
        }
      }

      return result;
    }

    private boolean isOnInactiveRule(ScannerReport.Issue reportIssue) {
      RuleKey ruleKey = RuleKey.of(reportIssue.getRuleRepository(), reportIssue.getRuleKey());
      return activeRulesHolder.get(ruleKey).isEmpty();
    }

    private boolean isIssueOnUnsupportedCommonRule(ScannerReport.Issue issue) {
      // issues on batch common rules are ignored. This feature
      // is natively supported by compute engine since 5.2.
      return !issue.getRuleRepository().startsWith(CommonRuleKeys.REPOSITORY_PREFIX);
    }

    private DefaultIssue toIssue(LineHashSequence lineHashSeq, ScannerReport.Issue reportIssue) {
      DefaultIssue issue = new DefaultIssue();
      init(issue, STATUS_OPEN);
      RuleKey ruleKey = RuleKey.of(reportIssue.getRuleRepository(), reportIssue.getRuleKey());
      issue.setRuleKey(ruleKey);
      if (reportIssue.hasTextRange()) {
        int startLine = reportIssue.getTextRange().getStartLine();
        issue.setLine(startLine);
        issue.setChecksum(lineHashSeq.getHashForLine(startLine));
      } else {
        issue.setChecksum("");
      }
      if (isNotEmpty(reportIssue.getMsg())) {
        issue.setMessage(reportIssue.getMsg());
        if (!reportIssue.getMsgFormattingList().isEmpty()) {
          issue.setMessageFormattings(convertMessageFormattings(reportIssue.getMsgFormattingList()));
        }
      } else {
        Rule rule = ruleRepository.getByKey(ruleKey);
        issue.setMessage(rule.getName());
      }
      issue.setSeverity(replaceDefaultWithOverriddenSeverity(issue.ruleKey(), reportIssue));
      if (Double.compare(reportIssue.getGap(), 0D) != 0) {
        issue.setGap(reportIssue.getGap());
      }
      DbIssues.Locations.Builder dbLocationsBuilder = DbIssues.Locations.newBuilder();
      if (reportIssue.hasTextRange()) {
        dbLocationsBuilder.setTextRange(convertTextRange(reportIssue.getTextRange()));
      }
      for (ScannerReport.Flow flow : reportIssue.getFlowList()) {
        if (flow.getLocationCount() > 0) {
          DbIssues.Flow.Builder dbFlowBuilder = convertLocations(flow);
          dbLocationsBuilder.addFlow(dbFlowBuilder);
        }
      }
      issue.setIsFromExternalRuleEngine(false);
      issue.setLocations(dbLocationsBuilder.build());
      issue.setQuickFixAvailable(reportIssue.getQuickFixAvailable());
      issue.setRuleDescriptionContextKey(reportIssue.hasRuleDescriptionContextKey() ? reportIssue.getRuleDescriptionContextKey() : null);
      issue.setCodeVariants(reportIssue.getCodeVariantsList());
      issue.setInternalTags(reportIssue.getInternalTagsList());

      issue.replaceImpacts(replaceDefaultWithOverriddenImpactsForIssue(issue.ruleKey(), reportIssue.getOverriddenImpactsList()));
      return issue;
    }

    private String replaceDefaultWithOverriddenSeverity(RuleKey ruleKey, ScannerReport.Issue reportIssue) {
      if (reportIssue.hasOverriddenSeverity()) {
        return reportIssue.getOverriddenSeverity().name();
      } else {
        // Rule can't be inactive (see contract of IssueVisitor)
        ActiveRule activeRule = activeRulesHolder.get(ruleKey).orElseThrow(() -> new IllegalStateException("Rule " + ruleKey + " is not active"));
        return activeRule.getSeverity();
      }
    }

    private Map<SoftwareQuality, Severity> replaceDefaultWithOverriddenImpactsForIssue(RuleKey ruleKey, List<Impact> overriddenImpactsList) {
      // Rule can't be inactive (see contract of IssueVisitor)
      ActiveRule activeRule = activeRulesHolder.get(ruleKey).orElseThrow(() -> new IllegalStateException("Rule " + ruleKey + " is not active"));
      return replaceDefaultWithOverriddenImpacts(ruleKey, activeRule, overriddenImpactsList);
    }

    private Map<SoftwareQuality, Severity> replaceDefaultWithOverriddenImpactsForExternalIssue(RuleKey ruleKey, List<Impact> overriddenImpactsList) {
      return replaceDefaultWithOverriddenImpacts(ruleKey, null, overriddenImpactsList);
    }

    private Map<SoftwareQuality, Severity> replaceDefaultWithOverriddenImpacts(RuleKey ruleKey, @Nullable ActiveRule activeRule,
      List<Impact> overriddenImpactsList) {
      EnumMap<SoftwareQuality, Severity> impacts = new EnumMap<>(SoftwareQuality.class);
      impacts.putAll(ruleRepository.getByKey(ruleKey).getDefaultImpacts());
      if (activeRule != null) {
        // Override default impacts with the ones possibly defined in the quality profile
        overrideImpacts(impacts, activeRule.getImpacts());
      }
      // Override default impacts with the ones possibly defined at the issue level
      overrideImpacts(impacts, toMap(overriddenImpactsList));
      return impacts;
    }

    private void overrideImpacts(EnumMap<SoftwareQuality, Severity> impacts, Map<SoftwareQuality, Severity> overridenImpactMap) {
      overridenImpactMap.forEach((softwareQuality, severity) -> {
        if (impacts.containsKey(softwareQuality)) {
          impacts.put(softwareQuality, severity);
        }
      });
    }

    private static Map<SoftwareQuality, Severity> toMap(List<Impact> overriddenImpactsList) {
      return overriddenImpactsList.stream()
        .collect(Collectors.toMap(
          impact -> SoftwareQuality.valueOf(impact.getSoftwareQuality().name()),
          impact -> org.sonar.ce.task.projectanalysis.issue.ImpactMapper.mapImpactSeverity(impact.getSeverity())));
    }

    private DbIssues.Flow.Builder convertLocations(ScannerReport.Flow flow) {
      DbIssues.Flow.Builder dbFlowBuilder = DbIssues.Flow.newBuilder();
      for (ScannerReport.IssueLocation location : flow.getLocationList()) {
        convertLocation(location).ifPresent(dbFlowBuilder::addLocation);
      }
      if (isNotEmpty(flow.getDescription())) {
        dbFlowBuilder.setDescription(flow.getDescription());
      }
      toFlowType(flow.getType()).ifPresent(dbFlowBuilder::setType);
      return dbFlowBuilder;
    }

    private DefaultIssue toExternalIssue(LineHashSequence lineHashSeq, ScannerReport.ExternalIssue reportExternalIssue, Map<RuleKey, ScannerReport.AdHocRule> adHocRuleMap) {
      DefaultIssue issue = new DefaultIssue();
      RuleKey ruleKey = RuleKey.of(RuleKey.EXTERNAL_RULE_REPO_PREFIX + reportExternalIssue.getEngineId(), reportExternalIssue.getRuleId());
      issue.setRuleKey(ruleKey);
      ruleRepository.addOrUpdateAddHocRuleIfNeeded(ruleKey, () -> toAdHocRule(reportExternalIssue, adHocRuleMap.get(issue.ruleKey())));

      Rule existingRule = ruleRepository.getByKey(ruleKey);
      issue.setSeverity(determineDeprecatedSeverity(reportExternalIssue, existingRule));
      issue.setType(determineDeprecatedType(reportExternalIssue, existingRule));
      issue.replaceImpacts(replaceDefaultWithOverriddenImpactsForExternalIssue(issue.ruleKey(), reportExternalIssue.getImpactsList()));

      init(issue, issue.type() == RuleType.SECURITY_HOTSPOT ? STATUS_TO_REVIEW : STATUS_OPEN);

      if (reportExternalIssue.hasTextRange()) {
        int startLine = reportExternalIssue.getTextRange().getStartLine();
        issue.setLine(startLine);
        issue.setChecksum(lineHashSeq.getHashForLine(startLine));
      } else {
        issue.setChecksum("");
      }
      if (isNotEmpty(reportExternalIssue.getMsg())) {
        issue.setMessage(reportExternalIssue.getMsg());
        if (!reportExternalIssue.getMsgFormattingList().isEmpty()) {
          issue.setMessageFormattings(convertMessageFormattings(reportExternalIssue.getMsgFormattingList()));
        }
      }
      issue.setEffort(Duration.create(reportExternalIssue.getEffort() != 0 ? reportExternalIssue.getEffort() : DEFAULT_EXTERNAL_ISSUE_EFFORT));
      DbIssues.Locations.Builder dbLocationsBuilder = DbIssues.Locations.newBuilder();
      if (reportExternalIssue.hasTextRange()) {
        dbLocationsBuilder.setTextRange(convertTextRange(reportExternalIssue.getTextRange()));
      }
      for (ScannerReport.Flow flow : reportExternalIssue.getFlowList()) {
        if (flow.getLocationCount() > 0) {
          DbIssues.Flow.Builder dbFlowBuilder = convertLocations(flow);
          dbLocationsBuilder.addFlow(dbFlowBuilder);
        }
      }
      issue.setIsFromExternalRuleEngine(true);
      issue.setLocations(dbLocationsBuilder.build());

      return issue;
    }

    private NewAdHocRule toAdHocRule(ScannerReport.ExternalIssue reportIssue, @Nullable ScannerReport.AdHocRule adHocRule) {
      if (adHocRule != null) {
        return new NewAdHocRule(adHocRule);
      }
      return new NewAdHocRule(reportIssue);
    }

    private Optional<DbIssues.FlowType> toFlowType(ScannerReport.FlowType flowType) {
      return switch (flowType) {
        case DATA -> Optional.of(DbIssues.FlowType.DATA);
        case EXECUTION -> Optional.of(DbIssues.FlowType.EXECUTION);
        case UNDEFINED -> Optional.empty();
        default -> throw new IllegalArgumentException("Unrecognized type: " + flowType);
      };
    }

    private RuleType toRuleType(IssueType type) {
      return switch (type) {
        case BUG -> RuleType.BUG;
        case CODE_SMELL -> RuleType.CODE_SMELL;
        case VULNERABILITY -> RuleType.VULNERABILITY;
        case SECURITY_HOTSPOT -> RuleType.SECURITY_HOTSPOT;
        default -> throw new IllegalStateException("Invalid issue type: " + type);
      };
    }

    private DefaultIssue init(DefaultIssue issue, String initialStatus) {
      issue.setStatus(initialStatus);
      issue.setResolution(null);
      issue.setComponentUuid(component.getUuid());
      issue.setComponentKey(component.getKey());
      issue.setProjectUuid(treeRootHolder.getRoot().getUuid());
      issue.setProjectKey(treeRootHolder.getRoot().getKey());
      return issue;
    }

    private Optional<DbIssues.Location> convertLocation(ScannerReport.IssueLocation source) {
      DbIssues.Location.Builder target = DbIssues.Location.newBuilder();
      if (source.getComponentRef() != 0 && source.getComponentRef() != component.getReportAttributes().getRef()) {
        // Component might not exist because on PR, only changed components are included in the component tree
        // See in BuildComponentTreeStep the call to buildChangedComponentTreeRoot
        Optional<Component> optionalComponent = treeRootHolder.getOptionalComponentByRef(source.getComponentRef());
        if (optionalComponent.isEmpty()) {
          return Optional.empty();
        }
        target.setComponentId(optionalComponent.get().getUuid());
      }
      if (isNotEmpty(source.getMsg())) {
        target.setMsg(source.getMsg());
        source.getMsgFormattingList()
          .forEach(m -> target.addMsgFormatting(convertMessageFormatting(m)));
      }
      if (source.hasTextRange()) {
        ScannerReport.TextRange sourceRange = source.getTextRange();
        DbCommons.TextRange.Builder targetRange = convertTextRange(sourceRange);
        target.setTextRange(targetRange);
      }
      return Optional.of(target.build());
    }

    private DbCommons.TextRange.Builder convertTextRange(ScannerReport.TextRange sourceRange) {
      DbCommons.TextRange.Builder targetRange = DbCommons.TextRange.newBuilder();
      targetRange.setStartLine(sourceRange.getStartLine());
      targetRange.setStartOffset(sourceRange.getStartOffset());
      targetRange.setEndLine(sourceRange.getEndLine());
      targetRange.setEndOffset(sourceRange.getEndOffset());
      return targetRange;
    }

    private RuleType determineDeprecatedType(ScannerReport.ExternalIssue reportExternalIssue, Rule rule) {
      if (reportExternalIssue.getType() != ScannerReport.IssueType.UNSET) {
        return toRuleType(reportExternalIssue.getType());
      } else if (rule.getType() != null) {
        return rule.getType();
      } else if (!rule.getDefaultImpacts().isEmpty()) {
        SoftwareQuality impactSoftwareQuality = ImpactMapper.getBestImpactForBackmapping(rule.getDefaultImpacts()).getKey();
        return RuleTypeMapper.toRuleType(ImpactMapper.convertToRuleType(impactSoftwareQuality));
      } else {
        throw new IllegalArgumentException("Cannot determine the type for issue of rule %s".formatted(reportExternalIssue.getRuleId()));
      }
    }

    private static String determineDeprecatedSeverity(ScannerReport.ExternalIssue reportExternalIssue, Rule rule) {
      if (reportExternalIssue.getSeverity() != Constants.Severity.UNSET_SEVERITY) {
        return reportExternalIssue.getSeverity().name();
      } else if (rule.getSeverity() != null) {
        return rule.getSeverity();
      } else if (!rule.getDefaultImpacts().isEmpty()) {
        Severity impactSeverity = ImpactMapper.getBestImpactForBackmapping(rule.getDefaultImpacts()).getValue();
        return ImpactMapper.convertToDeprecatedSeverity(impactSeverity);
      } else {
        throw new IllegalArgumentException("Cannot determine the severity for issue of rule %s".formatted(reportExternalIssue.getRuleId()));
      }
    }

  }

  private static DbIssues.MessageFormattings convertMessageFormattings(List<ScannerReport.MessageFormatting> msgFormattings) {
    DbIssues.MessageFormattings.Builder builder = DbIssues.MessageFormattings.newBuilder();
    msgFormattings.stream()
      .forEach(m -> builder.addMessageFormatting(TrackerRawInputFactory.convertMessageFormatting(m)));
    return builder.build();
  }

  @NotNull
  private static DbIssues.MessageFormatting convertMessageFormatting(ScannerReport.MessageFormatting m) {
    DbIssues.MessageFormatting.Builder msgFormattingBuilder = DbIssues.MessageFormatting.newBuilder();
    return msgFormattingBuilder
      .setStart(m.getStart())
      .setEnd(m.getEnd())
      .setType(DbIssues.MessageFormattingType.valueOf(m.getType().name())).build();
  }

}
