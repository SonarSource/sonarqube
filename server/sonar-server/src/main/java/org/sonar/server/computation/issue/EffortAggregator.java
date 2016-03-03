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
package org.sonar.server.computation.issue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.rules.RuleType;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;

import static com.google.common.collect.Maps.newHashMap;

/**
 * Compute effort related measures :
 * {@link CoreMetrics#TECHNICAL_DEBT_KEY}
 */
public class EffortAggregator extends IssueVisitor {

  private final RuleRepository ruleRepository;
  private final MetricRepository metricRepository;
  private final MeasureRepository measureRepository;

  private final Map<Integer, Effort> effortsByComponentRef = new HashMap<>();
  private Effort effort;

  public EffortAggregator(RuleRepository ruleRepository,
    MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.ruleRepository = ruleRepository;
    this.metricRepository = metricRepository;
    this.measureRepository = measureRepository;
  }

  @Override
  public void beforeComponent(Component component) {
    effort = new Effort();
    effortsByComponentRef.put(component.getReportAttributes().getRef(), effort);

    // aggregate children counters
    for (Component child : component.getChildren()) {
      // no need to keep the children in memory. They can be garbage-collected.
      Effort childEffort = effortsByComponentRef.remove(child.getReportAttributes().getRef());
      if (childEffort != null) {
        effort.add(childEffort);
      }
    }
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.resolution() == null) {
      effort.add(issue);
    }
  }

  @Override
  public void afterComponent(Component component) {
    Metric metric = metricRepository.getByKey(CoreMetrics.TECHNICAL_DEBT_KEY);

    // total value
    measureRepository.add(component, metric, Measure.newMeasureBuilder().create(this.effort.maintainabilityEffort));

    // TODO delete following lines when working on SONAR-7425
    // distribution by rule
    for (Map.Entry<Integer, Long> entry : effort.minutesByRuleId.entrySet()) {
      int ruleId = entry.getKey();
      long ruleDebt = entry.getValue();
      // debt can't be zero.
      measureRepository.add(component, metric, Measure.newMeasureBuilder().forRule(ruleId).create(ruleDebt));
    }

    this.effort = null;
  }

  private class Effort {
    private long maintainabilityEffort = 0L;
    private final SumMap<Integer> minutesByRuleId = new SumMap<>();

    void add(DefaultIssue issue) {
      Long issueEffort = issue.debtInMinutes();
      if (issueEffort != null && issueEffort != 0L) {
        if (issue.type().equals(RuleType.CODE_SMELL)) {
          this.maintainabilityEffort += issueEffort;
          Rule rule = ruleRepository.getByKey(issue.ruleKey());
          this.minutesByRuleId.add(rule.getId(), issueEffort);
        }
      }
    }

    public void add(Effort effort) {
      this.maintainabilityEffort += effort.maintainabilityEffort;
      this.minutesByRuleId.add(effort.minutesByRuleId);
    }
  }

  private static class SumMap<E> {
    private final Map<E, Long> sumByKeys = newHashMap();

    void add(SumMap<E> other) {
      for (Map.Entry<E, Long> entry : other.entrySet()) {
        add(entry.getKey(), entry.getValue());
      }
    }

    void add(@Nullable E key, Long value) {
      if (key != null) {
        Long currentValue = sumByKeys.get(key);
        sumByKeys.put(key, currentValue != null ? (currentValue + value) : value);
      }
    }

    @CheckForNull
    Long get(E key) {
      return sumByKeys.get(key);
    }

    Set<Map.Entry<E, Long>> entrySet() {
      return sumByKeys.entrySet();
    }
  }
}
