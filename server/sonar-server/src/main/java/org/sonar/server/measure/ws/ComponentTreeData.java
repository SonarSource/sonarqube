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
package org.sonar.server.measure.ws;

import com.google.common.collect.Table;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonarqube.ws.Measures;

import static java.lang.Double.NaN;
import static java.lang.Double.isNaN;
import static java.util.Objects.requireNonNull;

class ComponentTreeData {
  private final ComponentDto baseComponent;
  private final List<ComponentDto> components;
  private final int componentCount;
  private final Map<String, ComponentDto> referenceComponentsByUuid;
  private final List<MetricDto> metrics;
  private final List<Measures.Period> periods;
  private final Table<String, MetricDto, Measure> measuresByComponentUuidAndMetric;

  private ComponentTreeData(Builder builder) {
    this.baseComponent = builder.baseComponent;
    this.components = builder.componentsFromDb;
    this.componentCount = builder.componentCount;
    this.referenceComponentsByUuid = builder.referenceComponentsByUuid;
    this.metrics = builder.metrics;
    this.measuresByComponentUuidAndMetric = builder.measuresByComponentUuidAndMetric;
    this.periods = builder.periods;
  }

  public ComponentDto getBaseComponent() {
    return baseComponent;
  }

  @CheckForNull
  List<ComponentDto> getComponents() {
    return components;
  }

  @CheckForNull
  int getComponentCount() {
    return componentCount;
  }

  @CheckForNull
  public Map<String, ComponentDto> getReferenceComponentsByUuid() {
    return referenceComponentsByUuid;
  }

  @CheckForNull
  List<MetricDto> getMetrics() {
    return metrics;
  }

  @CheckForNull
  List<Measures.Period> getPeriods() {
    return periods;
  }

  @CheckForNull
  Table<String, MetricDto, Measure> getMeasuresByComponentUuidAndMetric() {
    return measuresByComponentUuidAndMetric;
  }

  static Builder builder() {
    return new Builder();
  }

  static class Builder {
    private ComponentDto baseComponent;
    private List<ComponentDto> componentsFromDb;
    private Map<String, ComponentDto> referenceComponentsByUuid;
    private int componentCount;
    private List<MetricDto> metrics;
    private List<Measures.Period> periods;
    private Table<String, MetricDto, Measure> measuresByComponentUuidAndMetric;

    private Builder() {
      // private constructor
    }

    public Builder setBaseComponent(ComponentDto baseComponent) {
      this.baseComponent = baseComponent;
      return this;
    }

    public Builder setComponentsFromDb(List<ComponentDto> componentsFromDbQuery) {
      this.componentsFromDb = componentsFromDbQuery;
      return this;
    }

    public Builder setComponentCount(int componentCount) {
      this.componentCount = componentCount;
      return this;
    }

    public Builder setMetrics(List<MetricDto> metrics) {
      this.metrics = metrics;
      return this;
    }

    public Builder setPeriods(List<Measures.Period> periods) {
      this.periods = periods;
      return this;
    }

    public Builder setMeasuresByComponentUuidAndMetric(Table<String, MetricDto, Measure> measuresByComponentUuidAndMetric) {
      this.measuresByComponentUuidAndMetric = measuresByComponentUuidAndMetric;
      return this;
    }

    public Builder setReferenceComponentsByUuid(Map<String, ComponentDto> referenceComponentsByUuid) {
      this.referenceComponentsByUuid = referenceComponentsByUuid;
      return this;
    }

    public ComponentTreeData build() {
      requireNonNull(baseComponent);
      return new ComponentTreeData(this);
    }
  }

  static class Measure {
    private double value;
    private String data;
    private double variation;

    private Measure(LiveMeasureDto measureDto) {
      this.value = toPrimitive(measureDto.getValue());
      this.data = measureDto.getDataAsString();
      this.variation = toPrimitive(measureDto.getVariation());
    }

    public double getValue() {
      return value;
    }

    public boolean isValueSet() {
      return !isNaN(value);
    }

    @CheckForNull
    public String getData() {
      return data;
    }

    public double getVariation() {
      return variation;
    }

    public boolean isVariationSet() {
      return !isNaN(variation);
    }

    static Measure createFromMeasureDto(LiveMeasureDto measureDto) {
      return new Measure(measureDto);
    }

    private static double toPrimitive(@Nullable Double value) {
      return value == null ? NaN : value;
    }
  }
}
