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
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.measure.Measure;
import org.sonar.server.computation.measure.MeasureRepository;
import org.sonar.server.computation.metric.Metric;
import org.sonar.server.computation.metric.MetricRepository;

import static com.google.common.collect.Maps.newHashMap;
import static org.sonar.api.measures.CoreMetrics.RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;

/**
 * Compute effort related measures :
 * {@link CoreMetrics#TECHNICAL_DEBT_KEY}
 * {@link CoreMetrics#RELIABILITY_REMEDIATION_EFFORT_KEY}
 * {@link CoreMetrics#SECURITY_REMEDIATION_EFFORT_KEY}
 */
public class EffortAggregator extends IssueVisitor {

  private final RuleRepository ruleRepository;
  private final MeasureRepository measureRepository;

  private final Metric maintainabilityEffortMetric;
  private final Metric reliabilityEffortMetric;
  private final Metric securityEffortMetric;

  private final Map<Integer, EffortCounter> effortsByComponentRef = new HashMap<>();
  private EffortCounter effortCounter;

  public EffortAggregator(RuleRepository ruleRepository,
    MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.ruleRepository = ruleRepository;
    this.measureRepository = measureRepository;
    this.maintainabilityEffortMetric = metricRepository.getByKey(TECHNICAL_DEBT_KEY);
    this.reliabilityEffortMetric = metricRepository.getByKey(RELIABILITY_REMEDIATION_EFFORT_KEY);
    this.securityEffortMetric = metricRepository.getByKey(SECURITY_REMEDIATION_EFFORT_KEY);
  }

  @Override
  public void beforeComponent(Component component) {
    effortCounter = new EffortCounter();
    effortsByComponentRef.put(component.getReportAttributes().getRef(), effortCounter);

    // aggregate children counters
    for (Component child : component.getChildren()) {
      // no need to keep the children in memory. They can be garbage-collected.
      EffortCounter childEffortCounter = effortsByComponentRef.remove(child.getReportAttributes().getRef());
      if (childEffortCounter != null) {
        effortCounter.add(childEffortCounter);
      }
    }
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.resolution() == null) {
      effortCounter.add(issue);
    }
  }

  @Override
  public void afterComponent(Component component) {
    computeMaintainabilityEffortMeasure(component);
    computeReliabilityEffortMeasure(component);
    computeSecurityEffortMeasure(component);

    this.effortCounter = null;
  }

  private void computeMaintainabilityEffortMeasure(Component component){
    // total value
    measureRepository.add(component, maintainabilityEffortMetric, Measure.newMeasureBuilder().create(effortCounter.maintainabilityEffort));

    // TODO delete following lines when working on SONAR-7425
    // distribution by rule
    for (Map.Entry<Integer, Long> entry : effortCounter.maintainabilityEffortByRuleId.entrySet()) {
      int ruleId = entry.getKey();
      long ruleDebt = entry.getValue();
      // debt can't be zero.
      measureRepository.add(component, maintainabilityEffortMetric, Measure.newMeasureBuilder().forRule(ruleId).create(ruleDebt));
    }
  }

  private void computeReliabilityEffortMeasure(Component component){
    // total value
    measureRepository.add(component, reliabilityEffortMetric, Measure.newMeasureBuilder().create(effortCounter.reliabilityEffort));
  }

  private void computeSecurityEffortMeasure(Component component){
    // total value
    measureRepository.add(component, securityEffortMetric, Measure.newMeasureBuilder().create(effortCounter.securityEffort));
  }

  private class EffortCounter {
    private long maintainabilityEffort = 0L;
    private final SumMap<Integer> maintainabilityEffortByRuleId = new SumMap<>();
    private long reliabilityEffort = 0L;
    private long securityEffort = 0L;

    void add(DefaultIssue issue) {
      Long issueEffort = issue.effortInMinutes();
      if (issueEffort != null && issueEffort != 0L) {
        switch (issue.type()) {
          case CODE_SMELL :
            maintainabilityEffort += issueEffort;
            Rule rule = ruleRepository.getByKey(issue.ruleKey());
            maintainabilityEffortByRuleId.add(rule.getId(), issueEffort);
            break;
          case BUG:
            reliabilityEffort += issueEffort;
            break;
          case VULNERABILITY:
            securityEffort += issueEffort;
            break;
          default:
            throw new IllegalStateException(String.format("Unknown type '%s'", issue.type()));
        }
      }
    }

    public void add(EffortCounter effortCounter) {
      maintainabilityEffort += effortCounter.maintainabilityEffort;
      maintainabilityEffortByRuleId.add(effortCounter.maintainabilityEffortByRuleId);
      reliabilityEffort += effortCounter.reliabilityEffort;
      securityEffort += effortCounter.securityEffort;
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
