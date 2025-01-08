/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.MeasureDto;
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

  MeasureMatrix(Collection<ComponentDto> components, Collection<MetricDto> metrics, List<MeasureDto> dbMeasures) {
    this(components.stream().map(ComponentDto::uuid).collect(Collectors.toSet()), metrics, dbMeasures);
  }

  MeasureMatrix(Set<String> componentUuids, Collection<MetricDto> metrics, List<MeasureDto> dbMeasures) {
    for (MetricDto metric : metrics) {
      this.metricsByKeys.put(metric.getKey(), metric);
    }
    this.table = ArrayTable.create(componentUuids, metricsByKeys.keySet());
    dbMeasures.forEach(dbMeasure -> {
      String branchUuid = dbMeasure.getBranchUuid();
      String componentUuid = dbMeasure.getComponentUuid();
      dbMeasure.getMetricValues().forEach((metricKey, value) -> {
        Measure measure = new Measure(componentUuid, branchUuid, metricsByKeys.get(metricKey), value);
        table.put(componentUuid, metricKey, new MeasureCell(measure));
      });
    });
  }

  MetricDto getMetric(String key) {
    return requireNonNull(metricsByKeys.get(key), () -> String.format("Metric with key %s not found", key));
  }

  Optional<Measure> getMeasure(ComponentDto component, String metricKey) {
    checkArgument(table.containsColumn(metricKey), "Metric with key %s is not registered", metricKey);
    MeasureCell cell = table.get(component.uuid(), metricKey);
    return cell == null ? Optional.empty() : Optional.of(cell.measure);
  }

  void setValue(ComponentDto component, String metricKey, double value) {
    changeCell(component, metricKey, m -> m.setValue(scale(getMetric(metricKey), value)));
  }

  void setValue(ComponentDto component, String metricKey, Rating value) {
    changeCell(component, metricKey, m -> m.setValue((double) value.getIndex()));
  }

  void setValue(ComponentDto component, String metricKey, @Nullable String data) {
    changeCell(component, metricKey, m -> m.setValue(data));
  }

  Stream<Measure> getChanged() {
    return table.values().stream()
      .filter(Objects::nonNull)
      .filter(MeasureCell::isChanged)
      .map(MeasureCell::getMeasure);
  }

  private void changeCell(ComponentDto component, String metricKey, Consumer<Measure> changer) {
    MeasureCell cell = table.get(component.uuid(), metricKey);
    if (cell == null) {
      Measure measure = new Measure(component.uuid(), component.branchUuid(), metricsByKeys.get(metricKey), null);
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
    private final Measure measure;
    private final Object initialValue;

    private MeasureCell(Measure measure) {
      this.measure = measure;
      this.initialValue = measure.getValue();
    }

    public Measure getMeasure() {
      return measure;
    }

    public boolean isChanged() {
      return !Objects.equals(initialValue, measure.getValue());
    }
  }

  static class Measure {
    static final Comparator<Measure> COMPARATOR = Comparator
      .comparing((Measure m) -> m.componentUuid)
      .thenComparing(m -> m.metricDto.getKey());

    private final String componentUuid;
    private final String branchUuid;
    private final MetricDto metricDto;
    private Object value;

    Measure(String componentUuid, String branchUuid, MetricDto metricDto, @Nullable Object value) {
      this.componentUuid = componentUuid;
      this.branchUuid = branchUuid;
      this.metricDto = metricDto;
      this.value = value;
    }

    @CheckForNull
    public Double doubleValue() {
      if (value == null || !metricDto.isNumeric()) {
        return null;
      }
      return Double.parseDouble(value.toString());
    }

    @CheckForNull
    public String stringValue() {
      if (value == null || metricDto.isNumeric()) {
        return null;
      }
      return String.valueOf(value);
    }

    public void setValue(@Nullable Object newValue) {
      this.value = newValue;
    }

    public String getBranchUuid() {
      return branchUuid;
    }

    public String getComponentUuid() {
      return componentUuid;
    }

    public String getMetricKey() {
      return metricDto.getKey();
    }

    @Nullable
    public Object getValue() {
      return value;
    }
  }
}
