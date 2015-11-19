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
package org.sonar.batch.scan.report;

import com.google.common.collect.Maps;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.batch.issue.tracking.TrackedIssue;

public class ReportSummary {

  private final IssueVariation total = new IssueVariation();

  private final Map<ReportRuleKey, RuleReport> ruleReportByRuleKey = Maps.newLinkedHashMap();
  private final Map<String, IssueVariation> totalByRuleKey = Maps.newLinkedHashMap();
  private final Map<String, IssueVariation> totalBySeverity = Maps.newLinkedHashMap();

  public IssueVariation getTotal() {
    return total;
  }

  public void addIssue(TrackedIssue issue, Rule rule, RulePriority severity) {
    ReportRuleKey reportRuleKey = new ReportRuleKey(rule, severity);
    initMaps(reportRuleKey);
    ruleReportByRuleKey.get(reportRuleKey).getTotal().incrementCountInCurrentAnalysis();
    total.incrementCountInCurrentAnalysis();
    totalByRuleKey.get(rule.key().toString()).incrementCountInCurrentAnalysis();
    totalBySeverity.get(severity.toString()).incrementCountInCurrentAnalysis();
    if (issue.isNew()) {
      total.incrementNewIssuesCount();
      ruleReportByRuleKey.get(reportRuleKey).getTotal().incrementNewIssuesCount();
      totalByRuleKey.get(rule.key().toString()).incrementNewIssuesCount();
      totalBySeverity.get(severity.toString()).incrementNewIssuesCount();
    }
  }

  public Map<String, IssueVariation> getTotalBySeverity() {
    return totalBySeverity;
  }

  public Map<String, IssueVariation> getTotalByRuleKey() {
    return totalByRuleKey;
  }

  public void addResolvedIssue(TrackedIssue issue, Rule rule, RulePriority severity) {
    ReportRuleKey reportRuleKey = new ReportRuleKey(rule, severity);
    initMaps(reportRuleKey);
    total.incrementResolvedIssuesCount();
    ruleReportByRuleKey.get(reportRuleKey).getTotal().incrementResolvedIssuesCount();
    totalByRuleKey.get(rule.key().toString()).incrementResolvedIssuesCount();
    totalBySeverity.get(severity.toString()).incrementResolvedIssuesCount();
  }

  private void initMaps(ReportRuleKey reportRuleKey) {
    if (!ruleReportByRuleKey.containsKey(reportRuleKey)) {
      ruleReportByRuleKey.put(reportRuleKey, new RuleReport(reportRuleKey));
    }
    if (!totalByRuleKey.containsKey(reportRuleKey.getRule().key().toString())) {
      totalByRuleKey.put(reportRuleKey.getRule().key().toString(), new IssueVariation());
    }
    if (!totalBySeverity.containsKey(reportRuleKey.getSeverity().toString())) {
      totalBySeverity.put(reportRuleKey.getSeverity().toString(), new IssueVariation());
    }
  }

  public List<RuleReport> getRuleReports() {
    List<RuleReport> result = new ArrayList<>(ruleReportByRuleKey.values());
    Collections.sort(result, new RuleReportComparator());
    return result;
  }
}
