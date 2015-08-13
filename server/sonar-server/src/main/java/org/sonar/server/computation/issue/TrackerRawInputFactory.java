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
package org.sonar.server.computation.issue;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.api.utils.log.Loggers;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LazyInput;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.protobuf.DbCommons;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.ReportTreeRootHolder;
import org.sonar.server.computation.issue.commonrule.CommonRuleEngine;
import org.sonar.server.rule.CommonRuleKeys;

public class TrackerRawInputFactory {

  private final ReportTreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;
  private final CommonRuleEngine commonRuleEngine;

  public TrackerRawInputFactory(ReportTreeRootHolder treeRootHolder, BatchReportReader reportReader,
    CommonRuleEngine commonRuleEngine) {
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.commonRuleEngine = commonRuleEngine;
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
      Iterable<String> lines;
      if (component.getType() == Component.Type.FILE) {
        lines = Lists.newArrayList(reportReader.readFileSource(component.getReportAttributes().getRef()));
      } else {
        lines = Collections.emptyList();
      }
      return LineHashSequence.createForLines(lines);
    }

    @Override
    protected List<DefaultIssue> loadIssues() {
      List<DefaultIssue> result = new ArrayList<>();

      for (DefaultIssue commonRuleIssue : commonRuleEngine.process(component)) {
        result.add(init(commonRuleIssue));
      }
      try (CloseableIterator<BatchReport.Issue> reportIssues = reportReader.readComponentIssues(component.getReportAttributes().getRef())) {
        // optimization - do not load line hashes if there are no issues -> getLineHashSequence() is executed
        // as late as possible
        while (reportIssues.hasNext()) {
          BatchReport.Issue reportIssue = reportIssues.next();
          if (isIssueOnUnsupportedCommonRule(reportIssue)) {
            DefaultIssue issue = toIssue(getLineHashSequence(), reportIssue);
            result.add(issue);
          } else {
            Loggers.get(getClass()).debug("Ignored issue from analysis report on rule {}:{}", reportIssue.getRuleRepository(), reportIssue.getRuleKey());
          }
        }
      }

      return result;
    }

    private boolean isIssueOnUnsupportedCommonRule(BatchReport.Issue issue) {
      // issues on batch common rules are ignored. This feature
      // is natively supported by compute engine since 5.2.
      return !issue.getRuleRepository().startsWith(CommonRuleKeys.REPOSITORY_PREFIX);
    }

    private DefaultIssue toIssue(LineHashSequence lineHashSeq, BatchReport.Issue reportIssue) {
      DefaultIssue issue = new DefaultIssue();
      init(issue);
      issue.setRuleKey(RuleKey.of(reportIssue.getRuleRepository(), reportIssue.getRuleKey()));
      if (reportIssue.hasLine()) {
        issue.setLine(reportIssue.getLine());
        issue.setChecksum(lineHashSeq.getHashForLine(reportIssue.getLine()));
      } else {
        issue.setChecksum("");
      }
      if (reportIssue.hasMsg()) {
        issue.setMessage(reportIssue.getMsg());
      }
      if (reportIssue.hasSeverity()) {
        issue.setSeverity(reportIssue.getSeverity().name());
      }
      if (reportIssue.hasEffortToFix()) {
        issue.setEffortToFix(reportIssue.getEffortToFix());
      }
      if (reportIssue.hasAttributes()) {
        issue.setAttributes(KeyValueFormat.parse(reportIssue.getAttributes()));
      }
      DbIssues.Locations.Builder dbLocationsBuilder = DbIssues.Locations.newBuilder();
      if (reportIssue.hasPrimaryLocation()) {
        BatchReport.IssueLocation location = reportIssue.getPrimaryLocation();
        if (location.hasTextRange()) {
          dbLocationsBuilder.setPrimary(convertTextRange(location.getTextRange()));
        }
      }
      for (BatchReport.IssueLocation location : reportIssue.getAdditionalLocationList()) {
        dbLocationsBuilder.addSecondary(convertLocation(location));
      }
      for (BatchReport.ExecutionFlow flow : reportIssue.getExecutionFlowList()) {
        DbIssues.ExecutionFlow.Builder dbFlowBuilder = DbIssues.ExecutionFlow.newBuilder();
        for (BatchReport.IssueLocation location : flow.getLocationList()) {
          dbFlowBuilder.addLocation(convertLocation(location));
        }
        dbLocationsBuilder.addExecutionFlow(dbFlowBuilder);
      }
      issue.setLocations(dbLocationsBuilder.build());
      return issue;
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

    private DbIssues.Location convertLocation(BatchReport.IssueLocation source) {
      DbIssues.Location.Builder target = DbIssues.Location.newBuilder();
      if (source.hasComponentRef() && source.getComponentRef() != component.getReportAttributes().getRef()) {
        target.setComponentId(treeRootHolder.getComponentByRef(source.getComponentRef()).getUuid());
      }
      if (source.hasMsg()) {
        target.setMsg(source.getMsg());
      }
      if (source.hasTextRange()) {
        BatchReport.TextRange sourceRange = source.getTextRange();
        DbCommons.TextRange.Builder targetRange = convertTextRange(sourceRange);
        target.setTextRange(targetRange);
      }
      return target.build();
    }
  }

  private DbCommons.TextRange.Builder convertTextRange(BatchReport.TextRange sourceRange) {
    DbCommons.TextRange.Builder targetRange = DbCommons.TextRange.newBuilder();
    if (sourceRange.hasStartLine()) {
      targetRange.setStartLine(sourceRange.getStartLine());
    }
    if (sourceRange.hasStartOffset()) {
      targetRange.setStartOffset(sourceRange.getStartOffset());
    }
    if (sourceRange.hasEndLine()) {
      targetRange.setEndLine(sourceRange.getEndLine());
    }
    if (sourceRange.hasEndOffset()) {
      targetRange.setEndOffset(sourceRange.getEndOffset());
    }
    return targetRange;
  }
}
