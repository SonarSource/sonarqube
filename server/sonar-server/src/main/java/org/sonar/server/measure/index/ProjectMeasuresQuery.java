/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.sonar.server.component.ws.FilterParser.Operator;

public class ProjectMeasuresQuery {

  public static final String SORT_BY_NAME = "name";
  public static final String SORT_BY_LAST_ANALYSIS_DATE = "analysisDate";

  private List<MetricCriterion> metricCriteria = new ArrayList<>();
  private Metric.Level qualityGateStatus;
  private String organizationUuid;
  private Set<String> projectUuids;
  private Set<String> languages;
  private Set<String> tags;
  private String sort = SORT_BY_NAME;
  private boolean asc = true;
  private String queryText;

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

  public ProjectMeasuresQuery setLanguages(@Nullable Set<String> languages) {
    this.languages = languages;
    return this;
  }

  public Optional<Set<String>> getLanguages() {
    return Optional.ofNullable(languages);
  }

  public ProjectMeasuresQuery setTags(@Nullable Set<String> tags) {
    this.tags = tags;
    return this;
  }

  public Optional<Set<String>> getTags() {
    return Optional.ofNullable(tags);
  }

  public Optional<String> getQueryText() {
    return Optional.ofNullable(queryText);
  }

  public ProjectMeasuresQuery setQueryText(@Nullable String queryText) {
    this.queryText = queryText;
    return this;
  }

  public String getSort() {
    return sort;
  }

  public ProjectMeasuresQuery setSort(String sort) {
    this.sort = requireNonNull(sort, "Sort cannot be null");
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
    @Nullable
    private final Double value;

    private MetricCriterion(String metricKey, @Nullable Operator operator, @Nullable Double value) {
      this.metricKey = metricKey;
      this.operator = operator;
      this.value = value;
    }

    public String getMetricKey() {
      return metricKey;
    }

    public Operator getOperator() {
      checkDataAvailable();
      return operator;
    }

    public double getValue() {
      checkDataAvailable();
      return value;
    }

    public boolean isNoData() {
      return value == null;
    }

    public static MetricCriterion createNoData(String metricKey) {
      return new MetricCriterion(requireNonNull(metricKey), null, null);
    }

    public static MetricCriterion create(String metricKey, Operator operator, double value) {
      return new MetricCriterion(requireNonNull(metricKey), requireNonNull(operator), value);
    }

    private void checkDataAvailable() {
      checkState(!isNoData(), "The criterion for metric %s has no data", metricKey);
    }
  }

}
