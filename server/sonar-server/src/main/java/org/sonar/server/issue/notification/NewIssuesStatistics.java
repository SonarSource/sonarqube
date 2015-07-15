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

package org.sonar.server.issue.notification;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.issue.Issue;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.Duration;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.ASSIGNEE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.COMPONENT;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.RULE;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.SEVERITY;
import static org.sonar.server.issue.notification.NewIssuesStatistics.Metric.TAG;

public class NewIssuesStatistics {
  private Map<String, Stats> assigneesStatistics = new LinkedHashMap<>();
  private Stats globalStatistics = new Stats();

  public void add(Issue issue) {
    globalStatistics.add(issue);
    String login = issue.assignee();
    if (login != null) {
      getOrCreate(login).add(issue);
    }
  }

  private Stats getOrCreate(String assignee) {
    if (assigneesStatistics.get(assignee) == null) {
      assigneesStatistics.put(assignee, new Stats());
    }
    return assigneesStatistics.get(assignee);
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
    SEVERITY(true), TAG(true), COMPONENT(true), ASSIGNEE(true), DEBT(false), RULE(true);
    private final boolean isComputedByDistribution;

    Metric(boolean isComputedByDistribution) {
      this.isComputedByDistribution = isComputedByDistribution;
    }

    boolean isComputedByDistribution() {
      return this.isComputedByDistribution;
    }
  }

  public static class Stats {
    private final Map<Metric, Multiset<String>> distributions = new EnumMap<>(Metric.class);
    private long debtInMinutes = 0L;

    public Stats() {
      for (Metric metric : Metric.values()) {
        if (metric.isComputedByDistribution()) {
          distributions.put(metric, HashMultiset.<String>create());
        }
      }
    }

    public void add(Issue issue) {
      distributions.get(SEVERITY).add(issue.severity());
      distributions.get(COMPONENT).add(issue.componentUuid());
      RuleKey ruleKey = issue.ruleKey();
      if (ruleKey != null) {
        distributions.get(RULE).add(ruleKey.toString());
      }
      if (issue.assignee() != null) {
        distributions.get(ASSIGNEE).add(issue.assignee());
      }
      for (String tag : issue.tags()) {
        distributions.get(TAG).add(tag);
      }
      Duration debt = issue.debt();
      if (debt != null) {
        debtInMinutes += debt.toMinutes();
      }
    }

    public int countForMetric(Metric metric) {
      return distributionFor(metric).size();
    }

    public int countForMetric(Metric metric, String label) {
      return distributionFor(metric).count(label);
    }

    public Duration debt() {
      return Duration.create(debtInMinutes);
    }

    public boolean hasIssues() {
      return !distributionFor(SEVERITY).isEmpty();
    }

    public List<Multiset.Entry<String>> statsForMetric(Metric metric) {
      return Multisets.copyHighestCountFirst(distributionFor(metric)).entrySet().asList();
    }

    private Multiset<String> distributionFor(Metric metric) {
      checkArgument(metric.isComputedByDistribution());
      return distributions.get(metric);
    }
  }
}
