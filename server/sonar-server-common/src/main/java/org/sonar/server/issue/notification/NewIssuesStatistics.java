/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.issue.notification;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.issue.DefaultIssue;

import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.ASSIGNEE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.COMPONENT;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.RULE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.TAG;

public class NewIssuesStatistics {
  private final Predicate<DefaultIssue> onCurrentAnalysisPredicate;
  private final Map<String, Stats> assigneesStatistics = new LinkedHashMap<>();
  private final Stats globalStatistics;

  public NewIssuesStatistics(Predicate<DefaultIssue> onCurrentAnalysisPredicate) {
    this.onCurrentAnalysisPredicate = onCurrentAnalysisPredicate;
    this.globalStatistics = new Stats(onCurrentAnalysisPredicate);
  }

  public void add(DefaultIssue issue) {
    globalStatistics.add(issue);
    String userUuid = issue.assignee();
    if (userUuid != null) {
      assigneesStatistics.computeIfAbsent(userUuid, a -> new Stats(onCurrentAnalysisPredicate)).add(issue);
    }
  }

  public Map<String, Stats> getAssigneesStatistics() {
    return assigneesStatistics;
  }

  public Stats globalStatistics() {
    return globalStatistics;
  }

  public boolean hasIssues() {
    return globalStatistics.hasIssues();
  }

  public boolean hasIssuesOnCurrentAnalysis() {
    return globalStatistics.hasIssuesOnCurrentAnalysis();
  }

  public enum Metric {
    ISSUE(false), TAG(true), COMPONENT(true), ASSIGNEE(true), RULE(true);
    private final boolean isComputedByDistribution;

    Metric(boolean isComputedByDistribution) {
      this.isComputedByDistribution = isComputedByDistribution;
    }

    boolean isComputedByDistribution() {
      return this.isComputedByDistribution;
    }
  }

  @Override
  public String toString() {
    return "NewIssuesStatistics{" +
      "assigneesStatistics=" + assigneesStatistics +
      ", globalStatistics=" + globalStatistics +
      '}';
  }

  public static class Stats {
    private final Predicate<DefaultIssue> onCurrentAnalysisPredicate;
    private final Map<Metric, DistributedMetricStatsInt> distributions = new EnumMap<>(Metric.class);
    private final MetricStatsInt issueCount = new MetricStatsInt();

    public Stats(Predicate<DefaultIssue> onCurrentAnalysisPredicate) {
      this.onCurrentAnalysisPredicate = onCurrentAnalysisPredicate;
      for (Metric metric : Metric.values()) {
        if (metric.isComputedByDistribution()) {
          distributions.put(metric, new DistributedMetricStatsInt());
        }
      }
    }

    public void add(DefaultIssue issue) {
      boolean onCurrentAnalysis = onCurrentAnalysisPredicate.test(issue);
      issueCount.increment(onCurrentAnalysis);
      String componentUuid = issue.componentUuid();
      if (componentUuid != null) {
        distributions.get(COMPONENT).increment(componentUuid, onCurrentAnalysis);
      }
      RuleKey ruleKey = issue.ruleKey();
      if (ruleKey != null) {
        distributions.get(RULE).increment(ruleKey.toString(), onCurrentAnalysis);
      }
      String assigneeUuid = issue.assignee();
      if (assigneeUuid != null) {
        distributions.get(ASSIGNEE).increment(assigneeUuid, onCurrentAnalysis);
      }
      for (String tag : issue.tags()) {
        distributions.get(TAG).increment(tag, onCurrentAnalysis);
      }
    }

    public DistributedMetricStatsInt getDistributedMetricStats(Metric metric) {
      return distributions.get(metric);
    }

    public MetricStatsInt getIssueCount() {
      return issueCount;
    }

    public boolean hasIssues() {
      return getIssueCount().getTotal() > 0;
    }

    public boolean hasIssuesOnCurrentAnalysis() {
      return getIssueCount().getOnCurrentAnalysis() > 0;
    }

    @Override
    public String toString() {
      return "Stats{" +
        "distributions=" + distributions +
        ", issueCount=" + issueCount +
        '}';
    }
  }

}
