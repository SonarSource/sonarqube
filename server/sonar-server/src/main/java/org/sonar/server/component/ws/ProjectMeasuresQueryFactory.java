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
package org.sonar.server.component.ws;

import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric.Level;
import org.sonar.server.component.ws.FilterParser.Criterion;
import org.sonar.server.component.ws.FilterParser.Operator;
import org.sonar.server.measure.index.ProjectMeasuresQuery;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.Locale.ENGLISH;
import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.server.component.ws.FilterParser.Operator.EQ;
import static org.sonar.server.component.ws.FilterParser.Operator.IN;
import static org.sonar.server.measure.index.ProjectMeasuresQuery.MetricCriterion;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_LANGUAGES;
import static org.sonarqube.ws.client.project.ProjectsWsParameters.FILTER_TAGS;

class ProjectMeasuresQueryFactory {

  public static final String IS_FAVORITE_CRITERION = "isFavorite";
  public static final String QUERY_KEY = "query";
  private static final String NO_DATA = "NO_DATA";

  private static final Map<String, BiConsumer<Criterion, ProjectMeasuresQuery>> CRITERION_PROCESSORS = ImmutableMap.<String, BiConsumer<Criterion, ProjectMeasuresQuery>>builder()
    .put(IS_FAVORITE_CRITERION.toLowerCase(ENGLISH), (criterion, query) -> processIsFavorite(criterion))
    .put(FILTER_LANGUAGES, ProjectMeasuresQueryFactory::processLanguages)
    .put(FILTER_TAGS, ProjectMeasuresQueryFactory::processTags)
    .put(QUERY_KEY, ProjectMeasuresQueryFactory::processQuery)
    .put(ALERT_STATUS_KEY, ProjectMeasuresQueryFactory::processQualityGateStatus)
    .build();

  private ProjectMeasuresQueryFactory() {
    // prevent instantiation
  }

  static ProjectMeasuresQuery newProjectMeasuresQuery(List<Criterion> criteria, @Nullable Set<String> projectUuids) {
    ProjectMeasuresQuery query = new ProjectMeasuresQuery();
    Optional.ofNullable(projectUuids).ifPresent(query::setProjectUuids);
    criteria.forEach(criterion -> processCriterion(criterion, query));
    return query;
  }

  private static void processCriterion(Criterion criterion, ProjectMeasuresQuery query) {
    String key = criterion.getKey().toLowerCase(ENGLISH);
    CRITERION_PROCESSORS.getOrDefault(key, ProjectMeasuresQueryFactory::processMetricCriterion).accept(criterion, query);
  }

  private static void processIsFavorite(Criterion criterion) {
    checkArgument(criterion.getOperator() == null && criterion.getValue() == null, "Filter on favorites should be declared without an operator nor a value");
  }

  private static void processLanguages(Criterion criterion, ProjectMeasuresQuery query) {
    checkOperator(criterion);
    Operator operator = criterion.getOperator();
    String value = criterion.getValue();
    List<String> values = criterion.getValues();
    if (value != null && EQ.equals(operator)) {
      query.setLanguages(singleton(value));
      return;
    }
    if (!values.isEmpty() && IN.equals(operator)) {
      query.setLanguages(new HashSet<>(values));
      return;
    }
    throw new IllegalArgumentException("Languages should be set either by using 'languages = java' or 'languages IN (java, js)'");
  }

  private static void processTags(Criterion criterion, ProjectMeasuresQuery query) {
    checkOperator(criterion);
    Operator operator = criterion.getOperator();
    String value = criterion.getValue();
    List<String> values = criterion.getValues();
    if (value != null && EQ.equals(operator)) {
      query.setTags(singleton(value));
      return;
    }
    if (!values.isEmpty() && IN.equals(operator)) {
      query.setTags(new HashSet<>(values));
      return;
    }
    throw new IllegalArgumentException("Tags should be set either by using 'tags = java' or 'tags IN (finance, platform)'");
  }

  private static void processQuery(Criterion criterion, ProjectMeasuresQuery query) {
    checkOperator(criterion);
    Operator operatorValue = criterion.getOperator();
    String value = criterion.getValue();
    checkArgument(value != null, "Query is invalid");
    checkArgument(EQ.equals(operatorValue), "Query should only be used with equals operator");
    query.setQueryText(value);
  }

  private static void processQualityGateStatus(Criterion criterion, ProjectMeasuresQuery query) {
    checkOperator(criterion);
    checkValue(criterion);
    Operator operator = criterion.getOperator();
    String value = criterion.getValue();
    checkArgument(EQ.equals(operator), "Only equals operator is available for quality gate criteria");
    Level qualityGate = Arrays.stream(Level.values()).filter(level -> level.name().equalsIgnoreCase(value)).findFirst()
      .orElseThrow(() -> new IllegalArgumentException(format("Unknown quality gate status : '%s'", value)));
    query.setQualityGateStatus(qualityGate);
  }

  private static void processMetricCriterion(Criterion criterion, ProjectMeasuresQuery query) {
    checkOperator(criterion);
    checkValue(criterion);
    query.addMetricCriterion(createMetricCriterion(criterion, criterion.getKey().toLowerCase(ENGLISH), criterion.getOperator()));
  }

  private static MetricCriterion createMetricCriterion(Criterion criterion, String metricKey, Operator operator) {
    if (NO_DATA.equalsIgnoreCase(criterion.getValue())) {
      checkArgument(EQ.equals(operator), "%s can only be used with equals operator", NO_DATA);
      return MetricCriterion.createNoData(metricKey);
    }
    return MetricCriterion.create(metricKey, operator, parseValue(criterion.getValue()));
  }

  private static double parseValue(String value) {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(format("Value '%s' is not a number", value));
    }
  }

  private static void checkValue(Criterion criterion) {
    checkArgument(criterion.getValue() != null, "Value cannot be null for '%s'", criterion.getKey());
  }

  private static void checkOperator(Criterion criterion) {
    checkArgument(criterion.getOperator() != null, "Operator cannot be null for '%s'", criterion.getKey());
  }
}
