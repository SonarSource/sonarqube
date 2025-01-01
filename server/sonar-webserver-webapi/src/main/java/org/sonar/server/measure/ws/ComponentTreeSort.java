/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.measure.ws;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Table;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.measure.ImpactMeasureBuilder;

import static java.lang.String.CASE_INSENSITIVE_ORDER;
import static java.lang.String.format;
import static org.sonar.api.measures.Metric.ValueType.BOOL;
import static org.sonar.api.measures.Metric.ValueType.FLOAT;
import static org.sonar.api.measures.Metric.ValueType.INT;
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

public class ComponentTreeSort {

  static final Set<ValueType> NUMERIC_VALUE_TYPES = EnumSet.of(BOOL, FLOAT, INT, MILLISEC, WORK_DUR, PERCENT, RATING);
  static final Set<ValueType> TEXTUAL_VALUE_TYPES = EnumSet.of(STRING);

  private ComponentTreeSort() {
    // static method only
  }

  public static List<ComponentDto> sortComponents(List<ComponentDto> components, ComponentTreeRequest wsRequest, List<MetricDto> metrics,
    Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric) {
    List<String> sortParameters = wsRequest.getSort();
    if (sortParameters == null || sortParameters.isEmpty()) {
      return components;
    }
    boolean isAscending = wsRequest.getAsc();
    Map<String, Ordering<ComponentDto>> orderingsBySortField = ImmutableMap.<String, Ordering<ComponentDto>>builder()
      .put(NAME_SORT, componentNameOrdering(isAscending))
      .put(QUALIFIER_SORT, componentQualifierOrdering(isAscending))
      .put(PATH_SORT, componentPathOrdering(isAscending))
      .put(METRIC_SORT, metricValueOrdering(wsRequest, metrics, measuresByComponentUuidAndMetric))
      .put(METRIC_PERIOD_SORT, metricPeriodOrdering(wsRequest, metrics, measuresByComponentUuidAndMetric))
      .build();

    String firstSortParameter = sortParameters.get(0);
    Ordering<ComponentDto> primaryOrdering = orderingsBySortField.get(firstSortParameter);
    if (sortParameters.size() > 1) {
      for (int i = 1; i < sortParameters.size(); i++) {
        String secondarySortParameter = sortParameters.get(i);
        Ordering<ComponentDto> secondaryOrdering = orderingsBySortField.get(secondarySortParameter);
        primaryOrdering = primaryOrdering.compound(secondaryOrdering);
      }
    }
    primaryOrdering = primaryOrdering.compound(componentNameOrdering(true));

    return primaryOrdering.immutableSortedCopy(components);
  }

  private static Ordering<ComponentDto> componentNameOrdering(boolean isAscending) {
    return stringOrdering(isAscending, ComponentDto::name);
  }

  private static Ordering<ComponentDto> componentQualifierOrdering(boolean isAscending) {
    return stringOrdering(isAscending, ComponentDto::qualifier);
  }

  private static Ordering<ComponentDto> componentPathOrdering(boolean isAscending) {
    return stringOrdering(isAscending, ComponentDto::path);
  }

  private static Ordering<ComponentDto> stringOrdering(boolean isAscending, Function<ComponentDto, String> function) {
    Ordering<String> ordering = Ordering.from(CASE_INSENSITIVE_ORDER);
    if (!isAscending) {
      ordering = ordering.reverse();
    }

    return ordering.nullsLast().onResultOf(function);
  }

  private static Ordering<ComponentDto> metricValueOrdering(ComponentTreeRequest wsRequest, List<MetricDto> metrics,
    Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric) {
    boolean isAscending = Optional.ofNullable(wsRequest.getAsc()).orElse(false);
    if (wsRequest.getMetricSort() == null) {
      return componentNameOrdering(isAscending);
    }
    Map<String, MetricDto> metricsByKey = Maps.uniqueIndex(metrics, MetricDto::getKey);
    MetricDto metric = metricsByKey.get(wsRequest.getMetricSort());

    ValueType metricValueType = ValueType.valueOf(metric.getValueType());
    if (NUMERIC_VALUE_TYPES.contains(metricValueType)) {
      return numericalMetricOrdering(isAscending, metric, measuresByComponentUuidAndMetric);
    } else if (TEXTUAL_VALUE_TYPES.contains(metricValueType)) {
      return stringOrdering(isAscending, new ComponentDtoToTextualMeasureValue(metric, measuresByComponentUuidAndMetric));
    } else if (ValueType.LEVEL.equals(ValueType.valueOf(metric.getValueType()))) {
      return levelMetricOrdering(isAscending, metric, measuresByComponentUuidAndMetric);
    } else if (ValueType.DATA.equals(ValueType.valueOf(metric.getValueType()))
               && DataSupportedMetrics.IMPACTS_SUPPORTED_METRICS.contains(metric.getKey())) {
      return totalMetricOrdering(isAscending, metric, measuresByComponentUuidAndMetric);
    }

    throw new IllegalStateException("Unrecognized metric value type: " + metric.getValueType());
  }

  private static Ordering<ComponentDto> metricPeriodOrdering(ComponentTreeRequest wsRequest, List<MetricDto> metrics,
    Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric) {
    boolean isAscending = Optional.ofNullable(wsRequest.getAsc()).orElse(false);
    if (wsRequest.getMetricSort() == null || wsRequest.getMetricPeriodSort() == null) {
      return componentNameOrdering(isAscending);
    }
    Map<String, MetricDto> metricsByKey = Maps.uniqueIndex(metrics, MetricDto::getKey);
    MetricDto metric = metricsByKey.get(wsRequest.getMetricSort());

    ValueType metricValueType = ValueType.valueOf(metric.getValueType());
    if (NUMERIC_VALUE_TYPES.contains(metricValueType)) {
      return numericalMetricPeriodOrdering(isAscending, metric, measuresByComponentUuidAndMetric);
    } else if (ValueType.DATA.equals(metricValueType)
               && DataSupportedMetrics.IMPACTS_SUPPORTED_METRICS.contains(metric.getKey())) {
      return totalNewPeriodMetricOrdering(isAscending, metric, measuresByComponentUuidAndMetric);
    }

    throw BadRequestException.create(format("Impossible to sort metric '%s' by measure period.", metric.getKey()));
  }

  private static Ordering<ComponentDto> numericalMetricOrdering(boolean isAscending, @Nullable MetricDto metric,
    Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric) {
    Ordering<Double> ordering = getOrdering(isAscending);

    return ordering.onResultOf(new ComponentDtoToNumericalMeasureValue(metric, measuresByComponentUuidAndMetric));
  }

  @NotNull
  private static <T extends Comparable<T>> Ordering<T> getOrdering(boolean isAscending) {
    Ordering<T> ordering = Ordering.natural();

    if (!isAscending) {
      ordering = ordering.reverse();
    }
    return ordering.nullsLast();
  }

  private static Ordering<ComponentDto> totalMetricOrdering(boolean isAscending, MetricDto metric,
    Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric) {
    Ordering<Long> ordering = getOrdering(isAscending);
    return ordering.onResultOf(new ComponentDtoToTotalImpactMeasureValue(metric, measuresByComponentUuidAndMetric, false));
  }

  private static Ordering<ComponentDto> totalNewPeriodMetricOrdering(boolean isAscending, MetricDto metric,
    Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric) {
    Ordering<Long> ordering = getOrdering(isAscending);
    return ordering.onResultOf(new ComponentDtoToTotalImpactMeasureValue(metric, measuresByComponentUuidAndMetric, true));
  }


  private static Ordering<ComponentDto> numericalMetricPeriodOrdering(boolean isAscending, @Nullable MetricDto metric,
    Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric) {
    Ordering<Double> ordering = getOrdering(isAscending);

    return ordering.onResultOf(new ComponentDtoToMeasureVariationValue(metric, measuresByComponentUuidAndMetric));
  }

  private static Ordering<ComponentDto> levelMetricOrdering(boolean isAscending, @Nullable MetricDto metric,
    Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric) {
    // inverse the order of org.sonar.api.measures.Metric.Level enum
    Ordering<Integer> ordering = getOrdering(!isAscending);

    return ordering.onResultOf(new ComponentDtoToLevelIndex(metric, measuresByComponentUuidAndMetric));
  }

  private static class ComponentDtoToNumericalMeasureValue implements Function<ComponentDto, Double> {
    private final MetricDto metric;
    private final Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric;

    private ComponentDtoToNumericalMeasureValue(@Nullable MetricDto metric,
      Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric) {
      this.metric = metric;
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
    }

    @Override
    public Double apply(@Nonnull ComponentDto input) {
      ComponentTreeData.Measure measure = measuresByComponentUuidAndMetric.get(input.uuid(), metric);
      if (measure == null || !measure.isValueSet()) {
        return null;
      }

      return measure.getValue();
    }
  }

  private static class ComponentDtoToLevelIndex implements Function<ComponentDto, Integer> {
    private final MetricDto metric;
    private final Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric;

    private ComponentDtoToLevelIndex(@Nullable MetricDto metric,
      Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric) {
      this.metric = metric;
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
    }

    @Override
    public Integer apply(@Nonnull ComponentDto input) {
      ComponentTreeData.Measure measure = measuresByComponentUuidAndMetric.get(input.uuid(), metric);
      if (measure == null || measure.getData() == null) {
        return null;
      }

      return Metric.Level.names().indexOf(measure.getData());
    }
  }

  private static class ComponentDtoToMeasureVariationValue implements Function<ComponentDto, Double> {
    private final MetricDto metric;
    private final Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric;

    private ComponentDtoToMeasureVariationValue(@Nullable MetricDto metric,
      Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric) {
      this.metric = metric;
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
    }

    @Override
    public Double apply(@Nonnull ComponentDto input) {
      ComponentTreeData.Measure measure = measuresByComponentUuidAndMetric.get(input.uuid(), metric);
      if (measure == null || !metric.getKey().startsWith("new_")) {
        return null;
      }
      return measure.getValue();
    }
  }

  private static class ComponentDtoToTextualMeasureValue implements Function<ComponentDto, String> {
    private final MetricDto metric;
    private final Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric;

    private ComponentDtoToTextualMeasureValue(@Nullable MetricDto metric,
      Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric) {
      this.metric = metric;
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
    }

    @Override
    public String apply(@Nonnull ComponentDto input) {
      ComponentTreeData.Measure measure = measuresByComponentUuidAndMetric.get(input.uuid(), metric);
      if (measure == null || measure.getData() == null) {
        return null;
      }

      return measure.getData();
    }
  }

  private static class ComponentDtoToTotalImpactMeasureValue implements Function<ComponentDto, Long> {
    private final MetricDto metric;
    private final Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric;
    private final boolean onlyNewPeriodMeasures;

    //Store the total value for each component to avoid multiple deserialization of the same measure
    Map<String, Long> totalByComponentUuid = new HashMap<>();

    private ComponentDtoToTotalImpactMeasureValue(@Nullable MetricDto metric,
      Table<String, MetricDto, ComponentTreeData.Measure> measuresByComponentUuidAndMetric, boolean onlyNewPeriodMeasures) {
      this.metric = metric;
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
      this.onlyNewPeriodMeasures = onlyNewPeriodMeasures;
    }

    @Override
    public Long apply(@Nonnull ComponentDto input) {
      if (onlyNewPeriodMeasures && metric != null && !metric.getKey().startsWith("new_")) {
        return null;
      }
      return totalByComponentUuid.computeIfAbsent(input.uuid(),
        k -> {
          ComponentTreeData.Measure measure = measuresByComponentUuidAndMetric.get(input.uuid(), metric);
          return Optional.ofNullable(measure).map(ComponentTreeData.Measure::getData)
            .map(data -> ImpactMeasureBuilder.fromString(measure.getData()).getTotal())
            .orElse(null);
        });
    }
  }

}
