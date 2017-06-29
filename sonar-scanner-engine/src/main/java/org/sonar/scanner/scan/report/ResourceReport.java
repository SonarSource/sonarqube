/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.scanner.scan.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputDir;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.InputPath;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.scanner.issue.tracking.TrackedIssue;

public final class ResourceReport {
  private final InputComponent component;
  private final IssueVariation total = new IssueVariation();
  private final Map<ReportRuleKey, RuleReport> ruleReportByRuleKey = new HashMap<>();

  private List<TrackedIssue> issues = new ArrayList<>();
  private Map<Integer, List<TrackedIssue>> issuesPerLine = new HashMap<>();
  private Map<Integer, List<TrackedIssue>> newIssuesPerLine = new HashMap<>();
  private Map<Rule, AtomicInteger> issuesByRule = new HashMap<>();
  private Map<RulePriority, AtomicInteger> issuesBySeverity = new EnumMap<>(RulePriority.class);

  public ResourceReport(InputComponent component) {
    this.component = component;
  }

  public InputComponent getResourceNode() {
    return component;
  }

  public String getName() {
    if (component instanceof InputPath) {
      InputPath inputPath = (InputPath) component;
      return inputPath.path().getFileName().toString();
    } else if (component instanceof InputModule) {
      DefaultInputModule module = (DefaultInputModule) component;
      return module.definition().getName();
    }
    throw new IllegalStateException("Unknown component type: " + component.getClass());
  }

  public String getKey() {
    return component.key();
  }

  /**
   * Must match one of the png in the resources, under org/scanner/scan/report/issuesreport_files
   */
  public String getType() {
    if (component instanceof InputFile) {
      return "FIL";
    } else if (component instanceof InputDir) {
      return "DIR";
    } else if (component instanceof InputModule) {
      return "PRJ";
    }
    throw new IllegalStateException("Unknown component type: " + component.getClass());
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

  public boolean isDisplayableLine(@Nullable Integer lineNumber, boolean all) {
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
