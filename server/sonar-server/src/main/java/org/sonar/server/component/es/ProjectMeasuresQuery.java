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

package org.sonar.server.component.es;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.sonar.api.measures.Metric;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

public class ProjectMeasuresQuery {
  private List<MetricCriterion> metricCriteria = new ArrayList<>();
  private Metric.Level qualityGateStatus;
  private Set<String> projectUuids = null;

  public ProjectMeasuresQuery addMetricCriterion(MetricCriterion metricCriterion) {
    this.metricCriteria.add(metricCriterion);
    return this;
  }

  public List<MetricCriterion> getMetricCriteria() {
    return metricCriteria;
  }

  public ProjectMeasuresQuery setQualityGateStatus(Metric.Level qualityGateStatus) {
    this.qualityGateStatus = requireNonNull(qualityGateStatus);
    return this;
  }

  public boolean hasQualityGateStatus() {
    return qualityGateStatus != null;
  }

  public Metric.Level getQualityGateStatus() {
    checkState(qualityGateStatus != null);
    return qualityGateStatus;
  }

  public ProjectMeasuresQuery setProjectUuids(Set<String> projectUuids) {
    this.projectUuids = requireNonNull(projectUuids);
    return this;
  }

  public boolean doesFilterOnProjectUuids() {
    return projectUuids != null;
  }

  public Set<String> getProjectUuids() {
    return requireNonNull(projectUuids);
  }

  public enum Operator {
    LT("<"), LTE("<="), GT(">"), GTE(">="), EQ("=");

    String value;

    Operator(String value) {
      this.value = value;
    }

    String getValue() {
      return value;
    }

    public static Operator getByValue(String value) {
      return stream(Operator.values())
        .filter(operator -> operator.getValue().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(format("Unknown operator '%s'", value)));
    }
  }

  public static class MetricCriterion {
    private final String metricKey;
    private final Operator operator;
    private final double value;

    public MetricCriterion(String metricKey, Operator operator, double value) {
      this.metricKey = requireNonNull(metricKey);
      this.operator = requireNonNull(operator);
      this.value = requireNonNull(value);
    }

    public String getMetricKey() {
      return metricKey;
    }

    public Operator getOperator() {
      return operator;
    }

    public double getValue() {
      return value;
    }
  }
}
