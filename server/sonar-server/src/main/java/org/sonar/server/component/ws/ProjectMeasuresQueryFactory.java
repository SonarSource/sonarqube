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

package org.sonar.server.component.ws;

import com.google.common.base.Splitter;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.Metric.Level;
import org.sonar.server.component.es.ProjectMeasuresQuery;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Locale.ENGLISH;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.server.component.es.ProjectMeasuresQuery.MetricCriterion;
import static org.sonar.server.component.es.ProjectMeasuresQuery.Operator;

class ProjectMeasuresQueryFactory {
  private static final Splitter CRITERIA_SPLITTER = Splitter.on(Pattern.compile("and", Pattern.CASE_INSENSITIVE));
  private static final Pattern CRITERIA_PATTERN = Pattern.compile("(\\w+)\\s*([<>]?[=]?)\\s*(\\w+)");
  private static final String IS_FAVORITE_CRITERION = "isFavorite";

  private ProjectMeasuresQueryFactory() {
    // prevent instantiation
  }

  static ProjectMeasuresQuery newProjectMeasuresQuery(String filter, Set<String> favoriteProjectUuids) {
    if (StringUtils.isBlank(filter)) {
      return new ProjectMeasuresQuery();
    }

    ProjectMeasuresQuery query = new ProjectMeasuresQuery();

    CRITERIA_SPLITTER.split(filter)
      .forEach(criteria -> processCriterion(criteria, query, favoriteProjectUuids));
    return query;
  }

  private static void processCriterion(String rawCriterion, ProjectMeasuresQuery query, Set<String> favoriteProjectUuids) {
    String criterion = rawCriterion.trim();

    try {
      if (IS_FAVORITE_CRITERION.equalsIgnoreCase(criterion)) {
        query.setProjectUuids(favoriteProjectUuids);
        return;
      }

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
