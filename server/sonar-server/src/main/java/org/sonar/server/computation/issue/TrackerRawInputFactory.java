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
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LazyInput;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.server.computation.batch.BatchReportReader;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.TreeRootHolder;

public class TrackerRawInputFactory {

  private final TreeRootHolder treeRootHolder;
  private final BatchReportReader reportReader;
  private final RuleCache ruleCache;

  public TrackerRawInputFactory(TreeRootHolder treeRootHolder, BatchReportReader reportReader, RuleCache ruleCache) {
    this.treeRootHolder = treeRootHolder;
    this.reportReader = reportReader;
    this.ruleCache = ruleCache;
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
    protected Iterable<String> loadSourceLines() {
      return Lists.newArrayList(reportReader.readFileSource(component.getRef()));
    }

    @Override
    protected List<DefaultIssue> loadIssues() {
      List<BatchReport.Issue> reportIssues = reportReader.readComponentIssues(component.getRef());
      List<DefaultIssue> issues = new ArrayList<>();
      if (!reportIssues.isEmpty()) {
        // optimization - do not load line hashes if there are no issues
        LineHashSequence lineHashSeq = getLineHashSequence();
        for (BatchReport.Issue reportIssue : reportIssues) {
          DefaultIssue issue = toIssue(lineHashSeq, reportIssue);
          if (isValid(issue, lineHashSeq)) {
            issues.add(issue);
          }
        }
      }
      return issues;
    }

    private DefaultIssue toIssue(LineHashSequence lineHashSeq, BatchReport.Issue reportIssue) {
      DefaultIssue issue = new DefaultIssue();
      issue.setRuleKey(RuleKey.of(reportIssue.getRuleRepository(), reportIssue.getRuleKey()));
      issue.setStatus(Issue.STATUS_OPEN);
      issue.setComponentUuid(component.getUuid());
      issue.setComponentKey(component.getKey());
      issue.setProjectUuid(treeRootHolder.getRoot().getUuid());

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
      if (reportIssue.hasDebtInMinutes()) {
        issue.setDebt(Duration.create(reportIssue.getDebtInMinutes()));
      }
      issue.setTags(Sets.newHashSet(reportIssue.getTagList()));
      // TODO issue attributes
      return issue;
    }
  }

  private boolean isValid(DefaultIssue issue, LineHashSequence lineHashSeq) {
    // TODO log debug when invalid ?
    if (ruleCache.getNullable(issue.ruleKey()) == null) {
      return false;
    }
    if (issue.getLine() != null && !lineHashSeq.hasLine(issue.getLine())) {
      return false;
    }
    return true;
  }
}
