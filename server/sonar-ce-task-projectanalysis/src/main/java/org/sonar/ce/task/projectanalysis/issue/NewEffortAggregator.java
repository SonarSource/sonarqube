/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.measure.Measure;
import org.sonar.ce.task.projectanalysis.measure.MeasureRepository;
import org.sonar.ce.task.projectanalysis.metric.Metric;
import org.sonar.ce.task.projectanalysis.metric.MetricRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.metric.SoftwareQualitiesMetrics;

import static org.sonar.api.measures.CoreMetrics.NEW_RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.NEW_TECHNICAL_DEBT_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY;

/**
 * Compute new effort related measures :
 * {@link CoreMetrics#NEW_TECHNICAL_DEBT_KEY}
 * {@link CoreMetrics#NEW_RELIABILITY_REMEDIATION_EFFORT_KEY}
 * {@link CoreMetrics#NEW_SECURITY_REMEDIATION_EFFORT_KEY}
 * {@link SoftwareQualitiesMetrics#NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY}
 * {@link SoftwareQualitiesMetrics#NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY}
 * {@link SoftwareQualitiesMetrics#NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY}
 */
public class NewEffortAggregator extends IssueVisitor {
  private final Map<String, NewEffortCounter> counterByComponentUuid = new HashMap<>();
  private final MeasureRepository measureRepository;

  private final Metric newMaintainabilityEffortMetric;
  private final Metric newReliabilityEffortMetric;
  private final Metric newSecurityEffortMetric;
  private final NewIssueClassifier newIssueClassifier;
  private final Metric newSoftwareQualityMaintainabilityEffortMetric;
  private final Metric newSoftwareQualityReliabilityEffortMetric;
  private final Metric newSoftwareQualitySecurityEffortMetric;

  private NewEffortCounter counter = null;

  public NewEffortAggregator(MetricRepository metricRepository, MeasureRepository measureRepository, NewIssueClassifier newIssueClassifier) {
    this.measureRepository = measureRepository;

    // Based on issue Type and Severity
    this.newMaintainabilityEffortMetric = metricRepository.getByKey(NEW_TECHNICAL_DEBT_KEY);
    this.newReliabilityEffortMetric = metricRepository.getByKey(NEW_RELIABILITY_REMEDIATION_EFFORT_KEY);
    this.newSecurityEffortMetric = metricRepository.getByKey(NEW_SECURITY_REMEDIATION_EFFORT_KEY);

    // Based on software qualities
    this.newSoftwareQualityMaintainabilityEffortMetric = metricRepository.getByKey(NEW_SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY);
    this.newSoftwareQualityReliabilityEffortMetric = metricRepository.getByKey(NEW_SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY);
    this.newSoftwareQualitySecurityEffortMetric = metricRepository.getByKey(NEW_SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY);

    this.newIssueClassifier = newIssueClassifier;
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
      counter.add(component, issue);
    }
  }

  @Override
  public void afterComponent(Component component) {
    if (newIssueClassifier.isEnabled()) {
      computeMeasure(component, newMaintainabilityEffortMetric, counter.maintainabilitySum);
      computeMeasure(component, newReliabilityEffortMetric, counter.reliabilitySum);
      computeMeasure(component, newSecurityEffortMetric, counter.securitySum);

      computeMeasure(component, newSoftwareQualityMaintainabilityEffortMetric, counter.softwareQualityMaintainabilitySum);
      computeMeasure(component, newSoftwareQualityReliabilityEffortMetric, counter.softwareQualityReliabilitySum);
      computeMeasure(component, newSoftwareQualitySecurityEffortMetric, counter.softwareQualitySecuritySum);
    }
    counter = null;
  }

  private void computeMeasure(Component component, Metric metric, EffortSum effortSum) {
    long value = effortSum.isEmpty ? 0 : effortSum.newEffort;
    measureRepository.add(component, metric, Measure.newMeasureBuilder().create(value));
  }

  private class NewEffortCounter {
    private final EffortSum maintainabilitySum = new EffortSum();
    private final EffortSum reliabilitySum = new EffortSum();
    private final EffortSum securitySum = new EffortSum();

    private final EffortSum softwareQualityMaintainabilitySum = new EffortSum();
    private final EffortSum softwareQualityReliabilitySum = new EffortSum();
    private final EffortSum softwareQualitySecuritySum = new EffortSum();

    void add(NewEffortCounter otherCounter) {
      maintainabilitySum.add(otherCounter.maintainabilitySum);
      reliabilitySum.add(otherCounter.reliabilitySum);
      securitySum.add(otherCounter.securitySum);

      softwareQualityMaintainabilitySum.add(otherCounter.softwareQualityMaintainabilitySum);
      softwareQualityReliabilitySum.add(otherCounter.softwareQualityReliabilitySum);
      softwareQualitySecuritySum.add(otherCounter.softwareQualitySecuritySum);
    }

    void add(Component component, DefaultIssue issue) {
      long newEffort = calculate(component, issue);
      computeTypeEffort(issue, newEffort);
      computeSoftwareQualityEffort(issue, newEffort);
    }

    private void computeSoftwareQualityEffort(DefaultIssue issue, long newEffort) {
      issue.impacts().forEach((sq, severity) -> {
        switch (sq) {
          case MAINTAINABILITY:
            softwareQualityMaintainabilitySum.add(newEffort);
            break;
          case RELIABILITY:
            softwareQualityReliabilitySum.add(newEffort);
            break;
          case SECURITY:
            softwareQualitySecuritySum.add(newEffort);
            break;
          default:
            throw new IllegalStateException(String.format("Unknown software quality '%s'", sq));
        }
      });
    }

    private void computeTypeEffort(DefaultIssue issue, long newEffort) {
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

    long calculate(Component component, DefaultIssue issue) {
      if (newIssueClassifier.isNew(component, issue)) {
        return MoreObjects.firstNonNull(issue.effortInMinutes(), 0L);
      }

      return 0L;
    }
  }

  private static class EffortSum {
    private Long newEffort;
    private boolean isEmpty = true;

    void add(long newEffort) {
      long previous = MoreObjects.firstNonNull(this.newEffort, 0L);
      this.newEffort = previous + newEffort;
      isEmpty = false;
    }

    void add(EffortSum other) {
      Long otherValue = other.newEffort;
      if (otherValue != null) {
        add(otherValue);
      }
    }
  }
}
