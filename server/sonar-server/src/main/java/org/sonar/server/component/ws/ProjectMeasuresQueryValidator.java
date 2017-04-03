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
package org.sonar.server.component.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.measure.index.ProjectMeasuresQuery;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sonar.core.util.stream.MoreCollectors.toHashSet;
import static org.sonar.core.util.stream.MoreCollectors.toSet;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_NAME;

public class ProjectMeasuresQueryValidator {

  private final DbClient dbClient;

  public ProjectMeasuresQueryValidator(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  public void validate(DbSession dbSession, ProjectMeasuresQuery query) {
    Set<String> metricKeys = getMetrics(query);
    if (metricKeys.isEmpty()) {
      return;
    }
    List<MetricDto> dbMetrics = dbClient.metricDao().selectByKeys(dbSession, new ArrayList<>(metricKeys));
    checkMetricKeysExists(dbMetrics, metricKeys);
    checkMetricsAreEnabled(dbMetrics);
    checkMetricsAreNumerics(dbMetrics);
  }

  private static Set<String> getMetrics(ProjectMeasuresQuery query) {
    Set<String> metricKeys = query.getMetricCriteria().stream().map(MetricCriterion::getMetricKey).collect(toHashSet());
    if (query.getSort() != null && !SORT_BY_NAME.equals(query.getSort())) {
      metricKeys.add(query.getSort());
    }
    return metricKeys;
  }

  private static void checkMetricKeysExists(List<MetricDto> dbMetrics, Set<String> inputMetricKeys) {
    Set<String> dbMetricsKeys = dbMetrics.stream().map(MetricDto::getKey).collect(toSet());
    Set<String> unknownKeys = inputMetricKeys.stream().filter(metricKey -> !dbMetricsKeys.contains(metricKey)).collect(toSet());
    checkArgument(unknownKeys.isEmpty(), "Unknown metric(s) %s", new TreeSet<>(unknownKeys));
  }

  private static void checkMetricsAreEnabled(List<MetricDto> dbMetrics) {
    Set<String> invalidKeys = dbMetrics.stream()
      .filter(metricDto -> !metricDto.isEnabled())
      .map(MetricDto::getKey)
      .collect(toSet());
    checkArgument(invalidKeys.isEmpty(), "Following metrics are disabled : %s", new TreeSet<>(invalidKeys));
  }

  private static void checkMetricsAreNumerics(List<MetricDto> dbMetrics) {
    Set<String> invalidKeys = dbMetrics.stream()
      .filter(MetricDto::isDataType)
      .map(MetricDto::getKey)
      .collect(toSet());
    checkArgument(invalidKeys.isEmpty(), "Following metrics are not numeric : %s", new TreeSet<>(invalidKeys));
  }

}
