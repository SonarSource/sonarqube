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
package org.sonar.server.measure.live;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.measure.Rating;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Keep the measures in memory during refresh of live measures:
 * <ul>
 *   <li>the values of last analysis, restricted to the needed metrics</li>
 *   <li>the refreshed values</li>
 * </ul>
 */
class MeasureMatrix {
  // component uuid -> metric key -> measure
  private final Table<String, String, MeasureCell> table;

  private final Map<String, MetricDto> metricsByKeys = new HashMap<>();
  private final Map<String, MetricDto> metricsByUuids = new HashMap<>();

  MeasureMatrix(Collection<ComponentDto> components, Collection<MetricDto> metrics, List<LiveMeasureDto> dbMeasures) {
    this(components.stream().map(ComponentDto::uuid).collect(Collectors.toSet()), metrics, dbMeasures);
  }

  MeasureMatrix(Set<String> componentUuids, Collection<MetricDto> metrics, List<LiveMeasureDto> dbMeasures) {
    for (MetricDto metric : metrics) {
      this.metricsByKeys.put(metric.getKey(), metric);
      this.metricsByUuids.put(metric.getUuid(), metric);
    }
    this.table = ArrayTable.create(componentUuids, metricsByKeys.keySet());

    for (LiveMeasureDto dbMeasure : dbMeasures) {
      table.put(dbMeasure.getComponentUuid(), metricsByUuids.get(dbMeasure.getMetricUuid()).getKey(), new MeasureCell(dbMeasure));
    }
  }

  MetricDto getMetricByUuid(String uuid) {
    return requireNonNull(metricsByUuids.get(uuid), () -> String.format("Metric with uuid %s not found", uuid));
  }

  private MetricDto getMetric(String key) {
    return requireNonNull(metricsByKeys.get(key), () -> String.format("Metric with key %s not found", key));
  }

  Optional<LiveMeasureDto> getMeasure(ComponentDto component, String metricKey) {
    checkArgument(table.containsColumn(metricKey), "Metric with key %s is not registered", metricKey);
    MeasureCell cell = table.get(component.uuid(), metricKey);
    return cell == null ? Optional.empty() : Optional.of(cell.measure);
  }

  void setValue(ComponentDto component, String metricKey, double value) {
    changeCell(component, metricKey, m -> m.setValue(scale(getMetric(metricKey), value)));
  }

  void setValue(ComponentDto component, String metricKey, Rating value) {
    changeCell(component, metricKey, m -> {
      m.setData(value.name());
      m.setValue((double) value.getIndex());
    });
  }

  void setValue(ComponentDto component, String metricKey, @Nullable String data) {
    changeCell(component, metricKey, m -> m.setData(data));
  }

  Stream<LiveMeasureDto> getChanged() {
    return table.values().stream()
      .filter(Objects::nonNull)
      .filter(MeasureCell::isChanged)
      .map(MeasureCell::getMeasure);
  }

  private void changeCell(ComponentDto component, String metricKey, Consumer<LiveMeasureDto> changer) {
    MeasureCell cell = table.get(component.uuid(), metricKey);
    if (cell == null) {
      LiveMeasureDto measure = new LiveMeasureDto()
        .setComponentUuid(component.uuid())
        .setProjectUuid(component.branchUuid())
        .setMetricUuid(metricsByKeys.get(metricKey).getUuid());
      cell = new MeasureCell(measure);
      table.put(component.uuid(), metricKey, cell);
    }
    changer.accept(cell.getMeasure());
  }

  /**
   * Round a measure value by applying the scale defined on the metric.
   * Example: scale(0.1234) returns 0.12 if metric scale is 2
   */
  private static double scale(MetricDto metric, double value) {
    if (metric.getDecimalScale() == null) {
      return value;
    }
    BigDecimal bd = BigDecimal.valueOf(value);
    return bd.setScale(metric.getDecimalScale(), RoundingMode.HALF_UP).doubleValue();
  }

  private static class MeasureCell {
    private final LiveMeasureDto measure;
    private final Double initialValue;
    private final byte[] initialData;
    private final String initialTextValue;

    private MeasureCell(LiveMeasureDto measure) {
      this.measure = measure;
      this.initialValue = measure.getValue();
      this.initialData = measure.getData();
      this.initialTextValue = measure.getTextValue();
    }

    public LiveMeasureDto getMeasure() {
      return measure;
    }

    public boolean isChanged() {
      return !Objects.equals(initialValue, measure.getValue()) || !Arrays.equals(initialData, measure.getData()) || !Objects.equals(initialTextValue, measure.getTextValue());
    }
  }
}
