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
package org.sonar.server.measure.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

public class ProjectMeasuresQuery {

  public static final String SORT_BY_NAME = "name";

  private List<MetricCriterion> metricCriteria = new ArrayList<>();
  private Metric.Level qualityGateStatus;
  private String organizationUuid;
  private Set<String> projectUuids;
  private String sort;
  private boolean asc = true;

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

  public Optional<Metric.Level> getQualityGateStatus() {
    return Optional.ofNullable(qualityGateStatus);
  }

  public ProjectMeasuresQuery setOrganizationUuid(@Nullable String organizationUuid) {
    this.organizationUuid = organizationUuid;
    return this;
  }

  public Optional<String> getOrganizationUuid() {
    return Optional.ofNullable(organizationUuid);
  }

  public ProjectMeasuresQuery setProjectUuids(@Nullable Set<String> projectUuids) {
    this.projectUuids = projectUuids;
    return this;
  }

  public Optional<Set<String>> getProjectUuids() {
    return Optional.ofNullable(projectUuids);
  }

  @CheckForNull
  public String getSort() {
    return sort;
  }

  public ProjectMeasuresQuery setSort(@Nullable String sort) {
    this.sort = sort;
    return this;
  }

  public boolean isAsc() {
    return asc;
  }

  public ProjectMeasuresQuery setAsc(boolean asc) {
    this.asc = asc;
    return this;
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
}
