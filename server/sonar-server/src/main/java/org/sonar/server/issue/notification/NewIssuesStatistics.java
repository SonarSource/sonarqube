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
import org.sonar.api.issue.Issue;
import org.sonar.api.utils.Duration;
import org.sonar.core.util.MultiSets;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.server.issue.notification.NewIssuesStatistics.METRIC.*;

public class NewIssuesStatistics {
  private Map<String, Stats> statisticsByLogin = new HashMap<>();
  private Stats globalStatistics = new Stats();

  public void add(Issue issue) {
    globalStatistics.add(issue);
    String login = issue.assignee();
    if (login != null) {
      statisticsForLogin(login).add(issue);
    }
  }

  private Stats statisticsForLogin(String login) {
    if (statisticsByLogin.get(login) == null) {
      statisticsByLogin.put(login, new Stats());
    }
    return statisticsByLogin.get(login);
  }

  public int countForMetric(METRIC metric) {
    return globalStatistics.distributionFor(metric).size();
  }

  public int countForMetric(METRIC metric, String label) {
    return globalStatistics.distributionFor(metric).count(label);
  }

  public List<Multiset.Entry<String>> statsForMetric(METRIC metric) {
    return MultiSets.listOrderedByHighestCounts(globalStatistics.distributionFor(metric));
  }

  public Duration debt() {
    return globalStatistics.debt();
  }

  public boolean hasIssues() {
    return globalStatistics.hasIssues();
  }

  public enum METRIC {
    SEVERITY(true), TAGS(true), COMPONENT(true), LOGIN(true), DEBT(false);
    private final boolean computeDistribution;

    METRIC(boolean computeDistribution) {
      this.computeDistribution = computeDistribution;
    }

    boolean isComputedByDistribution() {
      return this.computeDistribution;
    }
  }

  private static class Stats {
    private final Map<METRIC, Multiset<String>> distributions = new EnumMap<>(METRIC.class);
    private long debtInMinutes = 0L;

    public Stats() {
      for (METRIC metric : METRIC.values()) {
        if (metric.isComputedByDistribution()) {
          distributions.put(metric, HashMultiset.<String>create());
        }
      }
    }

    public void add(Issue issue) {
      distributions.get(SEVERITY).add(issue.severity());
      distributions.get(COMPONENT).add(issue.componentUuid());
      if (issue.assignee() != null) {
        distributions.get(LOGIN).add(issue.assignee());
      }
      for (String tag : issue.tags()) {
        distributions.get(TAGS).add(tag);
      }
      if (issue.debt() != null) {
        debtInMinutes += issue.debt().toMinutes();
      }
    }

    public Multiset<String> distributionFor(METRIC metric) {
      checkArgument(metric.isComputedByDistribution());
      return distributions.get(metric);
    }

    public Duration debt() {
      return Duration.create(debtInMinutes);
    }

    public boolean hasIssues() {
      return distributions.get(SEVERITY) != null;
    }
  }
}
