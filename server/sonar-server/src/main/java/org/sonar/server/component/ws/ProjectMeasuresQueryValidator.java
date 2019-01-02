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
package org.sonar.server.component.ws;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.server.measure.index.ProjectMeasuresQuery;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static org.sonar.core.util.stream.MoreCollectors.toHashSet;
import static org.sonar.db.measure.ProjectMeasuresIndexerIterator.METRIC_KEYS;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_LAST_ANALYSIS_DATE;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.SORT_BY_NAME;

public class ProjectMeasuresQueryValidator {

  static final Set<String> NON_METRIC_SORT_KEYS = new HashSet<>(asList(SORT_BY_NAME, SORT_BY_LAST_ANALYSIS_DATE));

  private ProjectMeasuresQueryValidator() {
  }

  public static void validate(ProjectMeasuresQuery query) {
    validateFilterKeys(query.getMetricCriteria().stream().map(MetricCriterion::getMetricKey).collect(toHashSet()));
    validateSort(query.getSort());
  }

  private static void validateFilterKeys(Set<String> metricsKeys) {
    String invalidKeys = metricsKeys.stream()
      .filter(metric -> !METRIC_KEYS.contains(metric))
      .map(metric -> '\''+metric+'\'')
      .collect(Collectors.joining(", "));
    checkArgument(invalidKeys.isEmpty(), "Following metrics are not supported: %s", invalidKeys);
  }

  private static void validateSort(@Nullable String sort) {
    if (sort == null) {
      return;
    }
    if (NON_METRIC_SORT_KEYS.contains(sort)) {
      return;
    }
    validateFilterKeys(Collections.singleton(sort));
  }
}
