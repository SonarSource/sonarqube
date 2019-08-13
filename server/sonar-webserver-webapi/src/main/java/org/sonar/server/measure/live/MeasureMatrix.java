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
package org.sonar.server.measure.live;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.Collections2;
import com.google.common.collect.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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
  private final Map<Integer, MetricDto> metricsByIds = new HashMap<>();

  MeasureMatrix(Collection<ComponentDto> components, Collection<MetricDto> metrics, List<LiveMeasureDto> dbMeasures) {
    for (MetricDto metric : metrics) {
      this.metricsByKeys.put(metric.getKey(), metric);
      this.metricsByIds.put(metric.getId(), metric);
    }
    this.table = ArrayTable.create(Collections2.transform(components, ComponentDto::uuid), metricsByKeys.keySet());
    for (LiveMeasureDto dbMeasure : dbMeasures) {
      table.put(dbMeasure.getComponentUuid(), metricsByIds.get(dbMeasure.getMetricId()).getKey(), new MeasureCell(dbMeasure, false));
    }
  }

  MetricDto getMetric(int id) {
    return requireNonNull(metricsByIds.get(id), () -> String.format("Metric with id %d not found", id));
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
    changeCell(component, metricKey, m -> {
      MetricDto metric = getMetric(metricKey);
      double newValue = scale(metric, value);

      Double initialValue = m.getValue();
      if (initialValue != null && Double.compare(initialValue, newValue) == 0) {
        return false;
      }
      m.setValue(newValue);
      Double initialVariation = m.getVariation();
      if (initialValue != null && initialVariation != null) {
        double leakInitialValue = initialValue - initialVariation;
        m.setVariation(scale(metric, value - leakInitialValue));
      }
      return true;
    });
  }

  void setValue(ComponentDto component, String metricKey, Rating value) {
    changeCell(component, metricKey, m -> {
      Double initialValue = m.getValue();
      if (initialValue != null && Double.compare(initialValue, (double) value.getIndex()) == 0) {
        return false;
      }
      m.setData(value.name());
      m.setValue((double) value.getIndex());

      Double initialVariation = m.getVariation();
      if (initialValue != null && initialVariation != null) {
        double leakInitialValue = initialValue - initialVariation;
        m.setVariation(value.getIndex() - leakInitialValue);
      }
      return true;
    });
  }

  void setValue(ComponentDto component, String metricKey, @Nullable String data) {
    changeCell(component, metricKey, m -> {
      if (Objects.equals(m.getDataAsString(), data)) {
        return false;
      }
      m.setData(data);
      return true;
    });
  }

  void setLeakValue(ComponentDto component, String metricKey, double variation) {
    changeCell(component, metricKey, c -> {
      double newVariation = scale(getMetric(metricKey), variation);
      if (c.getVariation() != null && Double.compare(c.getVariation(), newVariation) == 0) {
        return false;
      }
      MetricDto metric = metricsByKeys.get(metricKey);
      c.setVariation(scale(metric, variation));
      return true;
    });
  }

  void setLeakValue(ComponentDto component, String metricKey, Rating variation) {
    setLeakValue(component, metricKey, (double) variation.getIndex());
  }

  Stream<LiveMeasureDto> getChanged() {
    return table.values()
      .stream()
      .filter(Objects::nonNull)
      .filter(MeasureCell::isChanged)
      .map(MeasureCell::getMeasure);
  }

  private void changeCell(ComponentDto component, String metricKey, Function<LiveMeasureDto, Boolean> changer) {
    MeasureCell cell = table.get(component.uuid(), metricKey);
    if (cell == null) {
      LiveMeasureDto measure = new LiveMeasureDto()
        .setComponentUuid(component.uuid())
        .setProjectUuid(component.projectUuid())
        .setMetricId(metricsByKeys.get(metricKey).getId());
      cell = new MeasureCell(measure, true);
      table.put(component.uuid(), metricKey, cell);
      changer.apply(cell.getMeasure());
    } else if (changer.apply(cell.getMeasure())) {
      cell.setChanged(true);
    }
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
    private boolean changed;

    private MeasureCell(LiveMeasureDto measure, boolean changed) {
      this.measure = measure;
      this.changed = changed;
    }

    public LiveMeasureDto getMeasure() {
      return measure;
    }

    public boolean isChanged() {
      return changed;
    }

    public void setChanged(boolean b) {
      this.changed = b;
    }
  }
}
