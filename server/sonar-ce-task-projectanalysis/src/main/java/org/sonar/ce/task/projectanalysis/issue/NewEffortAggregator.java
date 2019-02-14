/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.base.MoreObjects;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.ce.task.projectanalysis.period.PeriodHolder;
import org.sonar.core.issue.DefaultIssue;

import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT_KEY;
import static org.sonar.api.utils.DateUtils.truncateToSeconds;

/**
 * Compute new effort related measures :
 * {@link CoreMetrics#NEW_TECHNICAL_DEBT_KEY}
 * {@link CoreMetrics#NEW_RELIABILITY_REMEDIATION_EFFORT_KEY}
 * {@link CoreMetrics#NEW_SECURITY_REMEDIATION_EFFORT_KEY}
 */
public class NewEffortAggregator extends IssueVisitor {
  private final Map<String, NewEffortCounter> counterByComponentUuid = new HashMap<>();
  private final PeriodHolder periodHolder;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final MeasureRepository measureRepository;

  private final Metric newMaintainabilityEffortMetric;
  private final Metric newReliabilityEffortMetric;
  private final Metric newSecurityEffortMetric;

  private NewEffortCounter counter = null;

  public NewEffortAggregator(PeriodHolder periodHolder, AnalysisMetadataHolder analysisMetadataHolder, MetricRepository metricRepository,
    MeasureRepository measureRepository) {
    this.periodHolder = periodHolder;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.measureRepository = measureRepository;

    this.newMaintainabilityEffortMetric = metricRepository.getByKey(NEW_TECHNICAL_DEBT_KEY);
    this.newReliabilityEffortMetric = metricRepository.getByKey(NEW_RELIABILITY_REMEDIATION_EFFORT_KEY);
    this.newSecurityEffortMetric = metricRepository.getByKey(NEW_SECURITY_REMEDIATION_EFFORT_KEY);
  }

  @Override
  public void beforeComponent(Component component) {
    counter = new NewEffortCounter();
    counterByComponentUuid.put(component.getUuid(), counter);
    for (Component child : component.getChildren()) {
      NewEffortCounter childSum = counterByComponentUuid.remove(child.getUuid());
      if (childSum != null) {
        counter.add(childSum);
      }
    }
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.resolution() == null && issue.effortInMinutes() != null) {
      if (analysisMetadataHolder.isSLBorPR()) {
        counter.add(issue, 0L);
      } else if (periodHolder.hasPeriod()) {
        counter.add(issue, periodHolder.getPeriod().getSnapshotDate());
      }
    }
  }

  @Override
  public void afterComponent(Component component) {
    if (periodHolder.hasPeriod() || analysisMetadataHolder.isSLBorPR()) {
      computeMeasure(component, newMaintainabilityEffortMetric, counter.maintainabilitySum);
      computeMeasure(component, newReliabilityEffortMetric, counter.reliabilitySum);
      computeMeasure(component, newSecurityEffortMetric, counter.securitySum);
    }
    counter = null;
  }

  private void computeMeasure(Component component, Metric metric, EffortSum effortSum) {
    double variation = effortSum.isEmpty ? 0.0 : effortSum.newEffort;
    measureRepository.add(component, metric, Measure.newMeasureBuilder().setVariation(variation).createNoValue());
  }

  private static class NewEffortCounter {
    private final EffortSum maintainabilitySum = new EffortSum();
    private final EffortSum reliabilitySum = new EffortSum();
    private final EffortSum securitySum = new EffortSum();

    void add(NewEffortCounter otherCounter) {
      maintainabilitySum.add(otherCounter.maintainabilitySum);
      reliabilitySum.add(otherCounter.reliabilitySum);
      securitySum.add(otherCounter.securitySum);
    }

    void add(DefaultIssue issue, long startDate) {
      long newEffort = calculate(issue, startDate);
      switch (issue.type()) {
        case CODE_SMELL:
          maintainabilitySum.add(newEffort);
          break;
        case BUG:
          reliabilitySum.add(newEffort);
          break;
        case VULNERABILITY:
          securitySum.add(newEffort);
          break;
        case SECURITY_HOTSPOT:
          // Not counted
          break;
        default:
          throw new IllegalStateException(String.format("Unknown type '%s'", issue.type()));
      }
    }

    long calculate(DefaultIssue issue, long startDate) {
      if (issue.creationDate().getTime() > truncateToSeconds(startDate)) {
        return MoreObjects.firstNonNull(issue.effortInMinutes(), 0L);
      }
      return 0L;
    }
  }

  private static class EffortSum {
    private Double newEffort;
    private boolean isEmpty = true;

    void add(long newEffort) {
      double previous = MoreObjects.firstNonNull(this.newEffort, 0d);
      this.newEffort = previous + newEffort;
      isEmpty = false;
    }

    void add(EffortSum other) {
      Double otherValue = other.newEffort;
      if (otherValue != null) {
        add(otherValue.longValue());
      }
    }
  }
}
