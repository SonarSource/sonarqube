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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.api.utils.log.Loggers;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolder;
import org.sonar.ce.task.projectanalysis.issue.commonrule.CommonRuleEngine;
import org.sonar.ce.task.projectanalysis.issue.filter.IssueFilter;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolder;
import org.sonar.ce.task.projectanalysis.source.SourceLinesHashRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LazyInput;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.IssueType;
import org.sonar.server.rule.CommonRuleKeys;

import static org.apache.commons.lang.StringUtils.isNotEmpty;

public class TrackerRawInputFactory {
  private static final long DEFAULT_EXTERNAL_ISSUE_EFFORT = 0l;
  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;
  private final CommonRuleEngine commonRuleEngine;
  private final IssueFilter issueFilter;
  private final SourceLinesHashRepository sourceLinesHash;
  private final RuleRepository ruleRepository;
  private final ActiveRulesHolder activeRulesHolder;

  public TrackerRawInputFactory(TreeRootHolder treeRootHolder, BatchReportReader reportReader,
    SourceLinesHashRepository sourceLinesHash, CommonRuleEngine commonRuleEngine, IssueFilter issueFilter, RuleRepository ruleRepository,
    ActiveRulesHolder activeRulesHolder) {
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.sourceLinesHash = sourceLinesHash;
    this.commonRuleEngine = commonRuleEngine;
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

      for (DefaultIssue commonRuleIssue : commonRuleEngine.process(component)) {
        if (issueFilter.accept(commonRuleIssue, component)) {
          result.add(init(commonRuleIssue));
        }
      }

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
            Loggers.get(getClass()).debug("Ignored issue from analysis report on rule {}:{}", reportIssue.getRuleRepository(), reportIssue.getRuleKey());
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
      return !activeRulesHolder.get(ruleKey).isPresent();
    }

    private boolean isIssueOnUnsupportedCommonRule(ScannerReport.Issue issue) {
      // issues on batch common rules are ignored. This feature
      // is natively supported by compute engine since 5.2.
      return !issue.getRuleRepository().startsWith(CommonRuleKeys.REPOSITORY_PREFIX);
    }

    private DefaultIssue toIssue(LineHashSequence lineHashSeq, ScannerReport.Issue reportIssue) {
      DefaultIssue issue = new DefaultIssue();
      init(issue);
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
      } else {
        Rule rule = ruleRepository.getByKey(ruleKey);
        issue.setMessage(rule.getName());
      }
      if (reportIssue.getSeverity() != Severity.UNSET_SEVERITY) {
        issue.setSeverity(reportIssue.getSeverity().name());
      }
      if (reportIssue.getGap() != 0) {
        issue.setGap(reportIssue.getGap());
      }
      DbIssues.Locations.Builder dbLocationsBuilder = DbIssues.Locations.newBuilder();
      if (reportIssue.hasTextRange()) {
        dbLocationsBuilder.setTextRange(convertTextRange(reportIssue.getTextRange()));
      }
      for (ScannerReport.Flow flow : reportIssue.getFlowList()) {
        if (flow.getLocationCount() > 0) {
          DbIssues.Flow.Builder dbFlowBuilder = DbIssues.Flow.newBuilder();
          for (ScannerReport.IssueLocation location : flow.getLocationList()) {
            convertLocation(location).ifPresent(dbFlowBuilder::addLocation);
          }
          dbLocationsBuilder.addFlow(dbFlowBuilder);
        }
      }
      issue.setIsFromExternalRuleEngine(false);
      issue.setLocations(dbLocationsBuilder.build());
      return issue;
    }

    private DefaultIssue toExternalIssue(LineHashSequence lineHashSeq, ScannerReport.ExternalIssue reportExternalIssue, Map<RuleKey, ScannerReport.AdHocRule> adHocRuleMap) {
      DefaultIssue issue = new DefaultIssue();
      init(issue);

      RuleKey ruleKey = RuleKey.of(RuleKey.EXTERNAL_RULE_REPO_PREFIX + reportExternalIssue.getEngineId(), reportExternalIssue.getRuleId());
      issue.setRuleKey(ruleKey);
      if (reportExternalIssue.hasTextRange()) {
        int startLine = reportExternalIssue.getTextRange().getStartLine();
        issue.setLine(startLine);
        issue.setChecksum(lineHashSeq.getHashForLine(startLine));
      } else {
        issue.setChecksum("");
      }
      if (isNotEmpty(reportExternalIssue.getMsg())) {
        issue.setMessage(reportExternalIssue.getMsg());
      }
      if (reportExternalIssue.getSeverity() != Severity.UNSET_SEVERITY) {
        issue.setSeverity(reportExternalIssue.getSeverity().name());
      }
      issue.setEffort(Duration.create(reportExternalIssue.getEffort() != 0 ? reportExternalIssue.getEffort() : DEFAULT_EXTERNAL_ISSUE_EFFORT));
      DbIssues.Locations.Builder dbLocationsBuilder = DbIssues.Locations.newBuilder();
      if (reportExternalIssue.hasTextRange()) {
        dbLocationsBuilder.setTextRange(convertTextRange(reportExternalIssue.getTextRange()));
      }
      for (ScannerReport.Flow flow : reportExternalIssue.getFlowList()) {
        if (flow.getLocationCount() > 0) {
          DbIssues.Flow.Builder dbFlowBuilder = DbIssues.Flow.newBuilder();
          for (ScannerReport.IssueLocation location : flow.getLocationList()) {
            convertLocation(location).ifPresent(dbFlowBuilder::addLocation);
          }
          dbLocationsBuilder.addFlow(dbFlowBuilder);
        }
      }
      issue.setIsFromExternalRuleEngine(true);
      issue.setLocations(dbLocationsBuilder.build());
      issue.setType(toRuleType(reportExternalIssue.getType()));

      ruleRepository.addOrUpdateAddHocRuleIfNeeded(ruleKey, () -> toAdHocRule(reportExternalIssue, adHocRuleMap.get(issue.ruleKey())));
      return issue;
    }

    private NewAdHocRule toAdHocRule(ScannerReport.ExternalIssue reportIssue, @Nullable ScannerReport.AdHocRule adHocRule) {
      if (adHocRule != null) {
        return new NewAdHocRule(adHocRule);
      }
      return new NewAdHocRule(reportIssue);
    }

    private RuleType toRuleType(IssueType type) {
      switch (type) {
        case BUG:
          return RuleType.BUG;
        case CODE_SMELL:
          return RuleType.CODE_SMELL;
        case VULNERABILITY:
          return RuleType.VULNERABILITY;
        case SECURITY_HOTSPOT:
          return RuleType.SECURITY_HOTSPOT;
        case UNRECOGNIZED:
        default:
          throw new IllegalStateException("Invalid issue type: " + type);
      }
    }

    private DefaultIssue init(DefaultIssue issue) {
      issue.setResolution(null);
      issue.setStatus(Issue.STATUS_OPEN);
      issue.setComponentUuid(component.getUuid());
      issue.setComponentKey(component.getKey());
      issue.setProjectUuid(treeRootHolder.getRoot().getUuid());
      issue.setProjectKey(treeRootHolder.getRoot().getKey());
      return issue;
    }

    private Optional<DbIssues.Location> convertLocation(ScannerReport.IssueLocation source) {
      DbIssues.Location.Builder target = DbIssues.Location.newBuilder();
      if (source.getComponentRef() != 0 && source.getComponentRef() != component.getReportAttributes().getRef()) {
        // SONAR-10781 Component might not exist because on short living branch and PR, only changed components are included in the report
        Optional<Component> optionalComponent = treeRootHolder.getOptionalComponentByRef(source.getComponentRef());
        if (!optionalComponent.isPresent()) {
          return Optional.empty();
        }
        target.setComponentId(optionalComponent.get().getUuid());
      }
      if (isNotEmpty(source.getMsg())) {
        target.setMsg(source.getMsg());
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
  }
}
