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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric.Level;
import org.sonar.server.measure.index.ProjectMeasuresQuery;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.singleton;
import static java.util.Locale.ENGLISH;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.Operator;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.Operator.EQ;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_LANGUAGE;

class ProjectMeasuresQueryFactory {

  public static final String IS_FAVORITE_CRITERION = "isFavorite";
  public static final String IN_OPERATOR = "IN";

  private ProjectMeasuresQueryFactory() {
    // prevent instantiation
  }

  static ProjectMeasuresQuery newProjectMeasuresQuery(List<FilterParser.Criterion> criteria, @Nullable Set<String> projectUuids) {
    ProjectMeasuresQuery query = new ProjectMeasuresQuery();
    Optional.ofNullable(projectUuids).ifPresent(query::setProjectUuids);
    criteria.forEach(criterion -> processCriterion(criterion, query));
    return query;
  }

  private static void processCriterion(FilterParser.Criterion criterion, ProjectMeasuresQuery query) {
    String key = criterion.getKey().toLowerCase(ENGLISH);
    if (IS_FAVORITE_CRITERION.equalsIgnoreCase(key)) {
      return;
    }

    String operatorValue = criterion.getOperator();
    checkArgument(operatorValue != null, "Operator cannot be null for '%s'", key);
    if (FILTER_LANGUAGE.equalsIgnoreCase(key)) {
      processLanguages(criterion, query);
      return;
    }

    String value = criterion.getValue();
    checkArgument(value != null, "Value cannot be null for '%s'", key);
    Operator operator = Operator.getByValue(criterion.getOperator());
    if (ALERT_STATUS_KEY.equals(key)) {
      checkArgument(operator.equals(EQ), "Only equals operator is available for quality gate criteria");
      query.setQualityGateStatus(Level.valueOf(value));
    } else {
      query.addMetricCriterion(new MetricCriterion(key, operator, parseValue(value)));
    }
  }

  private static void processLanguages(FilterParser.Criterion criterion, ProjectMeasuresQuery query) {
    String operatorValue = criterion.getOperator();
    String value = criterion.getValue();
    List<String> values = criterion.getValues();
    if (value != null && EQ.getValue().equalsIgnoreCase(operatorValue)) {
      query.setLanguages(singleton(value));
    } else if (!values.isEmpty() && IN_OPERATOR.equalsIgnoreCase(operatorValue)) {
      query.setLanguages(new HashSet<>(values));
    } else {
      throw new IllegalArgumentException("Language should be set either by using 'language = java' or 'language IN (java, js)'");
    }
  }

  private static double parseValue(String value) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(String.format("Value '%s' is not a number", value));
    }
  }

}
