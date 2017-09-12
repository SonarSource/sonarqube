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
package org.sonar.server.issue.notification;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;

import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.ASSIGNEE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.COMPONENT;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.RULE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.SEVERITY;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.TAG;

public class NewIssuesStatistics {
  private final Predicate<Issue> onLeakPredicate;
  private final Map<String, Stats> assigneesStatistics = new LinkedHashMap<>();
  private final Stats globalStatistics;

  public NewIssuesStatistics(Predicate<Issue> onLeakPredicate) {
    this.onLeakPredicate = onLeakPredicate;
    this.globalStatistics = new Stats(onLeakPredicate);
  }

  public void add(Issue issue) {
    globalStatistics.add(issue);
    String login = issue.assignee();
    if (login != null) {
      assigneesStatistics.computeIfAbsent(login, a -> new Stats(onLeakPredicate)).add(issue);
    }
  }

  public Map<String, Stats> assigneesStatistics() {
    return assigneesStatistics;
  }

  public Stats globalStatistics() {
    return globalStatistics;
  }

  public boolean hasIssues() {
    return globalStatistics.hasIssues();
  }

  enum Metric {
    SEVERITY(true), TAG(true), COMPONENT(true), ASSIGNEE(true), EFFORT(false), RULE(true);
    private final boolean isComputedByDistribution;

    Metric(boolean isComputedByDistribution) {
      this.isComputedByDistribution = isComputedByDistribution;
    }

    boolean isComputedByDistribution() {
      return this.isComputedByDistribution;
    }
  }

  public static class Stats {
    private final Predicate<Issue> onLeakPredicate;
    private final Map<Metric, DistributedMetricStatsInt> distributions = new EnumMap<>(Metric.class);
    private MetricStatsLong effortStats = new MetricStatsLong();

    public Stats(Predicate<Issue> onLeakPredicate) {
      this.onLeakPredicate = onLeakPredicate;
      for (Metric metric : Metric.values()) {
        if (metric.isComputedByDistribution()) {
          distributions.put(metric, new DistributedMetricStatsInt());
        }
      }
    }

    public void add(Issue issue) {
      boolean isOnLeak = onLeakPredicate.test(issue);
      distributions.get(SEVERITY).increment(issue.severity(), isOnLeak);
      distributions.get(COMPONENT).increment(issue.componentUuid(), isOnLeak);
      RuleKey ruleKey = issue.ruleKey();
      if (ruleKey != null) {
        distributions.get(RULE).increment(ruleKey.toString(), isOnLeak);
      }
      String assignee = issue.assignee();
      if (assignee != null) {
        distributions.get(ASSIGNEE).increment(assignee, isOnLeak);
      }
      for (String tag : issue.tags()) {
        distributions.get(TAG).increment(tag, isOnLeak);
      }
      Duration effort = issue.effort();
      if (effort != null) {
        effortStats.add(effort.toMinutes(), isOnLeak);
      }
    }

    public DistributedMetricStatsInt getDistributedMetricStats(Metric metric) {
      return distributions.get(metric);
    }

    public MetricStatsLong effort() {
      return effortStats;
    }

    public boolean hasIssues() {
      return getDistributedMetricStats(SEVERITY).getTotal() > 0;
    }

  }

}
