/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import static org.sonar.api.measures.CoreMetrics.RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.SECURITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.api.measures.CoreMetrics.TECHNICAL_DEBT_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY;
import static org.sonar.core.metric.SoftwareQualitiesMetrics.SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY;

/**
 * Compute effort related measures :
 * {@link CoreMetrics#TECHNICAL_DEBT_KEY}
 * {@link CoreMetrics#RELIABILITY_REMEDIATION_EFFORT_KEY}
 * {@link CoreMetrics#SECURITY_REMEDIATION_EFFORT_KEY}
 * {@link SoftwareQualitiesMetrics#SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY}
 * {@link SoftwareQualitiesMetrics#SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY}
 * {@link SoftwareQualitiesMetrics#SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY}
 */
public class EffortAggregator extends MeasureComputationIssueVisitor {

  private final MeasureRepository measureRepository;
  private final Map<String, EffortCounter> effortsByComponentUuid = new HashMap<>();

  private final Metric maintainabilityEffortMetric;
  private final Metric reliabilityEffortMetric;
  private final Metric securityEffortMetric;

  private final Metric softwareQualityMaintainabilityEffortMetric;
  private final Metric softwareQualityReliabilityEffortMetric;
  private final Metric softwareQualitySecurityEffortMetric;

  private EffortCounter effortCounter;

  public EffortAggregator(MetricRepository metricRepository, MeasureRepository measureRepository) {
    this.measureRepository = measureRepository;

    // Based on issue Type and Severity
    this.maintainabilityEffortMetric = metricRepository.getByKey(TECHNICAL_DEBT_KEY);
    this.reliabilityEffortMetric = metricRepository.getByKey(RELIABILITY_REMEDIATION_EFFORT_KEY);
    this.securityEffortMetric = metricRepository.getByKey(SECURITY_REMEDIATION_EFFORT_KEY);

    // Based on software qualities
    this.softwareQualityMaintainabilityEffortMetric = metricRepository.getByKey(SOFTWARE_QUALITY_MAINTAINABILITY_REMEDIATION_EFFORT_KEY);
    this.softwareQualityReliabilityEffortMetric = metricRepository.getByKey(SOFTWARE_QUALITY_RELIABILITY_REMEDIATION_EFFORT_KEY);
    this.softwareQualitySecurityEffortMetric = metricRepository.getByKey(SOFTWARE_QUALITY_SECURITY_REMEDIATION_EFFORT_KEY);
  }

  @Override
  public void beforeComponent(Component component) {
    effortCounter = new EffortCounter();
    effortsByComponentUuid.put(component.getUuid(), effortCounter);

    // aggregate children counters
    for (Component child : component.getChildren()) {
      // no need to keep the children in memory. They can be garbage-collected.
      EffortCounter childEffortCounter = effortsByComponentUuid.remove(child.getUuid());
      if (childEffortCounter != null) {
        effortCounter.add(childEffortCounter);
      }
    }
  }

  @Override
  protected void onNonSandboxedIssue(Component component, DefaultIssue issue) {
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

  private void computeMaintainabilityEffortMeasure(Component component) {
    measureRepository.add(component, maintainabilityEffortMetric, Measure.newMeasureBuilder().create(effortCounter.maintainabilityEffort));
    measureRepository.add(component, softwareQualityMaintainabilityEffortMetric, Measure.newMeasureBuilder().create(effortCounter.softwareQualityMaintainabilityEffort));
  }

  private void computeReliabilityEffortMeasure(Component component) {
    measureRepository.add(component, reliabilityEffortMetric, Measure.newMeasureBuilder().create(effortCounter.reliabilityEffort));
    measureRepository.add(component, softwareQualityReliabilityEffortMetric, Measure.newMeasureBuilder().create(effortCounter.softwareQualityReliabilityEffort));
  }

  private void computeSecurityEffortMeasure(Component component) {
    measureRepository.add(component, securityEffortMetric, Measure.newMeasureBuilder().create(effortCounter.securityEffort));
    measureRepository.add(component, softwareQualitySecurityEffortMetric, Measure.newMeasureBuilder().create(effortCounter.softwareQualitySecurityEffort));
  }

  private static class EffortCounter {
    private long maintainabilityEffort = 0L;
    private long reliabilityEffort = 0L;
    private long securityEffort = 0L;

    private long softwareQualityMaintainabilityEffort = 0L;
    private long softwareQualityReliabilityEffort = 0L;
    private long softwareQualitySecurityEffort = 0L;

    void add(DefaultIssue issue) {
      Long issueEffort = issue.effortInMinutes();
      if (issueEffort != null && issueEffort != 0L) {
        computeTypeEffort(issue, issueEffort);
        computeSoftwareQualityEffort(issue, issueEffort);
      }
    }

    private void computeSoftwareQualityEffort(DefaultIssue issue, Long issueEffort) {
      issue.impacts().forEach((sq, severity) -> {
        switch (sq) {
          case MAINTAINABILITY:
            softwareQualityMaintainabilityEffort += issueEffort;
            break;
          case RELIABILITY:
            softwareQualityReliabilityEffort += issueEffort;
            break;
          case SECURITY:
            softwareQualitySecurityEffort += issueEffort;
            break;
          default:
            throw new IllegalStateException(String.format("Unknown software quality '%s'", sq));
        }
      });
    }

    private void computeTypeEffort(DefaultIssue issue, Long issueEffort) {
      switch (issue.type()) {
        case CODE_SMELL:
          maintainabilityEffort += issueEffort;
          break;
        case BUG:
          reliabilityEffort += issueEffort;
          break;
        case VULNERABILITY:
          securityEffort += issueEffort;
          break;
        case SECURITY_HOTSPOT:
          // Not counted
          break;
        default:
          throw new IllegalStateException(String.format("Unknown type '%s'", issue.type()));
      }
    }

    public void add(EffortCounter effortCounter) {
      maintainabilityEffort += effortCounter.maintainabilityEffort;
      reliabilityEffort += effortCounter.reliabilityEffort;
      securityEffort += effortCounter.securityEffort;

      softwareQualityMaintainabilityEffort += effortCounter.softwareQualityMaintainabilityEffort;
      softwareQualityReliabilityEffort += effortCounter.softwareQualityReliabilityEffort;
      softwareQualitySecurityEffort += effortCounter.softwareQualitySecurityEffort;
    }
  }
}
