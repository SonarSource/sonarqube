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
package org.sonar.server.measure.ws;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.db.component.ComponentDtoWithSnapshotId;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.metric.MetricDtoFunctions;
import org.sonar.server.exceptions.BadRequestException;
import org.sonarqube.ws.client.measure.ComponentTreeWsRequest;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.lang.String.format;
import static org.sonar.api.measures.Metric.ValueType.BOOL;
import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
import static org.sonar.api.measures.Metric.ValueType.LEVEL;
import static org.sonar.api.measures.Metric.ValueType.MILLISEC;
import static org.sonar.api.measures.Metric.ValueType.PERCENT;
import static org.sonar.api.measures.Metric.ValueType.RATING;
import static org.sonar.api.measures.Metric.ValueType.STRING;
import static org.sonar.api.measures.Metric.ValueType.WORK_DUR;
import static org.sonar.server.measure.ws.ComponentTreeAction.METRIC_PERIOD_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.METRIC_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.NAME_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.PATH_SORT;
import static org.sonar.server.measure.ws.ComponentTreeAction.QUALIFIER_SORT;

class ComponentTreeSort {

  private static final Set<ValueType> NUMERIC_VALUE_TYPES = EnumSet.of(BOOL, FLOAT, INT, MILLISEC, WORK_DUR, PERCENT, RATING);
  private static final Set<ValueType> TEXTUAL_VALUE_TYPES = EnumSet.of(DATA, DISTRIB, LEVEL, STRING);

  private ComponentTreeSort() {
    // static method only
  }

  static List<ComponentDtoWithSnapshotId> sortComponents(List<ComponentDtoWithSnapshotId> components, ComponentTreeWsRequest wsRequest, List<MetricDto> metrics,
    Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
    List<String> sortParameters = wsRequest.getSort();
    if (sortParameters == null || sortParameters.isEmpty()) {
      return components;
    }
    boolean isAscending = wsRequest.getAsc();
    Map<String, Ordering<ComponentDtoWithSnapshotId>> orderingsBySortField = ImmutableMap.<String, Ordering<ComponentDtoWithSnapshotId>>builder()
      .put(NAME_SORT, componentNameOrdering(isAscending))
      .put(QUALIFIER_SORT, componentQualifierOrdering(isAscending))
      .put(PATH_SORT, componentPathOrdering(isAscending))
      .put(METRIC_SORT, metricValueOrdering(wsRequest, metrics, measuresByComponentUuidAndMetric))
      .put(METRIC_PERIOD_SORT, metricPeriodOrdering(wsRequest, metrics, measuresByComponentUuidAndMetric))
      .build();

    String firstSortParameter = sortParameters.get(0);
    Ordering<ComponentDtoWithSnapshotId> primaryOrdering = orderingsBySortField.get(firstSortParameter);
    if (sortParameters.size() > 1) {
      for (int i = 1; i < sortParameters.size(); i++) {
        String secondarySortParameter = sortParameters.get(i);
        Ordering<ComponentDtoWithSnapshotId> secondaryOrdering = orderingsBySortField.get(secondarySortParameter);
        primaryOrdering = primaryOrdering.compound(secondaryOrdering);
      }
    }

    return primaryOrdering.immutableSortedCopy(components);
  }

  private static Ordering<ComponentDtoWithSnapshotId> componentNameOrdering(boolean isAscending) {
    return stringOrdering(isAscending, ComponentDtoWithSnapshotIdToName.INSTANCE);
  }

  private static Ordering<ComponentDtoWithSnapshotId> componentQualifierOrdering(boolean isAscending) {
    return stringOrdering(isAscending, ComponentDtoWithSnapshotIdToQualifier.INSTANCE);
  }

  private static Ordering<ComponentDtoWithSnapshotId> componentPathOrdering(boolean isAscending) {
    return stringOrdering(isAscending, ComponentDtoWithSnapshotIdToPath.INSTANCE);
  }

  private static Ordering<ComponentDtoWithSnapshotId> stringOrdering(boolean isAscending, Function<ComponentDtoWithSnapshotId, String> function) {
    Ordering<String> ordering = Ordering.from(CASE_INSENSITIVE_ORDER);
    if (!isAscending) {
      ordering = ordering.reverse();
    }

    return ordering.nullsLast().onResultOf(function);
  }

  private static Ordering<ComponentDtoWithSnapshotId> metricValueOrdering(ComponentTreeWsRequest wsRequest, List<MetricDto> metrics,
    Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
    if (wsRequest.getMetricSort() == null) {
      return componentNameOrdering(wsRequest.getAsc());
    }
    Map<String, MetricDto> metricsByKey = Maps.uniqueIndex(metrics, MetricDtoFunctions.toKey());
    MetricDto metric = metricsByKey.get(wsRequest.getMetricSort());

    boolean isAscending = wsRequest.getAsc();
    ValueType metricValueType = ValueType.valueOf(metric.getValueType());
    if (NUMERIC_VALUE_TYPES.contains(metricValueType)) {
      return numericalMetricOrdering(isAscending, metric, measuresByComponentUuidAndMetric);
    } else if (TEXTUAL_VALUE_TYPES.contains(metricValueType)) {
      return stringOrdering(isAscending, new ComponentDtoWithSnapshotIdToTextualMeasureValue(metric, measuresByComponentUuidAndMetric));
    }

    throw new IllegalStateException("Unrecognized metric value type: " + metric.getValueType());
  }

  private static Ordering<ComponentDtoWithSnapshotId> metricPeriodOrdering(ComponentTreeWsRequest wsRequest, List<MetricDto> metrics,
    Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
    if (wsRequest.getMetricSort() == null || wsRequest.getMetricPeriodSort() == null) {
      return componentNameOrdering(wsRequest.getAsc());
    }
    Map<String, MetricDto> metricsByKey = Maps.uniqueIndex(metrics, MetricDtoFunctions.toKey());
    MetricDto metric = metricsByKey.get(wsRequest.getMetricSort());

    ValueType metricValueType = ValueType.valueOf(metric.getValueType());
    if (NUMERIC_VALUE_TYPES.contains(metricValueType)) {
      return numericalMetricPeriodOrdering(wsRequest, metric, measuresByComponentUuidAndMetric);
    }

    throw new BadRequestException(format("Impossible to sort metric '%s' by measure period.", metric.getKey()));
  }

  private static Ordering<ComponentDtoWithSnapshotId> numericalMetricOrdering(boolean isAscending, @Nullable MetricDto metric,
    Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
    Ordering<Double> ordering = Ordering.natural();

    if (!isAscending) {
      ordering = ordering.reverse();
    }

    return ordering.nullsLast().onResultOf(new ComponentDtoWithSnapshotIdToNumericalMeasureValue(metric, measuresByComponentUuidAndMetric));
  }

  private static Ordering<ComponentDtoWithSnapshotId> numericalMetricPeriodOrdering(ComponentTreeWsRequest request, @Nullable MetricDto metric,
    Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
    Ordering<Double> ordering = Ordering.natural();

    if (!request.getAsc()) {
      ordering = ordering.reverse();
    }

    return ordering.nullsLast().onResultOf(new ComponentDtoWithSnapshotIdToMeasureVariationValue(metric, measuresByComponentUuidAndMetric, request.getMetricPeriodSort()));
  }

  private static class ComponentDtoWithSnapshotIdToNumericalMeasureValue implements Function<ComponentDtoWithSnapshotId, Double> {
    private final MetricDto metric;
    private final Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric;

    private ComponentDtoWithSnapshotIdToNumericalMeasureValue(@Nullable MetricDto metric,
      Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
      this.metric = metric;
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
    }

    @Override
    public Double apply(@Nonnull ComponentDtoWithSnapshotId input) {
      MeasureDto measure = measuresByComponentUuidAndMetric.get(input.uuid(), metric);
      if (measure == null || measure.getValue() == null) {
        return null;
      }

      return measure.getValue();
    }
  }

  private static class ComponentDtoWithSnapshotIdToMeasureVariationValue implements Function<ComponentDtoWithSnapshotId, Double> {
    private final MetricDto metric;
    private final Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric;
    private final int variationIndex;

    private ComponentDtoWithSnapshotIdToMeasureVariationValue(@Nullable MetricDto metric,
      Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric, int variationIndex) {
      this.metric = metric;
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
      this.variationIndex = variationIndex;
    }

    @Override
    public Double apply(@Nonnull ComponentDtoWithSnapshotId input) {
      MeasureDto measure = measuresByComponentUuidAndMetric.get(input.uuid(), metric);
      if (measure == null || measure.getVariation(variationIndex) == null) {
        return null;
      }

      return measure.getVariation(variationIndex);
    }
  }

  private static class ComponentDtoWithSnapshotIdToTextualMeasureValue implements Function<ComponentDtoWithSnapshotId, String> {
    private final MetricDto metric;
    private final Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric;

    private ComponentDtoWithSnapshotIdToTextualMeasureValue(@Nullable MetricDto metric,
      Table<String, MetricDto, MeasureDto> measuresByComponentUuidAndMetric) {
      this.metric = metric;
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
    }

    @Override
    public String apply(@Nonnull ComponentDtoWithSnapshotId input) {
      MeasureDto measure = measuresByComponentUuidAndMetric.get(input.uuid(), metric);
      if (measure == null || measure.getData() == null) {
        return null;
      }

      return measure.getData();
    }
  }

  private enum ComponentDtoWithSnapshotIdToName implements Function<ComponentDtoWithSnapshotId, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull ComponentDtoWithSnapshotId input) {
      return input.name();
    }
  }

  private enum ComponentDtoWithSnapshotIdToQualifier implements Function<ComponentDtoWithSnapshotId, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull ComponentDtoWithSnapshotId input) {
      return input.qualifier();
    }
  }

  private enum ComponentDtoWithSnapshotIdToPath implements Function<ComponentDtoWithSnapshotId, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull ComponentDtoWithSnapshotId input) {
      return input.path();
    }
  }
}
