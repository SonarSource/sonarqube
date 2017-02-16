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

import com.google.common.base.Splitter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric.Level;
import org.sonar.core.util.stream.Collectors;
import org.sonar.server.measure.index.ProjectMeasuresQuery;
import org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import org.sonar.server.measure.index.ProjectMeasuresQuery.Operator;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Locale.ENGLISH;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;

class ProjectMeasuresQueryFactory {
  private static final Splitter CRITERIA_SPLITTER = Splitter.on(Pattern.compile("and", Pattern.CASE_INSENSITIVE));
  private static final Pattern CRITERIA_PATTERN = Pattern.compile("(\\w+)\\s*([<>]?[=]?)\\s*(\\w+)");
  private static final String IS_FAVORITE_CRITERION = "isFavorite";

  private ProjectMeasuresQueryFactory() {
    // prevent instantiation
  }

  static List<String> toCriteria(String filter) {
    return StreamSupport.stream(CRITERIA_SPLITTER.split(filter).spliterator(), false)
      .filter(Objects::nonNull)
      .filter(criterion -> !criterion.isEmpty())
      .map(String::trim)
      .collect(Collectors.toList());
  }

  static boolean hasIsFavoriteCriterion(List<String> criteria) {
    return criteria.stream().anyMatch(IS_FAVORITE_CRITERION::equalsIgnoreCase);
  }

  static ProjectMeasuresQuery newProjectMeasuresQuery(List<String> criteria, @Nullable Set<String> projectUuids) {
    ProjectMeasuresQuery query = new ProjectMeasuresQuery();
    Optional.ofNullable(projectUuids).ifPresent(query::setProjectUuids);
    criteria.forEach(criterion -> processCriterion(criterion, query));
    return query;
  }

  private static void processCriterion(String rawCriterion, ProjectMeasuresQuery query) {
    String criterion = rawCriterion.trim();

    if (IS_FAVORITE_CRITERION.equalsIgnoreCase(criterion)) {
      return;
    }

    try {
      Matcher matcher = CRITERIA_PATTERN.matcher(criterion);
      checkArgument(matcher.find() && matcher.groupCount() == 3, "Criterion should be 'isFavourite' or criterion should have a metric, an operator and a value");
      String metric = matcher.group(1).toLowerCase(ENGLISH);
      Operator operator = Operator.getByValue(matcher.group(2));
      String value = matcher.group(3);
      if (ALERT_STATUS_KEY.equals(metric)) {
        checkArgument(operator.equals(Operator.EQ), "Only equals operator is available for quality gate criteria");
        query.setQualityGateStatus(Level.valueOf(value));
      } else {
        double doubleValue = Double.parseDouble(matcher.group(3));
        query.addMetricCriterion(new MetricCriterion(metric, operator, doubleValue));
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(String.format("Invalid criterion '%s'", criterion), e);
    }
  }

}
