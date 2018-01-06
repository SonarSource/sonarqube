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
package org.sonar.server.computation.task.projectanalysis.issue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LazyInput;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.scanner.protocol.Constants.Severity;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.computation.task.projectanalysis.batch.BatchReportReader;
import org.sonar.server.computation.task.projectanalysis.component.Component;
import org.sonar.server.computation.task.projectanalysis.component.TreeRootHolder;
import org.sonar.server.computation.task.projectanalysis.issue.commonrule.CommonRuleEngine;
import org.sonar.server.computation.task.projectanalysis.issue.filter.IssueFilter;
import org.sonar.server.computation.task.projectanalysis.source.SourceLinesRepository;
import org.sonar.server.rule.CommonRuleKeys;

import static com.google.common.collect.Lists.newArrayList;
import static org.apache.commons.lang.StringUtils.isNotEmpty;

public class TrackerRawInputFactory {

  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;
  private final SourceLinesRepository sourceLinesRepository;
  private final CommonRuleEngine commonRuleEngine;
  private final IssueFilter issueFilter;

  public TrackerRawInputFactory(TreeRootHolder treeRootHolder, BatchReportReader reportReader,
    SourceLinesRepository sourceLinesRepository, CommonRuleEngine commonRuleEngine, IssueFilter issueFilter) {
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.sourceLinesRepository = sourceLinesRepository;
    this.commonRuleEngine = commonRuleEngine;
    this.issueFilter = issueFilter;
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
      List<String> lines;
      if (component.getType() == Component.Type.FILE) {
        try (CloseableIterator<String> linesIt = sourceLinesRepository.readLines(component)) {
          lines = newArrayList(linesIt);
        }
      } else {
        lines = Collections.emptyList();
      }
      return LineHashSequence.createForLines(lines);
    }

    @Override
    protected List<DefaultIssue> loadIssues() {
      List<DefaultIssue> result = new ArrayList<>();

      for (DefaultIssue commonRuleIssue : commonRuleEngine.process(component)) {
        if (issueFilter.accept(commonRuleIssue, component)) {
          result.add(init(commonRuleIssue));
        }
      }
      try (CloseableIterator<ScannerReport.Issue> reportIssues = reportReader.readComponentIssues(component.getReportAttributes().getRef())) {
        // optimization - do not load line hashes if there are no issues -> getLineHashSequence() is executed
        // as late as possible
        while (reportIssues.hasNext()) {
          ScannerReport.Issue reportIssue = reportIssues.next();
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

      return result;
    }

    private boolean isIssueOnUnsupportedCommonRule(ScannerReport.Issue issue) {
      // issues on batch common rules are ignored. This feature
      // is natively supported by compute engine since 5.2.
      return !issue.getRuleRepository().startsWith(CommonRuleKeys.REPOSITORY_PREFIX);
    }

    private DefaultIssue toIssue(LineHashSequence lineHashSeq, ScannerReport.Issue reportIssue) {
      DefaultIssue issue = new DefaultIssue();
      init(issue);
      issue.setRuleKey(RuleKey.of(reportIssue.getRuleRepository(), reportIssue.getRuleKey()));
      if (reportIssue.hasTextRange()) {
        int startLine = reportIssue.getTextRange().getStartLine();
        issue.setLine(startLine);
        issue.setChecksum(lineHashSeq.getHashForLine(startLine));
      } else {
        issue.setChecksum("");
      }
      if (isNotEmpty(reportIssue.getMsg())) {
        issue.setMessage(reportIssue.getMsg());
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
            dbFlowBuilder.addLocation(convertLocation(location));
          }
          dbLocationsBuilder.addFlow(dbFlowBuilder);
        }
      }
      issue.setLocations(dbLocationsBuilder.build());
      return issue;
    }

    private DefaultIssue init(DefaultIssue issue) {
      issue.setResolution(null);
      issue.setStatus(Issue.STATUS_OPEN);
      issue.setComponentUuid(component.getUuid());
      issue.setComponentKey(component.getPublicKey());
      issue.setProjectUuid(treeRootHolder.getRoot().getUuid());
      issue.setProjectKey(treeRootHolder.getRoot().getPublicKey());
      return issue;
    }

    private DbIssues.Location convertLocation(ScannerReport.IssueLocation source) {
      DbIssues.Location.Builder target = DbIssues.Location.newBuilder();
      if (source.getComponentRef() != 0 && source.getComponentRef() != component.getReportAttributes().getRef()) {
        target.setComponentId(treeRootHolder.getComponentByRef(source.getComponentRef()).getUuid());
      }
      if (isNotEmpty(source.getMsg())) {
        target.setMsg(source.getMsg());
      }
      if (source.hasTextRange()) {
        ScannerReport.TextRange sourceRange = source.getTextRange();
        DbCommons.TextRange.Builder targetRange = convertTextRange(sourceRange);
        target.setTextRange(targetRange);
      }
      return target.build();
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
