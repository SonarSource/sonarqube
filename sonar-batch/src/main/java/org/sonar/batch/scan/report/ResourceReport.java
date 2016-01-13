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
package org.sonar.batch.scan.report;

import org.sonar.batch.issue.tracking.TrackedIssue;

import org.sonar.api.batch.rule.Rule;
import com.google.common.collect.Maps;
import org.sonar.api.rules.RulePriority;
import org.sonar.batch.index.BatchComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class ResourceReport {
  private final BatchComponent resource;
  private final IssueVariation total = new IssueVariation();
  private final Map<ReportRuleKey, RuleReport> ruleReportByRuleKey = Maps.newHashMap();

  private List<TrackedIssue> issues = new ArrayList<>();
  private Map<Integer, List<TrackedIssue>> issuesPerLine = Maps.newHashMap();
  private Map<Integer, List<TrackedIssue>> newIssuesPerLine = Maps.newHashMap();
  private Map<Rule, AtomicInteger> issuesByRule = Maps.newHashMap();
  private Map<RulePriority, AtomicInteger> issuesBySeverity = Maps.newHashMap();

  public ResourceReport(BatchComponent resource) {
    this.resource = resource;
  }

  public BatchComponent getResourceNode() {
    return resource;
  }

  public String getName() {
    return resource.resource().getName();
  }

  public String getType() {
    return resource.resource().getScope();
  }

  public IssueVariation getTotal() {
    return total;
  }

  public List<TrackedIssue> getIssues() {
    return issues;
  }

  public Map<Integer, List<TrackedIssue>> getIssuesPerLine() {
    return issuesPerLine;
  }

  public List<TrackedIssue> getIssuesAtLine(int lineId, boolean all) {
    if (all) {
      if (issuesPerLine.containsKey(lineId)) {
        return issuesPerLine.get(lineId);
      }
    } else if (newIssuesPerLine.containsKey(lineId)) {
      return newIssuesPerLine.get(lineId);
    }
    return Collections.emptyList();
  }

  public void addIssue(TrackedIssue issue, Rule rule, RulePriority severity) {
    ReportRuleKey reportRuleKey = new ReportRuleKey(rule, severity);
    initMaps(reportRuleKey);
    issues.add(issue);
    Integer line = issue.startLine();
    line = line != null ? line : 0;
    if (!issuesPerLine.containsKey(line)) {
      issuesPerLine.put(line, new ArrayList<TrackedIssue>());
    }
    issuesPerLine.get(line).add(issue);
    if (!issuesByRule.containsKey(rule)) {
      issuesByRule.put(rule, new AtomicInteger());
    }
    issuesByRule.get(rule).incrementAndGet();
    if (!issuesBySeverity.containsKey(severity)) {
      issuesBySeverity.put(severity, new AtomicInteger());
    }
    issuesBySeverity.get(severity).incrementAndGet();
    ruleReportByRuleKey.get(reportRuleKey).getTotal().incrementCountInCurrentAnalysis();
    total.incrementCountInCurrentAnalysis();
    if (issue.isNew()) {
      if (!newIssuesPerLine.containsKey(line)) {
        newIssuesPerLine.put(line, new ArrayList<TrackedIssue>());
      }
      newIssuesPerLine.get(line).add(issue);
      total.incrementNewIssuesCount();
      ruleReportByRuleKey.get(reportRuleKey).getTotal().incrementNewIssuesCount();
    }
  }

  public void addResolvedIssue(Rule rule, RulePriority severity) {
    ReportRuleKey reportRuleKey = new ReportRuleKey(rule, severity);
    initMaps(reportRuleKey);
    total.incrementResolvedIssuesCount();
    ruleReportByRuleKey.get(reportRuleKey).getTotal().incrementResolvedIssuesCount();
  }

  private void initMaps(ReportRuleKey reportRuleKey) {
    if (!ruleReportByRuleKey.containsKey(reportRuleKey)) {
      ruleReportByRuleKey.put(reportRuleKey, new RuleReport(reportRuleKey));
    }
  }

  public boolean isDisplayableLine(Integer lineNumber, boolean all) {
    if (lineNumber == null || lineNumber < 1) {
      return false;
    }
    for (int i = lineNumber - 2; i <= lineNumber + 2; i++) {
      if (hasIssues(i, all)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasIssues(Integer lineId, boolean all) {
    if (all) {
      List<TrackedIssue> issuesAtLine = issuesPerLine.get(lineId);
      return issuesAtLine != null && !issuesAtLine.isEmpty();
    }
    List<TrackedIssue> newIssuesAtLine = newIssuesPerLine.get(lineId);
    return newIssuesAtLine != null && !newIssuesAtLine.isEmpty();
  }

  public List<RuleReport> getRuleReports() {
    List<RuleReport> result = new ArrayList<>(ruleReportByRuleKey.values());
    Collections.sort(result, new RuleReportComparator());
    return result;
  }

}
