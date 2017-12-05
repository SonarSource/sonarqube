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
package org.sonar.server.measure.live;

import com.google.common.collect.ArrayTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.stream.Stream;
import org.sonar.api.measures.Metric;
import org.sonar.core.util.stream.MoreCollectors;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.server.computation.task.projectanalysis.qualitymodel.Rating;

import static com.google.common.base.Preconditions.checkArgument;

class MeasureMatrix {

  // component uuid -> metric key -> measure
  private final Table<String, String, MeasureCell> table;

  // direction is from file to project
  private final List<ComponentDto> bottomUpComponents;

  private final Map<String, MetricDto> metricsByKeys;

  MeasureMatrix(List<ComponentDto> bottomUpComponents, Collection<MetricDto> metrics, List<LiveMeasureDto> dbMeasures) {
    this.bottomUpComponents = bottomUpComponents;
    this.metricsByKeys = metrics
      .stream()
      .collect(MoreCollectors.uniqueIndex(MetricDto::getKey));
    this.table = ArrayTable.create(Lists.transform(bottomUpComponents, ComponentDto::uuid), metricsByKeys.keySet());
    Map<Integer, MetricDto> metricsByIds = metricsByKeys.values()
      .stream()
      .collect(MoreCollectors.uniqueIndex(MetricDto::getId));
    for (LiveMeasureDto dbMeasure : dbMeasures) {
      table.put(dbMeasure.getComponentUuid(), metricsByIds.get(dbMeasure.getMetricId()).getKey(), new MeasureCell(dbMeasure, false));
    }
  }

  ComponentDto getProject() {
    return bottomUpComponents.get(bottomUpComponents.size() - 1);
  }

  Stream<ComponentDto> getBottomUpComponents() {
    return bottomUpComponents.stream();
  }

  OptionalDouble getValue(ComponentDto component, Metric metric) {
    checkArgument(table.containsColumn(metric.getKey()));
    MeasureCell cell = table.get(component.uuid(), metric.getKey());
    if (cell == null || cell.getMeasure().getValue() == null) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of(cell.getMeasure().getValue());
  }

  void setValue(ComponentDto component, Metric metric, double value) {
    changeCell(component, metric, m -> {
      Double initialValue = m.getValue();
      if (initialValue != null && Double.compare(initialValue, value) == 0) {
        return false;
      }
      Double initialVariation = m.getVariation();
      if (initialValue != null && initialVariation != null) {
        double leakInitialValue = initialValue - initialVariation;
        m.setVariation(metric.scale(value - leakInitialValue));
      }
      m.setValue(metric.scale(value));
      return true;
    });
  }

  void setValue(ComponentDto component, Metric metric, String value) {
    changeCell(component, metric, m -> {
      if (Objects.equals(m.getDataAsString(), value)) {
        return false;
      }
      m.setData(value);
      return true;
    });
  }

  void setValue(ComponentDto component, Metric metric, Rating value) {
    changeCell(component, metric, m -> {
      if (m.getValue() != null && Double.compare(m.getValue(), (double) value.getIndex()) == 0) {
        return false;
      }
      m.setData(value.name());
      m.setValue((double) value.getIndex());
      // TODO variation on ratings
      return true;
    });
  }

  void setLeakValue(ComponentDto component, Metric metric, double variation) {
    changeCell(component, metric, c -> {
      if (c.getVariation() != null && Double.compare(c.getVariation(), variation) == 0) {
        return false;
      }
      c.setVariation(metric.scale(variation));
      return true;
    });
  }

  void setLeakValue(ComponentDto component, Metric metric, Rating variation) {
    setLeakValue(component, metric, (double) variation.getIndex());
  }

  Stream<LiveMeasureDto> getChanged() {
    return table.values()
      .stream()
      .filter(Objects::nonNull)
      .filter(MeasureCell::isChanged)
      .map(MeasureCell::getMeasure);
  }

  private void changeCell(ComponentDto component, Metric metric, Function<LiveMeasureDto, Boolean> changer) {
    MeasureCell cell = table.get(component.uuid(), metric.getKey());
    if (cell == null) {
      LiveMeasureDto measure = new LiveMeasureDto()
        .setComponentUuid(component.uuid())
        .setProjectUuid(component.projectUuid())
        .setMetricId(metricsByKeys.get(metric.getKey()).getId());
      cell = new MeasureCell(measure, true);
      table.put(component.uuid(), metric.getKey(), cell);
      changer.apply(cell.getMeasure());
    } else if (changer.apply(cell.getMeasure())) {
      cell.setChanged(true);
    }
  }

  private static class MeasureCell {
    private final LiveMeasureDto measure;
    private boolean changed = false;

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
